// app/src-tauri/src/commands.rs

use dotenvy::dotenv;
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json::{Map, Value};
use std::collections::HashMap;
use std::{env, fs, path::PathBuf, process::Command as StdCommand};
use tauri::{AppHandle, Manager, State};
use tokio::time::{sleep, Duration};
use urlencoding::encode;

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
    mcpServers: Vec<ApiCardData>, // data -> mcpServers로 이름 변경
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
    pub env: Option<Map<String, Value>>,
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
    env: Option<Map<String, Value>>,
    // command: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPServerConfig {
    pub command: String,
    pub args: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<Map<String, Value>>,
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
pub fn some_command() -> String {
    let _ = dotenv().ok();
    let crawler_api_base_url: String =
        env::var("CRAWLER_API_BASE_URL").expect("CRAWLER_API_BASE_URL must be set");
    return crawler_api_base_url;
}

#[tauri::command]
pub async fn get_mcp_data(
    state: State<'_, AppState>,
    search_term: Option<String>,
) -> Result<Vec<MCPCard>, String> {
    dotenv().ok();

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
            let encoded_term = encode(&term);
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
                                    if let Ok(data_wrapper) = serde_json::from_value::<DataWrapper>(
                                        Value::Object(data_obj.clone()),
                                    ) {
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
                                    } else {
                                        println!("[get_mcp_data] Failed to parse data object into DataWrapper.");
                                    }
                                } else {
                                    println!("[get_mcp_data] API response.data is not an object. Type: {:?}. Returning empty.", api_response.data);
                                }
                                return Ok(Vec::new());
                            }
                            Err(e) => {
                                let msg = format!(
                                    "[get_mcp_data] JSON parsing error: {}. Body: {:.500}",
                                    e, text_body
                                );
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
        // .env에는 .../servers 까지만
        Ok(url_val) => url_val,
        Err(e) => {
            let msg = format!("[get_mcp_detail_data] CRAWLER_API_BASE_URL not set: {}", e);
            return Err(msg);
        }
    };
    let request_url = format!("{}/{}", base_url, id); // 예: .../servers/16
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
                        println!(
                            "[get_mcp_detail_data] Successfully parsed detail for ID {}: {:?}",
                            id, card_detail
                        );
                        Ok(card_detail)
                    }
                    Err(e) => {
                        let msg = format!("[get_mcp_detail_data] JSON parsing error: {}", e);
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

    // mcplink 파일 경로 생성 (claude_desktop_config.json과 같은 위치)
    let mcplink_path = config_path
        .parent()
        .unwrap()
        .join("mcplink_desktop_config.json");

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
        return Err("No MCP servers are installed".to_string());
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
