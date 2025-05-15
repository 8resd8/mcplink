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

// --- Existing struct definitions (McpServerInfo, ApiCardData, PageInfo, DataWrapper, ApiResponse) ---
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

// This ApiResponse struct can be used for both get_mcp_data and get_mcp_detail_data.
#[derive(Debug, Deserialize)]
#[allow(non_snake_case)]
struct ApiResponse {
    // data field is flexible with Value type
    timestamp: String,
    message: String,
    code: String,
    data: Value, // Actual data comes in Value form here
}

// --- MCPCard, MCPCardDetail, MCPServerConfig, ClaudeDesktopConfig, AppState struct definitions ---
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPCard {
    pub id: i32,
    pub title: String,
    pub description: String,
    pub url: String,
    pub stars: i32,
}

// Response struct including page information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PageInfoResponse {
    pub has_next_page: bool,
    pub end_cursor: Option<i32>,
    pub total_items: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPCardResponse {
    pub cards: Vec<MCPCard>,
    pub page_info: PageInfoResponse,
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
    pub mcp_servers: Option<HashMap<String, MCPServerConfig>>,
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
            return Err(msg);
        }
    };

    // Add cursor ID to URL configuration
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
                        match serde_json::from_str::<ApiResponse>(&text_body) {
                            Ok(api_response) => {
                                if let Value::Object(data_obj) = &api_response.data {
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
                                            if let Some(card) = cards.first() {
                                            }

                                            // Extract page information
                                            let end_cursor = match data_wrapper.pageInfo.endCursor {
                                                Some(Value::Number(n)) => {
                                                    n.as_i64().map(|x| x as i32)
                                                }
                                                _ => None,
                                            };

                                            let response = MCPCardResponse {
                                                cards,
                                                page_info: PageInfoResponse {
                                                    has_next_page: data_wrapper.pageInfo.hasNextPage,
                                                    end_cursor: end_cursor,
                                                    total_items: data_wrapper.pageInfo.totalItems,
                                                },
                                            };

                                            return Ok(response);
                                        }
                                        Err(e) => {
                                            return Err(format!("[get_mcp_data] Failed to parse data into DataWrapper: {}", e));
                                        }
                                    }
                                } else {
                                    return Ok(MCPCardResponse {
                                        cards: Vec::new(),
                                        page_info: PageInfoResponse {
                                            has_next_page: false,
                                            end_cursor: None,
                                            total_items: 0,
                                        },
                                    });
                                }
                            }
                            Err(e) => {
                                let msg = format!("[get_mcp_data] JSON parsing error for ApiResponse: {}. Body: {:.500}", e, text_body);
                                return Err(msg);
                            }
                        }
                    }
                    Err(e) => {
                        let msg = format!("[get_mcp_data] Failed to read response text: {}", e);
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
                return Err(msg);
            }
        }
        Err(e) => {
            let msg = format!("[get_mcp_data] Request error for {}: {}", request_url, e);
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

    match state.client.get(&request_url).send().await {
        Ok(response) => {
            let status = response.status();
            if status.is_success() {
                match response.json::<ApiResponse>().await {
                    // Outer ApiResponse wrapper
                    Ok(api_response_wrapper) => {
                        if let Value::Object(data_map) = api_response_wrapper.data {
                            if let Some(inner_mcp_server_value) = data_map.get("mcpServer") {
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
                                        Ok(card_detail)
                                    }
                                    Err(e_inner_struct) => {
                                        let msg = format!("[get_mcp_detail_data] Failed to parse inner_mcp_server_value into DetailApiResponse: {}. Value was: {:?}", e_inner_struct, inner_mcp_server_value);
                                        Err(msg)
                                    }
                                }
                            } else {
                                let msg = format!("[get_mcp_detail_data] Key 'mcpServer' not found inside ApiResponse.data. ApiResponse.data was: {:?}", data_map);
                                Err(msg)
                            }
                        } else {
                            let msg = format!("[get_mcp_detail_data] ApiResponse.data is not an object. It was: {:?}", api_response_wrapper.data);
                            Err(msg)
                        }
                    }
                    Err(e_outer) => {
                        let msg = format!("[get_mcp_detail_data] Failed to parse outer ApiResponse: {}. Check if the overall response matches ApiResponse structure.", e_outer);
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
                Err(msg)
            }
        }
        Err(e) => {
            let msg = format!("[get_mcp_detail_data] Request error {}: {}", request_url, e);
            Err(msg)
        }
    }
}

