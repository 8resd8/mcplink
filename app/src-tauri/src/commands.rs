use reqwest::Client;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fs;
use tauri::{Manager, State};

/// MCP 카드 데이터
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPCard {
    pub id: i32,
    pub title: String,
    pub description: String,
}

/// MCP 서버 설정
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPServerConfig {
    pub command: String,
    pub args: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<serde_json::Map<String, serde_json::Value>>,
}

/// 클로드 데스크톱 설정
#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct ClaudeDesktopConfig {
    pub mcpServers: Option<HashMap<String, MCPServerConfig>>,
    #[serde(flatten)]
    pub other: serde_json::Map<String, serde_json::Value>,
}

/// 앱 상태 관리를 위함함
pub struct AppState {
    pub client: Client,
}

/// MCP 카드 데이터를 가져와
#[tauri::command]
pub async fn get_mcp_data(state: State<'_, AppState>) -> Result<Vec<MCPCard>, String> {
    let url = "http://localhost:8080/api/mcp-cards";

    // API 요청 보내기
    match state.client.get(url).send().await {
        Ok(response) => {
            if response.status().is_success() {
                // JSON 응답 : MCPCard로 파싱
                match response.json::<Vec<MCPCard>>().await {
                    Ok(cards) => Ok(cards),
                    Err(e) => Err(format!("JSON parsing error: {}", e)),
                }
            } else {
                Err(format!("server error: {}", response.status()))
            }
        }
        Err(e) => Err(format!("request error: {}", e)),
    }
}

/// MCP 서버 설정을 Claude Desktop 설정 파일에 추가
#[tauri::command]
pub async fn add_mcp_server_config(
    app: tauri::AppHandle,
    server_name: String,
    server_config: MCPServerConfig,
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

    // 설정 파일 읽기 (파일이 없으면 빈 객체 생성)
    let mut config = if config_path.exists() {
        let config_str = fs::read_to_string(&config_path)
            .map_err(|e| format!("Failed to read config file: {}", e))?;

        match serde_json::from_str::<ClaudeDesktopConfig>(&config_str) {
            Ok(config) => config,
            Err(_) => {
                // 파일이 있지만 형식이 잘못된 경우, 다른 필드는 유지하면서 새로 생성
                match serde_json::from_str::<serde_json::Value>(&config_str) {
                    Ok(value) => {
                        if let serde_json::Value::Object(map) = value {
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
    servers.insert(server_name, server_config);
    config.mcpServers = Some(servers);

    // 설정 파일에 쓰기
    let config_json = serde_json::to_string_pretty(&config)
        .map_err(|e| format!("Failed to serialize config: {}", e))?;

    fs::write(&config_path, config_json)
        .map_err(|e| format!("Failed to write config file: {}", e))?;

    Ok(())
}
