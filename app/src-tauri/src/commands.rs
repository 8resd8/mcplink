// app/src-tauri/src/commands.rs

use dotenvy::dotenv;
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;
use std::env;
use std::fs;
use std::path::Path;
use std::process::Command as StdCommand;
use tauri::{Manager, State};
use tokio;
use urlencoding;

// --- 기존 구조체 정의 (McpServerInfo, ApiCardData, PageInfo, DataWrapper, ApiResponse) ---
#[derive(Debug, Deserialize)]
struct McpServerInfo {
    name: String,
    description: String,
    args: Option<Vec<String>>,
    env: Option<serde_json::Map<String, Value>>,
    command: Option<String>,
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
    #[serde(rename = "mcpServer")]
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
    #[serde(rename = "mcpServers")]
    mcpServers: Vec<ApiCardData>,
}

// 이 ApiResponse 구조체는 get_mcp_data와 get_mcp_detail_data 모두에서 사용될 수 있습니다.
#[derive(Debug, Deserialize)]
#[allow(non_snake_case)]
struct ApiResponse {
    // data 필드를 Value로 하여 유연하게 처리
    timestamp: String,
    message: String,
    code: String,
    data: Value, // 실제 데이터는 이 안에 Value 형태로 들어옴
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

// DetailApiResponse is now designed to parse the object obtained from `api_response_wrapper.data.get("mcpServer")`
#[derive(Debug, Deserialize)]
struct DetailApiResponse {
    id: i32,
    url: String,
    stars: i32,
    #[serde(rename = "mcpServer")] // This inner mcpServer object holds the actual server details
    mcp_server_info: McpServerInfo,
    scanned: Option<bool>,
    #[serde(rename = "type")]
    _type: Option<String>,
    views: Option<i32>,
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
    pub command: Option<String>,
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
    let crawler_api_base_url: String =
        std::env::var("CRAWLER_API_BASE_URL").expect("CRAWLER_API_BASE_URL must be set");
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
        if term.is_empty() {
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
                                if let Value::Object(data_obj) = &api_response.data {
                                    // DataWrapper 파싱 전 로그 추가
                                    println!("[get_mcp_data] Attempting to parse data_obj into DataWrapper: {:?}", data_obj);
                                    match serde_json::from_value::<DataWrapper>(Value::Object(
                                        data_obj.clone(),
                                    )) {
                                        Ok(data_wrapper) => {
                                            let cards: Vec<MCPCard> = data_wrapper
                                                .mcpServers
                                                .iter()
                                                .map(|api_card| MCPCard {
                                                    id: api_card.id,
                                                    title: api_card.mcpServers.name.clone(),
                                                    description: api_card
                                                        .mcpServers
                                                        .description
                                                        .clone(),
                                                    url: api_card.url.clone(),
                                                    stars: api_card.stars,
                                                })
                                                .collect();
                                            println!(
                                                "[get_mcp_data] Successfully parsed {} cards.",
                                                cards.len()
                                            );
                                            if let Some(card) = cards.first() {
                                                println!(
                                                    "[get_mcp_data] First parsed card: {:?}",
                                                    card
                                                );
                                            }
                                            return Ok(cards);
                                        }
                                        Err(e) => {
                                            // DataWrapper 파싱 실패 시 상세 로그
                                            println!("[get_mcp_data] Failed to parse data object into DataWrapper: {}. Data object was: {:?}", e, data_obj);
                                            return Err(format!("[get_mcp_data] Failed to parse data into DataWrapper: {}", e));
                                        }
                                    }
                                } else {
                                    println!("[get_mcp_data] API response.data is not an object or not found. Data: {:?}. Returning empty.", api_response.data);
                                    return Ok(Vec::new());
                                }
                            }
                            Err(e) => {
                                let msg = format!("[get_mcp_data] JSON parsing error for ApiResponse: {}. Body: {:.500}", e, text_body);
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
                let error_body = response
                    .text()
                    .await
                    .unwrap_or_else(|e| format!("Failed to read error body: {}", e));
                let msg = format!(
                    "[get_mcp_data] Server error for {}: {}. Body: {:.500}",
                    request_url, status, error_body
                );
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
    let base_url: String = match env::var("CRAWLER_API_BASE_URL") {
        Ok(url_val) => url_val,
        Err(e) => {
            let msg = format!("[get_mcp_detail_data] CRAWLER_API_BASE_URL not set: {}", e);
            return Err(msg);
        }
    };
    let request_url = format!("{}/{}", base_url, id);
    println!(
        "[get_mcp_detail_data] Requesting detail for ID {}: {}",
        id, request_url
    );

    match state.client.get(&request_url).send().await {
        Ok(response) => {
            let status = response.status();
            println!(
                "[get_mcp_detail_data] Response status for {}: {}",
                request_url, status
            );
            if status.is_success() {
                match response.json::<ApiResponse>().await {
                    // Outer ApiResponse wrapper
                    Ok(api_response_wrapper) => {
                        println!("[get_mcp_detail_data] Successfully parsed outer ApiResponse. Data field: {:?}", api_response_wrapper.data);

                        // Check if api_response_wrapper.data is an Object and get the inner "mcpServer" value
                        if let Value::Object(data_map) = api_response_wrapper.data {
                            if let Some(inner_mcp_server_value) = data_map.get("mcpServer") {
                                println!("[get_mcp_detail_data] Extracted inner_mcp_server_value (target for DetailApiResponse): {:?}", inner_mcp_server_value);

                                // Now parse this inner_mcp_server_value into DetailApiResponse
                                match serde_json::from_value::<DetailApiResponse>(
                                    inner_mcp_server_value.clone(),
                                ) {
                                    Ok(detail_data) => {
                                        let card_detail = MCPCardDetail {
                                            id: detail_data.id,
                                            title: detail_data.mcp_server_info.name, // Name from McpServerInfo
                                            description: detail_data.mcp_server_info.description, // Description from McpServerInfo
                                            url: detail_data.url,
                                            stars: detail_data.stars,
                                            args: detail_data.mcp_server_info.args,
                                            env: detail_data.mcp_server_info.env,
                                            command: detail_data.mcp_server_info.command,
                                        };
                                        println!("[get_mcp_detail_data] Successfully parsed inner data to MCPCardDetail: {:?}", card_detail);
                                        Ok(card_detail)
                                    }
                                    Err(e_inner_struct) => {
                                        let msg = format!("[get_mcp_detail_data] Failed to parse inner_mcp_server_value into DetailApiResponse: {}. Value was: {:?}", e_inner_struct, inner_mcp_server_value);
                                        println!("{}", msg);
                                        Err(msg)
                                    }
                                }
                            } else {
                                let msg = format!("[get_mcp_detail_data] Key 'mcpServer' not found inside ApiResponse.data. ApiResponse.data was: {:?}", data_map);
                                println!("{}", msg);
                                Err(msg)
                            }
                        } else {
                            let msg = format!("[get_mcp_detail_data] ApiResponse.data is not an object. It was: {:?}", api_response_wrapper.data);
                            println!("{}", msg);
                            Err(msg)
                        }
                    }
                    Err(e_outer) => {
                        let msg = format!("[get_mcp_detail_data] Failed to parse outer ApiResponse: {}. Check if the overall response matches ApiResponse structure.", e_outer);
                        println!("{}", msg);
                        Err(msg)
                    }
                }
            } else {
                let error_body = response
                    .text()
                    .await
                    .unwrap_or_else(|e| format!("Failed to read error body: {}", e));
                let msg = format!(
                    "[get_mcp_detail_data] Server error {}: {}. Body: {:.500}",
                    request_url, status, error_body
                );
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
            let appdata_dir = app
                .path()
                .app_data_dir()
                .map_err(|e| format!("Failed to get AppData directory: {}", e))?;
            let claude_config_base_dir = appdata_dir
                .parent()
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
    let config_path = match std::env::consts::OS {
        "windows" => {
            let appdata = app
                .path()
                .app_data_dir()
                .map_err(|e| format!("Failed to get AppData directory: {}", e))?;
            let claude_dir = appdata.parent().unwrap().join("Claude");
            if !claude_dir.exists() {
                return Err("Claude directory does not exist".to_string());
            }
            claude_dir.join("claude_desktop_config.json")
        }
        _ => {
            return Err("This function is currently only supported on Windows".to_string());
        }
    };

    if !config_path.exists() {
        return Err("Configuration file does not exist".to_string());
    }

    let config_str = fs::read_to_string(&config_path)
        .map_err(|e| format!("Failed to read config file: {}", e))?;

    let mut config = match serde_json::from_str::<ClaudeDesktopConfig>(&config_str) {
        Ok(config) => config,
        Err(e) => return Err(format!("Failed to parse config file: {}", e)),
    };

    if config.mcpServers.is_none() {
        return Err(format!("No MCP servers are installed"));
    }

    let mut servers = config.mcpServers.unwrap_or_default();

    if !servers.contains_key(&server_name) {
        return Err(format!("MCP server '{}' not found", server_name));
    }

    servers.remove(&server_name);
    config.mcpServers = Some(servers);

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
            .args([
                "/FI",
                &format!("IMAGENAME eq {}", process_name),
                "/FO",
                "CSV",
                "/NH",
            ])
            .output()
            .map_err(|e| format!("Failed to execute tasklist: {}", e))?;
        let stdout = String::from_utf8_lossy(&output.stdout);
        for line in stdout.lines() {
            let parts: Vec<&str> = line.split(',').collect();
            if parts.len() > 1 {
                let pid_str = parts[1].trim_matches('"');
                println!(
                    "Found {} process with PID: {}. Attempting to kill...",
                    process_name, pid_str
                );
                StdCommand::new("taskkill")
                    .args(["/F", "/PID", pid_str])
                    .output()
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
                    StdCommand::new("taskkill")
                        .args(["/F", "/IM", process_name_from_all])
                        .output()
                        .map_err(|e| {
                            format!("Failed to kill process {}: {}", process_name_from_all, e)
                        })?;
                    println!("Process {} killed.", process_name_from_all);
                }
            }
        }
    }

    tokio::time::sleep(tokio::time::Duration::from_millis(1500)).await;

    let local_appdata =
        env::var("LOCALAPPDATA").map_err(|e| format!("Failed to get LOCALAPPDATA: {}", e))?;
    let claude_exe_path_str = format!("{}\\AnthropicClaude\\Claude.exe", local_appdata);
    let claude_path = Path::new(&claude_exe_path_str);

    println!("Attempting to start Claude from: {}", claude_path.display());
    if !claude_path.exists() {
        return Err(format!("Claude.exe not found at {}", claude_path.display()));
    }

    StdCommand::new(claude_path)
        .spawn()
        .map(|_| println!("Claude Desktop restarted successfully."))
        .map_err(|e| format!("Failed to start Claude Desktop: {}", e))
}
