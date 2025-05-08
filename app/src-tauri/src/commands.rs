// app/src-tauri/src/commands.rs

use reqwest::Client;
use serde::{Deserialize, Serialize};
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
    #[serde(rename = "mcpServer")] // JSON의 "mcpServer" 키를 이 필드에 매핑
    mcpServers: McpServerInfo, // 필드명 예시 (McpServerInfo는 name, description을 가짐)
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
    #[serde(rename = "mcpServers")] // JSON의 "mcpServers" 키를 이 필드에 매핑
    mcpServers: Vec<ApiCardData>,    // data -> mcpServers로 이름 변경
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
    search_term: Option<String>,
) -> Result<Vec<MCPCard>, String> {
    dotenvy::dotenv().ok();

    let base_url: String = match env::var("CRAWLER_API_BASE_URL") {
        Ok(url_val) => url_val,
        Err(e) => {
            let msg = format!("[get_mcp_data] CRAWLER_API_BASE_URL not set: {}", e);
            println!("{}", msg);
            return Err(msg);
        }
    };

    let request_url = if let Some(term) = search_term {
        if term.is_empty(){
             base_url.to_string()
        } else {
            let encoded_term = urlencoding::encode(&term);
            let search_url = format!("{}/search?name={}", base_url, encoded_term);
            search_url
        }
    } else {
        base_url.to_string()
    };

    match state.client.get(&request_url).send().await {
        Ok(response) => {
            let status = response.status();

            if status.is_success() {
                match response.text().await {
                    Ok(text_body) => {
                        println!("[get_mcp_data] RAW API Response Body: {}", text_body);
                        match serde_json::from_str::<ApiResponse>(&text_body) {
                            Ok(api_response) => {
                                // data가 객체일 때 DataWrapper로 파싱
                                if let Value::Object(data_obj) = &api_response.data {
                                    if let Ok(data_wrapper) = serde_json::from_value::<DataWrapper>(Value::Object(data_obj.clone())) {
                                        let cards: Vec<MCPCard> = data_wrapper.mcpServers.iter()
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
                                    } else {
                                        println!("[get_mcp_data] Failed to parse data object into DataWrapper.");
                                    }
                                } else {
                                    println!("[get_mcp_data] API response.data is not an object. Type: {:?}. Returning empty.", api_response.data);
                                }
                                return Ok(Vec::new());
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
            } else {
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

/// MCP 서버 설정을 Claude Desktop 설정 파일에서 삭제
#[tauri::command]
pub async fn remove_mcp_server_config(
    app: tauri::AppHandle,
    server_name: String,
) -> Result<(), String> {
    // 설정 파일 경로를 생성합니다.
    let config_path = match std::env::consts::OS {
        "windows" => {
            // Windows의 경우 %APPDATA%\Claude\claude_desktop_config.json
            let appdata = app
                .path()
                .app_data_dir()
                .map_err(|e| format!("Failed to get AppData directory: {}", e))?;
            let claude_dir = appdata.parent().unwrap().join("Claude");

            // Claude 디렉토리가 없으면 에러 반환
            if !claude_dir.exists() {
                return Err("Claude directory does not exist".to_string());
            }

            claude_dir.join("claude_desktop_config.json")
        }
        _ => {
            return Err("This function is currently only supported on Windows".to_string());
        }
    };

    // 설정 파일이 존재하는지 확인
    if !config_path.exists() {
        return Err("Configuration file does not exist".to_string());
    }

    // 설정 파일 읽기
    let config_str = fs::read_to_string(&config_path)
        .map_err(|e| format!("Failed to read config file: {}", e))?;

    // 설정 파일 파싱
    let mut config = match serde_json::from_str::<ClaudeDesktopConfig>(&config_str) {
        Ok(config) => config,
        Err(e) => return Err(format!("Failed to parse config file: {}", e)),
    };

    // mcpServers가 없으면 서버가 설치되어 있지 않은 것
    if config.mcpServers.is_none() {
        return Err(format!("No MCP servers are installed"));
    }

    // MCP 서버 맵 가져오기
    let mut servers = config.mcpServers.unwrap_or_default();

    // 서버가 존재하는지 확인
    if !servers.contains_key(&server_name) {
        return Err(format!("MCP server '{}' not found", server_name));
    }

    // 서버 삭제
    servers.remove(&server_name);

    // 서버 맵 업데이트
    config.mcpServers = Some(servers);

    // 설정 파일에 쓰기
    let config_json = serde_json::to_string_pretty(&config)
        .map_err(|e| format!("Failed to serialize config: {}", e))?;

    fs::write(&config_path, config_json)
        .map_err(|e| format!("Failed to write config file: {}", e))?;

    Ok(())
}

/// Claude Desktop 애플리케이션을 재시작
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
