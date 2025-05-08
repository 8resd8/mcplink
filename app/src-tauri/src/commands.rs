// app/src-tauri/src/commands.rs

use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json;
use serde_json::Value;
use std::collections::HashMap;
use std::fs;
use std::env;
use std::path::Path;
use std::process::Command as StdCommand;
use tauri::{Manager, State};
use urlencoding;
use dotenvy::dotenv;
use tokio;

// --- 기존 구조체 정의 (McpServerInfo, ApiCardData, PageInfo, DataWrapper, ApiResponse) ---
#[derive(Debug, Deserialize)]
struct McpServerInfo {
    name: String,
    description: String,
}

#[derive(Debug, Deserialize)]
#[allow(non_snake_case)]
struct ApiCardData {
    id: i32,
    #[serde(rename = "type")]
    _type: String,
    url: String,
    stars: i32,
    views: i32,
    scanned: bool,
    mcpServers: McpServerInfo,
}

#[derive(Debug, Deserialize)]
#[allow(non_snake_case)]
struct PageInfo {
    startCursor: Option<Value>,
    endCursor: Option<Value>,
    hasNextPage: bool,
    totalItems: i32,
}

#[derive(Debug, Deserialize)]
#[allow(non_snake_case)]
struct DataWrapper {
    pageInfo: PageInfo,
    data: Vec<ApiCardData>,
}

#[derive(Debug, Deserialize)]
#[allow(non_snake_case)]
struct ApiResponse {
    timestamp: String,
    message: String,
    code: String,
    data: Value,
}