/// Add MCP server configuration to Claude Desktop config file
#[tauri::command]
pub async fn add_mcp_server_config(
    app: AppHandle,
    server_name: String,
    server_config: MCPServerConfig,
    server_id: i64,
) -> Result<(), String> {
    // Generate config file path
    let config_path = match env::consts::OS {
        "windows" => {
            // For Windows, %APPDATA%\Claude\claude_desktop_config.json
            let appdata = app
                .path()
                .app_data_dir()
                .map_err(|e| format!("Failed to get AppData directory: {}", e))?;
            let claude_dir = appdata.parent().unwrap().join("Claude");

            // Create Claude directory if it doesn't exist
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

    // Create mcplink file path (same location as claude_desktop_config.json)
    let mcplink_path = config_path
        .parent()
        .unwrap()
        .join("mcplink_desktop_config.json");

    // Read config file (create empty object if file doesn't exist)
    let mut config = if config_path.exists() {
        let config_str = fs::read_to_string(&config_path)
            .map_err(|e| format!("Failed to read config file: {}", e))?;

        match serde_json::from_str::<ClaudeDesktopConfig>(&config_str) {
            Ok(config) => config,
            Err(_) => {
                // If file exists but format is incorrect, create new while preserving other fields
                match serde_json::from_str::<Value>(&config_str) {
                    Ok(value) => {
                        if let Value::Object(map) = value {
                            ClaudeDesktopConfig {
                                mcp_servers: None,
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

    // Get or create MCP server map
    let mut servers = config.mcp_servers.unwrap_or_default();

    // Add or update server configuration
    servers.insert(server_name.clone(), server_config);
    config.mcp_servers = Some(servers);

    // Write to config file
    let config_json = serde_json::to_string_pretty(&config)
        .map_err(|e| format!("Failed to serialize config: {}", e))?;

    fs::write(&config_path, config_json)
        .map_err(|e| format!("Failed to write config file: {}", e))?;

    // Add server_id and server_name to mcplink_desktop_config.json
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

    // Convert server_id to string key and save server_name as value
    mcplink_config.insert(server_id.to_string(), Value::String(server_name));

    // Write to mcplink config file
    let mcplink_json = serde_json::to_string_pretty(&mcplink_config)
        .map_err(|e| format!("Failed to serialize mcplink config: {}", e))?;

    fs::write(&mcplink_path, mcplink_json)
        .map_err(|e| format!("Failed to write mcplink config file: {}", e))?;

    Ok(())
}

/// Remove MCP server configuration from Claude Desktop config file
#[tauri::command]
pub async fn remove_mcp_server_config(app: AppHandle, server_name: String) -> Result<(), String> {
    // Generate config file path
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

    // Create mcplink file path (same location as claude_desktop_config.json)
    let mcplink_path = config_path
        .parent()
        .unwrap()
        .join("mcplink_desktop_config.json");

    // Check if config file exists
    if !config_path.exists() {
        return Err("Configuration file does not exist".to_string());
    }

    let config_str = fs::read_to_string(&config_path)
        .map_err(|e| format!("Failed to read config file: {}", e))?;

    let mut config = match serde_json::from_str::<ClaudeDesktopConfig>(&config_str) {
        Ok(config) => config,
        Err(e) => return Err(format!("Failed to parse config file: {}", e)),
    };

    if config.mcp_servers.is_none() {
        return Err("No MCP servers are installed".to_string());
    }

    let mut servers = config.mcp_servers.unwrap_or_default();

    if !servers.contains_key(&server_name) {
        return Err(format!("MCP server '{}' not found", server_name));
    }

    servers.remove(&server_name);
    config.mcp_servers = Some(servers);

    let config_json = serde_json::to_string_pretty(&config)
        .map_err(|e| format!("Failed to serialize config: {}", e))?;

    fs::write(&config_path, config_json)
        .map_err(|e| format!("Failed to write config file: {}", e))?;

    // Remove entries related to server_name from mcplink_desktop_config.json
    if mcplink_path.exists() {
        let mcplink_str = fs::read_to_string(&mcplink_path)
            .map_err(|e| format!("Failed to read mcplink config file: {}", e))?;

        match serde_json::from_str::<Map<String, Value>>(&mcplink_str) {
            Ok(mut mcplink_config) => {
                // Find and delete all entries matching server_name
                let mut keys_to_remove = Vec::new();
                for (key, value) in &mcplink_config {
                    if let Value::String(name) = value {
                        if name == &server_name {
                            keys_to_remove.push(key.clone());
                        }
                    }
                }

                // Delete found keys
                for key in keys_to_remove {
                    mcplink_config.remove(&key);
                }

                // Write updated config to file
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

/// Restart Claude Desktop application
#[tauri::command]
pub async fn restart_claude_desktop(_app: AppHandle) -> Result<(), String> {

    // 1) Terminate all claude.exe processes (use precise filter)
    let kill_status = StdCommand::new("taskkill")
        .args([
            "/F", // Force termination
            "/T", // Terminate child processes
            "/FI",
            "IMAGENAME eq claude.exe",
        ])
        .status()
        .map_err(|e| format!("Failed to execute taskkill: {}", e))?;
    match kill_status.code() {
        Some(0) => {} // Success
        Some(128) => {} // No process found (considered success for this step)
        Some(c) => {} // Other exit codes, log if needed
        None => {} // Process terminated by signal, log if needed
    }

    // 2) Wait sufficiently (2 seconds)
    sleep(Duration::from_millis(2000)).await;

    // 3) Confirm termination
    let check = StdCommand::new("tasklist")
        .args(["/FI", "IMAGENAME eq claude.exe", "/NH"]) // /NH for no header
        .output()
        .map_err(|e| format!("Failed to execute tasklist: {}", e))?;
    let running = String::from_utf8_lossy(&check.stdout);

    // 4) Pre-create cache directory (prevent permission issues)
    let cache_dir: PathBuf = {
        let base =
            env::var("LOCALAPPDATA").map_err(|e| format!("Failed to get LOCALAPPDATA: {}", e))?;
        let dir = PathBuf::from(&base).join("AnthropicClaude").join("Cache");
        fs::create_dir_all(&dir)
            .map_err(|e| format!("Failed to create cache dir {:?}: {}", dir, e))?;
        dir
    };
    let cache_dir_str = cache_dir.to_string_lossy();

    // 5) Prepare executable path
    let claude_exe: PathBuf = {
        let base =
            env::var("LOCALAPPDATA").map_err(|e| format!("Failed to get LOCALAPPDATA: {}", e))?;
        PathBuf::from(base)
            .join("AnthropicClaude")
            .join("Claude.exe")
    };
    if !claude_exe.exists() {
        return Err(format!("Claude.exe not found at {}", claude_exe.display()));
    }

    // 6) Pass only Electron runtime flags → prevent URL parsing errors
    let child = StdCommand::new(&claude_exe)
        .args([
            "--user-data-dir",
            &cache_dir_str,
            "--disable-gpu-shader-disk-cache",
            "--disable-gpu",
        ])
        .spawn()
        .map_err(|e| format!("Failed to start Claude Desktop: {}", e))?;

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

            if status.is_success() {
                match response.text().await {
                    // Read as text first for logging
                    Ok(text_body) => {
                        match serde_json::from_str::<ApiResponse>(&text_body) {
                            // Parse outer ApiResponse
                            Ok(api_response) => {
                                if let Value::Object(data_obj) = &api_response.data {
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
                                            let end_cursor = match data_wrapper.pageInfo.endCursor {
                                                Some(Value::Number(n)) => {
                                                    n.as_i64().map(|x| x as i32)
                                                }
                                                _ => None,
                                            };

                                            let response = MCPCardResponse {
                                                cards,
                                                page_info: PageInfoResponse {
                                                    has_next_page: data_wrapper.pageInfo.hasNextPage,
                                                    end_cursor: end_cursor,
                                                    total_items: data_wrapper.pageInfo.totalItems,
                                                },
                                            };

                                            return Ok(response);
                                        }
                                        Err(e) => {
                                            return Err(format!("[get_installed_mcp_data] Failed to parse data into DataWrapper: {}", e));
                                        }
                                    }
                                } else {
                                    return Ok(MCPCardResponse {
                                        cards: Vec::new(),
                                        page_info: PageInfoResponse {
                                            has_next_page: false,
                                            end_cursor: None,
                                            total_items: 0,
                                        },
                                    });
                                }
                            }
                            Err(e) => {
                                let msg = format!("[get_installed_mcp_data] JSON parsing error for ApiResponse: {}. Body: {:.500}", e, text_body);
                                return Err(msg);
                            }
                        }
                    }
                    Err(e) => {
                        let msg = format!(
                            "[get_installed_mcp_data] Failed to read response text: {}",
                            e
                        );
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
                return Err(msg);
            }
        }
        Err(e) => {
            let msg = format!(
                "[get_installed_mcp_data] Request error for {}: {}",
                request_url, e
            );
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
        return Ok("{}".to_string());
    }

    fs::read_to_string(config_path)
        .map_err(|e| format!("Failed to read mcplink_desktop_config.json: {}", e))
}

#[tauri::command]
pub async fn show_popup(app: AppHandle, tag: String) -> Result<(), String> {
    // --- CLAUDE CODE START MODIFICATION ---
    // Check notification permission state
    let permission_state = match app.notification().permission_state() {
        Ok(state) => state,
        Err(e) => {
            eprintln!("[Notification] Failed to get permission state: {}", e);
            return Err(format!("Failed to get notification permission: {}", e));
        }
    };

    // Request permission if not granted
    use tauri_plugin_notification::PermissionState;
    if permission_state != PermissionState::Granted {
        match app.notification().request_permission() {
            Ok(new_state) => {
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
    
    // Create notification body - including tag information
    let notification_body = format!("Selected keyword: {}. Click to confirm.", tag);

    // --- CLAUDE CODE START MODIFICATION ---
    // Set notification options - simplified way to create notification
    let builder = app
        .notification()
        .builder()
        .title("Confirm Recommendation")
        .body(&notification_body)
        .icon("icons/icon.png");
    
    // Send notification and handle result
    match builder.show() {
    // --- CLAUDE CODE END MODIFICATION ---
        Ok(_) => {
            if let Ok(permission_state) = app.notification().permission_state() {
            }
            Ok(())
        }
        Err(e) => {
            eprintln!("[Notification Send Error] Tag: {}, Error: {}", tag, e);
            Err(format!("Failed to send notification: {}", e))
        }
    }
    // --- CLAUDE CODE END MODIFICATION ---
}

// --- Modified function to check config file existence ---

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

    Ok(mcplink_config_path.exists())
}
