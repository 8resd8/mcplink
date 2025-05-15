// app/src-tauri/src/commands.rs

use dotenvy::dotenv;
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json::{json, Map, Value};
use std::collections::HashMap;
use std::{env, fs, path::PathBuf, process::Command as StdCommand};
use tauri::{AppHandle, Manager, State};
use tauri_plugin_notification::NotificationExt;
use tokio::time::{sleep, Duration};
use urlencoding::encode;

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

// 페이지 정보를 포함한 응답 구조체
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PageInfoResponse {
    pub hasNextPage: bool,
    pub endCursor: Option<i32>,
    pub totalItems: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPCardResponse {
    pub cards: Vec<MCPCard>,
    pub pageInfo: PageInfoResponse,
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
    pub env: Option<Map<String, Value>>,
    pub command: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPServerConfig {
    pub command: String,
    pub args: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<Map<String, Value>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct ClaudeDesktopConfig {
    pub mcpServers: Option<HashMap<String, MCPServerConfig>>,
    #[serde(flatten)]
    pub other: Map<String, Value>,
}

pub struct AppState {
    pub client: Client,
}

// --- Tauri Commands ---

#[tauri::command]
pub async fn get_mcp_data(
    state: State<'_, AppState>,
    search_term: Option<String>,
    cursor_id: Option<i32>,
) -> Result<MCPCardResponse, String> {
    dotenv().ok();

    let base_url: String = match env::var("CRAWLER_API_BASE_URL") {
        Ok(url_val) => url_val,
        Err(e) => {
            let msg = format!("[get_mcp_data] CRAWLER_API_BASE_URL not set: {}", e);
            println!("{}", msg);
            return Err(msg);
        }
    };

    // URL 구성에 커서 ID 추가
    let request_url = if let Some(term) = search_term {
        if term.is_empty() {
            if let Some(cursor) = cursor_id {
                format!("{}?size=10&cursorId={}", base_url, cursor)
            } else {
                format!("{}?size=10", base_url)
            }
        } else {
            let encoded_term = encode(&term);
            let search_url = format!("{}/search?name={}", base_url, encoded_term);
            search_url
        }
    } else {
        if let Some(cursor) = cursor_id {
            format!("{}?size=10&cursorId={}", base_url, cursor)
        } else {
            format!("{}?size=10", base_url)
        }
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

                                            // 페이지 정보 추출
                                            let end_cursor = match data_wrapper.pageInfo.endCursor {
                                                Some(Value::Number(n)) => {
                                                    n.as_i64().map(|x| x as i32)
                                                }
                                                _ => None,
                                            };

                                            let response = MCPCardResponse {
                                                cards,
                                                pageInfo: PageInfoResponse {
                                                    hasNextPage: data_wrapper.pageInfo.hasNextPage,
                                                    endCursor: end_cursor,
                                                    totalItems: data_wrapper.pageInfo.totalItems,
                                                },
                                            };

                                            return Ok(response);
                                        }
                                        Err(e) => {
                                            // DataWrapper 파싱 실패 시 상세 로그
                                            println!("[get_mcp_data] Failed to parse data object into DataWrapper: {}. Data object was: {:?}", e, data_obj);
                                            return Err(format!("[get_mcp_data] Failed to parse data into DataWrapper: {}", e));
                                        }
                                    }
                                } else {
                                    println!("[get_mcp_data] API response.data is not an object or not found. Data: {:?}. Returning empty.", api_response.data);
                                    return Ok(MCPCardResponse {
                                        cards: Vec::new(),
                                        pageInfo: PageInfoResponse {
                                            hasNextPage: false,
                                            endCursor: None,
                                            totalItems: 0,
                                        },
                                    });
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
    dotenv().ok();
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

/// MCP 서버 설정을 Claude Desktop 설정 파일에 추가
#[tauri::command]
pub async fn add_mcp_server_config(
    app: AppHandle,
    server_name: String,
    server_config: MCPServerConfig,
    server_id: i64,
) -> Result<(), String> {
    // 설정 파일 경로를 생성합니다.
    let config_path = match env::consts::OS {
        "windows" => {
            // Windows의 경우 %APPDATA%\Claude\claude_desktop_config.json
            let appdata = app
                .path()
                .app_data_dir()
                .map_err(|e| format!("Failed to get AppData directory: {}", e))?;
            let claude_dir = appdata.parent().unwrap().join("Claude");

            // Claude 디렉토리가 없으면 생성
            if !claude_dir.exists() {
                fs::create_dir_all(&claude_dir)
                    .map_err(|e| format!("Failed to create Claude directory: {}", e))?;
            }

            claude_dir.join("claude_desktop_config.json")
        }
        _ => {
            return Err("This function is currently only supported on Windows".to_string());
        }
    };

    // mcplink 파일 경로 생성 (claude_desktop_config.json과 같은 위치)
    let mcplink_path = config_path
        .parent()
        .unwrap()
        .join("mcplink_desktop_config.json");

    // 설정 파일 읽기 (파일이 없으면 빈 객체 생성)
    let mut config = if config_path.exists() {
        let config_str = fs::read_to_string(&config_path)
            .map_err(|e| format!("Failed to read config file: {}", e))?;

        match serde_json::from_str::<ClaudeDesktopConfig>(&config_str) {
            Ok(config) => config,
            Err(_) => {
                // 파일이 있지만 형식이 잘못된 경우, 다른 필드는 유지하면서 새로 생성
                match serde_json::from_str::<Value>(&config_str) {
                    Ok(value) => {
                        if let Value::Object(map) = value {
                            ClaudeDesktopConfig {
                                mcpServers: None,
                                other: map,
                            }
                        } else {
                            ClaudeDesktopConfig::default()
                        }
                    }
                    Err(_) => ClaudeDesktopConfig::default(),
                }
            }
        }
    } else {
        ClaudeDesktopConfig::default()
    };

    // MCP 서버 맵 가져오기 또는 생성
    let mut servers = config.mcpServers.unwrap_or_default();

    // 서버 설정 추가 또는 업데이트
    servers.insert(server_name.clone(), server_config);
    config.mcpServers = Some(servers);

    // 설정 파일에 쓰기
    let config_json = serde_json::to_string_pretty(&config)
        .map_err(|e| format!("Failed to serialize config: {}", e))?;

    fs::write(&config_path, config_json)
        .map_err(|e| format!("Failed to write config file: {}", e))?;

    // mcplink_desktop_config.json 파일에 server_id와 server_name 추가
    let mut mcplink_config = if mcplink_path.exists() {
        let mcplink_str = fs::read_to_string(&mcplink_path)
            .map_err(|e| format!("Failed to read mcplink config file: {}", e))?;

        match serde_json::from_str::<Map<String, Value>>(&mcplink_str) {
            Ok(map) => map,
            Err(_) => Map::new(),
        }
    } else {
        Map::new()
    };

    // server_id를 문자열 키로 변환하고 server_name을 값으로 저장
    mcplink_config.insert(server_id.to_string(), Value::String(server_name));

    // mcplink 설정 파일에 쓰기
    let mcplink_json = serde_json::to_string_pretty(&mcplink_config)
        .map_err(|e| format!("Failed to serialize mcplink config: {}", e))?;

    fs::write(&mcplink_path, mcplink_json)
        .map_err(|e| format!("Failed to write mcplink config file: {}", e))?;

    Ok(())
}

/// MCP 서버 설정을 Claude Desktop 설정 파일에서 삭제
#[tauri::command]
pub async fn remove_mcp_server_config(app: AppHandle, server_name: String) -> Result<(), String> {
    // 설정 파일 경로를 생성합니다.
    let config_path = match env::consts::OS {
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

    // mcplink 파일 경로 생성 (claude_desktop_config.json과 같은 위치)
    let mcplink_path = config_path
        .parent()
        .unwrap()
        .join("mcplink_desktop_config.json");

    // 설정 파일이 존재하는지 확인
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
        return Err("No MCP servers are installed".to_string());
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

    // mcplink_desktop_config.json 파일에서 server_name과 관련된 항목 제거
    if mcplink_path.exists() {
        let mcplink_str = fs::read_to_string(&mcplink_path)
            .map_err(|e| format!("Failed to read mcplink config file: {}", e))?;

        match serde_json::from_str::<Map<String, Value>>(&mcplink_str) {
            Ok(mut mcplink_config) => {
                // server_name과 일치하는 모든 항목을 찾아 삭제
                let mut keys_to_remove = Vec::new();
                for (key, value) in &mcplink_config {
                    if let Value::String(name) = value {
                        if name == &server_name {
                            keys_to_remove.push(key.clone());
                        }
                    }
                }

                // 찾은 키들을 삭제
                for key in keys_to_remove {
                    mcplink_config.remove(&key);
                }

                // 업데이트된 설정을 파일에 쓰기
                let mcplink_json = serde_json::to_string_pretty(&mcplink_config)
                    .map_err(|e| format!("Failed to serialize mcplink config: {}", e))?;

                fs::write(&mcplink_path, mcplink_json)
                    .map_err(|e| format!("Failed to write mcplink config file: {}", e))?;
            }
            Err(e) => return Err(format!("Failed to parse mcplink config file: {}", e)),
        }
    }

    Ok(())
}

/// Claude Desktop 애플리케이션을 재시작
#[tauri::command]
pub async fn restart_claude_desktop(_app: AppHandle) -> Result<(), String> {
    println!("Attempting to restart Claude Desktop...");

    // 1) claude.exe 프로세스 모두 종료 (정확한 필터 사용)
    let kill_status = StdCommand::new("taskkill")
        .args([
            "/F", // 강제 종료
            "/T", // 자식 프로세스까지 종료
            "/FI",
            "IMAGENAME eq claude.exe",
        ])
        .status()
        .map_err(|e| format!("Failed to execute taskkill: {}", e))?;
    match kill_status.code() {
        Some(0) => println!("✅ All existing Claude processes terminated."),
        Some(128) => println!("✅ No Claude processes to terminate (exit code 128)."),
        Some(c) => println!("⚠️ taskkill abnormal exit code: {}", c),
        None => println!("⚠️ taskkill terminated by signal."),
    }

    // 2) 충분히 대기 (2초)
    sleep(Duration::from_millis(2000)).await;

    // 3) 종료 확인
    let check = StdCommand::new("tasklist")
        .args(["/FI", "IMAGENAME eq claude.exe", "/NH"])
        .output()
        .map_err(|e| format!("Failed to execute tasklist: {}", e))?;
    let running = String::from_utf8_lossy(&check.stdout);
    if running.trim().is_empty() {
        println!("✅ Confirmed all claude.exe processes are terminated.");
    } else {
        println!("⚠️ Still running processes:\n{}", running);
    }

    // 4) 캐시 디렉터리 미리 생성 (권한 문제 방지)
    let cache_dir: PathBuf = {
        let base =
            env::var("LOCALAPPDATA").map_err(|e| format!("Failed to get LOCALAPPDATA: {}", e))?;
        let dir = PathBuf::from(&base).join("AnthropicClaude").join("Cache");
        fs::create_dir_all(&dir)
            .map_err(|e| format!("Failed to create cache dir {:?}: {}", dir, e))?;
        dir
    };
    let cache_dir_str = cache_dir.to_string_lossy();

    // 5) 실행 파일 경로 준비
    let claude_exe: PathBuf = {
        let base =
            env::var("LOCALAPPDATA").map_err(|e| format!("Failed to get LOCALAPPDATA: {}", e))?;
        PathBuf::from(base)
            .join("AnthropicClaude")
            .join("Claude.exe")
    };
    println!("Attempting to start Claude from: {}", claude_exe.display());
    if !claude_exe.exists() {
        return Err(format!("Claude.exe not found at {}", claude_exe.display()));
    }

    // 6) Electron 런타임 플래그만 전달 → URL 파싱 에러 방지
    let child = StdCommand::new(&claude_exe)
        .args([
            "--user-data-dir",
            &cache_dir_str,
            "--disable-gpu-shader-disk-cache",
            "--disable-gpu",
        ])
        .spawn()
        .map_err(|e| format!("Failed to start Claude Desktop: {}", e))?;
    println!(
        "✅ Claude Desktop restarted successfully (PID {}).",
        child.id()
    );

    Ok(())
}

#[tauri::command]
pub async fn get_installed_mcp_data(
    state: State<'_, AppState>,
    server_ids: Vec<i32>,
    cursor_id: Option<i32>,
) -> Result<MCPCardResponse, String> {
    dotenv().ok();

    let base_url: String = match env::var("CRAWLER_API_BASE_URL") {
        Ok(url_val) => url_val,
        Err(e) => {
            let msg = format!(
                "[get_installed_mcp_data] CRAWLER_API_BASE_URL not set: {}",
                e
            );
            println!("{}", msg);
            return Err(msg);
        }
    };

    // Use the batch endpoint
    let mut request_url = format!("{}/batch", base_url);

    // Add cursorId query parameter if present
    if let Some(cursor) = cursor_id {
        request_url.push_str(&format!("?size=10&cursorId={}", cursor)); // Assuming default size 10, adjust if needed
    } else {
        request_url.push_str("?size=10"); // Default size if no cursor
    }

    // Create the request body
    let request_body = json!({ "serverIds": server_ids });
    println!(
        "[get_installed_mcp_data] Requesting batch data for IDs: {:?}, URL: {}, Body: {}",
        server_ids, request_url, request_body
    );

    // Send POST request
    match state
        .client
        .post(&request_url)
        .json(&request_body) // Send the body as JSON
        .send()
        .await
    {
        Ok(response) => {
            let status = response.status();
            println!(
                "[get_installed_mcp_data] Response status for {}: {}",
                request_url, status
            );

            if status.is_success() {
                match response.text().await {
                    // Read as text first for logging
                    Ok(text_body) => {
                        println!(
                            "[get_installed_mcp_data] RAW API Response Body: {}",
                            text_body
                        );
                        match serde_json::from_str::<ApiResponse>(&text_body) {
                            // Parse outer ApiResponse
                            Ok(api_response) => {
                                if let Value::Object(data_obj) = &api_response.data {
                                    println!("[get_installed_mcp_data] Attempting to parse data_obj into DataWrapper: {:?}", data_obj);
                                    // Parse the inner data (which contains pageInfo and mcpServers) using DataWrapper
                                    match serde_json::from_value::<DataWrapper>(Value::Object(
                                        data_obj.clone(),
                                    )) {
                                        Ok(data_wrapper) => {
                                            let cards: Vec<MCPCard> = data_wrapper
                                                .mcpServers
                                                .iter()
                                                .map(|api_card| MCPCard {
                                                    id: api_card.id,
                                                    // Assuming the structure within mcpServers array is the same as in get_mcp_data
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
                                                "[get_installed_mcp_data] Successfully parsed {} cards.",
                                                cards.len()
                                            );

                                            // Extract page info
                                            let end_cursor = match data_wrapper.pageInfo.endCursor {
                                                Some(Value::Number(n)) => {
                                                    n.as_i64().map(|x| x as i32)
                                                }
                                                _ => None,
                                            };

                                            let response = MCPCardResponse {
                                                cards,
                                                pageInfo: PageInfoResponse {
                                                    hasNextPage: data_wrapper.pageInfo.hasNextPage,
                                                    endCursor: end_cursor,
                                                    totalItems: data_wrapper.pageInfo.totalItems,
                                                },
                                            };

                                            return Ok(response);
                                        }
                                        Err(e) => {
                                            println!("[get_installed_mcp_data] Failed to parse data object into DataWrapper: {}. Data object was: {:?}", e, data_obj);
                                            return Err(format!("[get_installed_mcp_data] Failed to parse data into DataWrapper: {}", e));
                                        }
                                    }
                                } else {
                                    println!("[get_installed_mcp_data] API response.data is not an object or not found. Data: {:?}. Returning empty.", api_response.data);
                                    // Return empty response consistent with get_mcp_data
                                    return Ok(MCPCardResponse {
                                        cards: Vec::new(),
                                        pageInfo: PageInfoResponse {
                                            hasNextPage: false,
                                            endCursor: None,
                                            totalItems: 0,
                                        },
                                    });
                                }
                            }
                            Err(e) => {
                                let msg = format!("[get_installed_mcp_data] JSON parsing error for ApiResponse: {}. Body: {:.500}", e, text_body);
                                println!("{}", msg);
                                return Err(msg);
                            }
                        }
                    }
                    Err(e) => {
                        let msg = format!(
                            "[get_installed_mcp_data] Failed to read response text: {}",
                            e
                        );
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
                    "[get_installed_mcp_data] Server error for {}: {}. Body: {:.500}",
                    request_url, status, error_body
                );
                println!("{}", msg);
                return Err(msg);
            }
        }
        Err(e) => {
            let msg = format!(
                "[get_installed_mcp_data] Request error for {}: {}",
                request_url, e
            );
            println!("{}", msg);
            return Err(msg);
        }
    }
}

/// Reads the content of mcplink_desktop_config.json and returns it as a string.
#[tauri::command]
pub fn read_mcplink_config_content(app: AppHandle) -> Result<String, String> {
    let config_path = match env::consts::OS {
        "windows" => {
            let appdata = app
                .path()
                .app_data_dir()
                .map_err(|e| format!("Failed to get AppData directory: {}", e))?;
            let claude_dir = appdata
                .parent()
                .ok_or("Failed to get parent of AppData")?
                .join("Claude");
            claude_dir.join("mcplink_desktop_config.json")
        }
        _ => return Err("Path resolution currently only supported on Windows".to_string()),
    };

    if !config_path.exists() {
        // If the file doesn't exist, return an empty JSON object string or a specific error/signal
        // For now, returning an empty JSON object string to be parsed by frontend
        // Or, you could return an Err to be handled specifically by the frontend.
        // For example: return Err("mcplink_config_not_found".to_string());
        println!(
            "mcplink_desktop_config.json not found at {:?}, returning empty JSON object string.",
            config_path
        );
        return Ok("{}".to_string());
    }

    fs::read_to_string(config_path)
        .map_err(|e| format!("Failed to read mcplink_desktop_config.json: {}", e))
}

#[tauri::command]
pub async fn show_popup(app: AppHandle, tag: String) -> Result<(), String> {
    // --- CLAUDE CODE 수정 시작 ---
    // 알림 권한 상태 확인
    let permission_state = match app.notification().permission_state() {
        Ok(state) => state,
        Err(e) => {
            eprintln!("[Notification] Failed to get permission state: {}", e);
            return Err(format!("Failed to get notification permission: {}", e));
        }
    };
    
    println!("[Notification] Current permission state: {:?}", permission_state);
    
    // 권한이 없는 경우 요청
    use tauri_plugin_notification::PermissionState;
    if permission_state != PermissionState::Granted {
        println!("[Notification] Permission not granted, requesting...");
        match app.notification().request_permission() {
            Ok(new_state) => {
                println!("[Notification] New permission state: {:?}", new_state);
                if new_state != PermissionState::Granted {
                    return Err("Notification permission was denied".to_string());
                }
            }
            Err(e) => {
                eprintln!("[Notification] Failed to request permission: {}", e);
                return Err(format!("Failed to request notification permission: {}", e));
            }
        }
    }
    
    // 알림 본문 생성 - 태그 정보 포함
    let notification_body = format!("선택된 키워드: {}. 클릭하여 확인하세요.", tag);

    // --- CLAUDE CODE 수정 시작 ---
    // 알림 옵션 설정 - 간소화된 방식으로 알림 생성
    let builder = app
        .notification()
        .builder()
        .title("추천 확인")
        .body(&notification_body)
        .icon("icons/icon.png");
    
    // 알림 본문에 태그 정보를 포함시키는 것으로 대체 (이미 포함되어 있음)
    println!("[Notification] 태그 정보를 알림 본문에 포함: {}", notification_body);
    
    // 알림 전송 및 결과 처리
    match builder.show() {
    // --- CLAUDE CODE 수정 끝 ---
        Ok(_) => {
            println!("[Notification Sent] Tag: {}, Body: {}", tag, notification_body);
            // 알림이 제대로 전송되었는지 로그에 추가 정보 출력
            if let Ok(permission_state) = app.notification().permission_state() {
                println!("[Notification] Permission state: {:?}", permission_state);
            }
            Ok(())
        }
        Err(e) => {
            eprintln!("[Notification Send Error] Tag: {}, Error: {}", tag, e);
            Err(format!("Failed to send notification: {}", e))
        }
    }
    // --- CLAUDE CODE 수정 끝 ---
}

// --- 설정 파일 존재 여부 확인 함수 수정 ---

#[tauri::command]
pub fn check_claude_config_exists(app: AppHandle) -> Result<bool, String> {
    let app_data_dir = match app.path().app_data_dir() {
        Ok(path) => path,
        Err(e) => {
            return Err(format!("Failed to get app data directory: {}", e));
        }
    };

    let claude_dir = app_data_dir
        .parent()
        .unwrap_or(&app_data_dir)
        .join("Claude");
    let claude_config_path = claude_dir.join("claude_desktop_config.json");

    println!(
        "[check_claude_config_exists] 검사 경로: {:?}",
        claude_config_path
    );

    Ok(claude_config_path.exists())
}

#[tauri::command]
pub fn check_mcplink_config_exists(app: AppHandle) -> Result<bool, String> {
    let app_data_dir = match app.path().app_data_dir() {
        Ok(path) => path,
        Err(e) => {
            return Err(format!("Failed to get app data directory: {}", e));
        }
    };

    let claude_dir = app_data_dir
        .parent()
        .unwrap_or(&app_data_dir)
        .join("Claude");
    let mcplink_config_path = claude_dir.join("mcplink_desktop_config.json");

    println!(
        "[check_mcplink_config_exists] 검사 경로: {:?}",
        mcplink_config_path
    );

    Ok(mcplink_config_path.exists())
}