// --- MCPCard, MCPCardDetail, MCPServerConfig, ClaudeDesktopConfig, AppState 구조체 정의 ---
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPCard {
    pub id: i32,
    pub title: String,
    pub description: String,
    pub url: String,
    pub stars: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPCardDetail {
    pub id: i32,
    pub title: String,
    pub description: String,
    pub url: String,
    pub stars: i32,
    pub args: Option<Vec<String>>,
    pub env: Option<serde_json::Map<String, Value>>,
    // pub command: Option<String>,
}

#[derive(Debug, Deserialize)] // DetailApiResponse는 Deserialize만 필요할 수 있음
struct DetailApiResponse {
    id: i32,
    url: String,
    stars: i32,
    #[serde(rename = "mcpServers")]
    mcp_servers: McpServerInfo,
    args: Option<Vec<String>>,
    env: Option<serde_json::Map<String, Value>>,
    // command: Option<String>,
}


#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPServerConfig {
    pub command: String,
    pub args: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<serde_json::Map<String, Value>>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct ClaudeDesktopConfig {
    pub mcpServers: Option<HashMap<String, MCPServerConfig>>,
    #[serde(flatten)]
    pub other: serde_json::Map<String, Value>,
}

pub struct AppState {
    pub client: Client,
}

// --- Tauri Commands ---

#[tauri::command]
pub fn some_command() -> String {
    let _ = dotenvy::dotenv().ok();
    let crawler_api_base_url: String = std::env::var("CRAWLER_API_BASE_URL")
        .expect("CRAWLER_API_BASE_URL must be set");
    return crawler_api_base_url;
}

#[tauri::command]
pub async fn get_mcp_data(
    state: State<'_, AppState>,
    search_term: Option<String>, // JS에서 searchTerm으로 보내면 Rust에서 search_term으로 받음
) -> Result<Vec<MCPCard>, String> {
    dotenvy::dotenv().ok();

    let base_url: String = match env::var("CRAWLER_API_BASE_URL") { // .env에는 .../servers 까지만
        Ok(url_val) => url_val,
        Err(e) => {
            let msg = format!("[get_mcp_data] CRAWLER_API_BASE_URL not set: {}", e);
            println!("{}", msg);
            return Err(msg);
        }
    };
    println!("[get_mcp_data] CRAWLER_API_BASE_URL loaded: {}", base_url);
    println!("[get_mcp_data] Received search_term: {:?}", search_term);

    let request_url = if let Some(term) = search_term {
        // searchTerm이 Some일 때 (mcp-api.ts에서 빈 문자열은 이미 필터링)
        if term.is_empty(){ // 안전 장치: 혹시 빈 문자열이 오면 전체 목록으로
             println!("[get_mcp_data] Search term is Some but empty. Requesting all data from base endpoint: {}", base_url);
             base_url.to_string()
        } else {
            let encoded_term = urlencoding::encode(&term);
            let search_url = format!("{}/search?name={}", base_url, encoded_term);
            println!("[get_mcp_data] Search term is Some(\"{}\"). Requesting from search endpoint: {}", term, search_url);
            search_url
        }
    } else {
        // searchTerm이 None일 때 (mcp-api.ts에서 인자 없이 호출) -> 전체 목록 요청
        println!("[get_mcp_data] Search term is None. Requesting all data from base endpoint: {}", base_url);
        base_url.to_string()
    };

    match state.client.get(&request_url).send().await {
        Ok(response) => {
            let status = response.status();
            println!("[get_mcp_data] Response status for {}: {}", request_url, status);

            if status.is_success() {
                match response.text().await {
                    Ok(text_body) => {
                        println!("[get_mcp_data] Response body (first 500 chars): {:.500}", text_body);
                        match serde_json::from_str::<ApiResponse>(&text_body) {
                            Ok(api_response) => {
                                if let Value::Array(data_array) = &api_response.data {
                                    if let Some(Value::Object(first_item)) = data_array.get(0) {
                                        if first_item.contains_key("pageInfo") && first_item.contains_key("data") {
                                            if let Ok(data_wrapper) = serde_json::from_value::<DataWrapper>(Value::Object(first_item.clone())) {
                                                let cards: Vec<MCPCard> = data_wrapper.data.iter()
                                                    .map(|api_card| MCPCard {
                                                        id: api_card.id,
                                                        title: api_card.mcpServers.name.clone(),
                                                        description: api_card.mcpServers.description.clone(),
                                                        url: api_card.url.clone(),
                                                        stars: api_card.stars,
                                                    }).collect();
                                                println!("[get_mcp_data] Successfully parsed {} cards.", cards.len());
                                                if let Some(card) = cards.first() { println!("[get_mcp_data] First parsed card: {:?}", card); }
                                                return Ok(cards);
                                            } else { println!("[get_mcp_data] Failed to parse first_item into DataWrapper."); }
                                        } else { println!("[get_mcp_data] First item in data_array missing pageInfo or data fields."); }
                                    } else { println!("[get_mcp_data] api_response.data array is empty or no object at index 0."); }
                                    println!("[get_mcp_data] API response.data not expected DataWrapper structure. Returning empty.");
                                    return Ok(Vec::new());
                                } else {
                                    println!("[get_mcp_data] API response.data is not an array. Type: {:?}. Returning empty.", api_response.data.as_str());
                                    return Ok(Vec::new());
                                }
                            }
                            Err(e) => {
                                let msg = format!("[get_mcp_data] JSON parsing error: {}. Body: {:.500}", e, text_body);
                                println!("{}", msg);
                                return Err(msg);
                            }
                        }
                    }
                    Err(e) => {
                        let msg = format!("[get_mcp_data] Failed to read response text: {}", e);
                        println!("{}", msg);
                        return Err(msg);
                    }
                }
            } else { // HTTP 에러 (500 포함)
                let error_body = response.text().await.unwrap_or_else(|e| format!("Failed to read error body: {}", e));
                let msg = format!("[get_mcp_data] Server error for {}: {}. Body: {:.500}", request_url, status, error_body);
                println!("{}", msg);
                return Err(msg);
            }
        }
        Err(e) => {
            let msg = format!("[get_mcp_data] Request error for {}: {}", request_url, e);
            println!("{}", msg);
            return Err(msg);
        }
    }
}

#[tauri::command]
pub async fn get_mcp_detail_data(
    state: State<'_, AppState>,
    id: i32,
) -> Result<MCPCardDetail, String> {
    dotenvy::dotenv().ok();
    let base_url: String = match env::var("CRAWLER_API_BASE_URL") { // .env에는 .../servers 까지만
        Ok(url_val) => url_val,
        Err(e) => {
            let msg = format!("[get_mcp_detail_data] CRAWLER_API_BASE_URL not set: {}", e);
            println!("{}", msg);
            return Err(msg);
        }
    };
    let request_url = format!("{}/{}", base_url, id); // 예: .../servers/16
    println!("[get_mcp_detail_data] Requesting detail for ID {}: {}", id, request_url);

    match state.client.get(&request_url).send().await {
        Ok(response) => {
            let status = response.status();
            println!("[get_mcp_detail_data] Response status for {}: {}", request_url, status);
            if status.is_success() {
                match response.json::<DetailApiResponse>().await {
                    Ok(detail_data) => {
                        let card_detail = MCPCardDetail {
                            id: detail_data.id,
                            title: detail_data.mcp_servers.name,
                            description: detail_data.mcp_servers.description,
                            url: detail_data.url,
                            stars: detail_data.stars,
                            args: detail_data.args,
                            env: detail_data.env,
                            // command: detail_data.command,
                        };
                        println!("[get_mcp_detail_data] Successfully parsed detail for ID {}: {:?}", id, card_detail);
                        Ok(card_detail)
                    }
                    Err(e) => {
                        let msg = format!("[get_mcp_detail_data] JSON parsing error: {}", e);
                        println!("{}", msg);
                        Err(msg)
                    }
                }
            } else {
                let error_body = response.text().await.unwrap_or_else(|e| format!("Failed to read error body: {}", e));
                let msg = format!("[get_mcp_detail_data] Server error {}: {}. Body: {:.500}", request_url, status, error_body);
                println!("{}", msg);
                Err(msg)
            }
        }
        Err(e) => {
            let msg = format!("[get_mcp_detail_data] Request error {}: {}", request_url, e);
            println!("{}", msg);
            Err(msg)
        }
    }
}

#[tauri::command]
pub async fn add_mcp_server_config(
    app: tauri::AppHandle,
    server_name: String,
    server_config: MCPServerConfig,
) -> Result<(), String> {
    let config_path = match std::env::consts::OS {
        "windows" => {
            let appdata_dir = app.path().app_data_dir()
                .map_err(|e| format!("Failed to get AppData directory: {}", e))?;
            let claude_config_base_dir = appdata_dir.parent()
                .ok_or_else(|| "Failed to get parent of AppData dir".to_string())?
                .join("Claude");
            if !claude_config_base_dir.exists() {
                fs::create_dir_all(&claude_config_base_dir)
                    .map_err(|e| format!("Failed to create Claude directory: {}", e))?;
            }
            claude_config_base_dir.join("claude_desktop_config.json")
        }
        _ => return Err("This function is currently only supported on Windows".to_string()),
    };

    let mut config: ClaudeDesktopConfig = if config_path.exists() {
        let config_str = fs::read_to_string(&config_path)
            .map_err(|e| format!("Failed to read config file: {}", e))?;
        serde_json::from_str(&config_str).unwrap_or_default()
    } else {
        ClaudeDesktopConfig::default()
    };

    let mut servers = config.mcpServers.unwrap_or_default();
    servers.insert(server_name, server_config);
    config.mcpServers = Some(servers);

    let config_json = serde_json::to_string_pretty(&config)
        .map_err(|e| format!("Failed to serialize config: {}", e))?;
    fs::write(&config_path, config_json)
        .map_err(|e| format!("Failed to write config file: {}", e))?;
    Ok(())
}

#[tauri::command]
pub async fn restart_claude_desktop(_app: tauri::AppHandle) -> Result<(), String> {
    println!("Attempting to restart Claude Desktop...");
    let possible_process_names = ["claude.exe"];
    let mut found_and_killed = false;

    for process_name in possible_process_names.iter() {
        let output = StdCommand::new("tasklist")
            .args(["/FI", &format!("IMAGENAME eq {}", process_name), "/FO", "CSV", "/NH"])
            .output()
            .map_err(|e| format!("Failed to execute tasklist: {}", e))?;
        let stdout = String::from_utf8_lossy(&output.stdout);
        for line in stdout.lines() {
            let parts: Vec<&str> = line.split(',').collect();
            if parts.len() > 1 {
                let pid_str = parts[1].trim_matches('"');
                println!("Found {} process with PID: {}. Attempting to kill...", process_name, pid_str);
                StdCommand::new("taskkill").args(["/F", "/PID", pid_str]).output()
                    .map_err(|e| format!("Failed to kill process {}: {}", pid_str, e))?;
                println!("Process {} (PID: {}) killed.", process_name, pid_str);
                found_and_killed = true;
            }
        }
    }

    if !found_and_killed {
        println!("No running Claude Desktop process found by name. Checking all processes for 'claude'...");
         let output_all = StdCommand::new("tasklist")
            .args(["/FO", "CSV", "/NH"])
            .output()
            .map_err(|e| format!("Failed to execute tasklist for all processes: {}", e))?;
        let stdout_all = String::from_utf8_lossy(&output_all.stdout);
        for line in stdout_all.lines() {
            if line.to_lowercase().contains("claude") {
                 let parts: Vec<&str> = line.split(',').collect();
                 if parts.len() > 0 {
                    let process_name_from_all = parts[0].trim_matches('"');
                     println!("Found potential Claude-related process: {}. Attempting to kill by image name...", process_name_from_all);
                    StdCommand::new("taskkill").args(["/F", "/IM", process_name_from_all]).output()
                        .map_err(|e| format!("Failed to kill process {}: {}", process_name_from_all, e))?;
                    println!("Process {} killed.", process_name_from_all);
                 }
            }
        }
    }

    tokio::time::sleep(tokio::time::Duration::from_millis(1500)).await;

    let local_appdata = env::var("LOCALAPPDATA").map_err(|e| format!("Failed to get LOCALAPPDATA: {}", e))?;
    let claude_exe_path_str = format!("{}\\AnthropicClaude\\Claude.exe", local_appdata);
    let claude_path = Path::new(&claude_exe_path_str);

    println!("Attempting to start Claude from: {}", claude_path.display());
    if !claude_path.exists() {
        return Err(format!("Claude.exe not found at {}", claude_path.display()));
    }

    StdCommand::new(claude_path).spawn()
        .map(|_| println!("Claude Desktop restarted successfully."))
        .map_err(|e| format!("Failed to start Claude Desktop: {}", e))
}

// extract_pids_from_tasklist 함수는 restart_claude_desktop 함수 내 로직으로 통합되어 더 이상 필요 없을 수 있습니다.
// 만약 별도로 사용된다면 유지하고, 아니라면 삭제해도 됩니다.
// fn extract_pids_from_tasklist(tasklist_output: &str) -> Option<Vec<String>> {
//     let mut pids = Vec::new();
//     for line in tasklist_output.lines() { // header line skip 제거
//         let parts: Vec<&str> = line.split(',').collect();
//         if parts.len() >= 2 {
//             let pid = parts[1].trim_matches('"').to_string();
//             if !pid.is_empty() && pid.chars().all(char::is_numeric) { // 간단한 PID 유효성 검사
//                 pids.push(pid);
//             }
//         }
//     }
//     if pids.is_empty() { None } else { Some(pids) }
// }
