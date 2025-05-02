use reqwest::Client;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fs;
use std::path::Path;
use std::process::Command as StdCommand;
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

/// Claude Desktop 애플리케이션을 재시작
#[tauri::command]
pub async fn restart_claude_desktop(_app: tauri::AppHandle) -> Result<(), String> {
    // Windows에서 Claude Desktop 프로세스를 종료하는 다양한 방법 시도
    println!("Claude Desktop 프로세스를 종료합니다...");

    // 가능한 Electron Claude 관련 프로세스 이름들
    let possible_process_names = [
        "claude.exe", // 소문자 버전
    ];

    let mut found_process = false;

    // 각 가능한 프로세스 이름에 대해 검색 및 종료 시도
    for process_name in possible_process_names.iter() {
        // 프로세스 확인
        let check_process = StdCommand::new("tasklist")
            .args([
                "/FI",
                &format!("IMAGENAME eq {}", process_name),
                "/FO",
                "CSV",
            ])
            .output()
            .map_err(|e| format!("tasklist 실행 실패: {}", e))?;

        let output = String::from_utf8_lossy(&check_process.stdout);

        if output.contains(process_name) {
            println!(
                "{} 프로세스가 실행 중입니다. 종료를 시도합니다.",
                process_name
            );
            found_process = true;

            // PID 기반 종료 시도
            if let Some(pid_list) = extract_pids_from_tasklist(&output) {
                for pid in pid_list {
                    println!("PID {}를 강제 종료합니다.", pid);
                    let _ = StdCommand::new("taskkill")
                        .args(["/F", "/PID", &pid])
                        .output();
                }
            }
        }
    }

    // 프로세스를 찾지 못했을 경우에도 정보 출력
    if !found_process {
        println!("실행 중인 Claude Desktop 프로세스를 찾을 수 없습니다.");
        // 가장 일반적인 Electron 프로세스들을 확인하고 'Claude'라는 문자열이 있는지 검사
        let check_all_processes = StdCommand::new("tasklist")
            .args(["/FO", "CSV"])
            .output()
            .map_err(|e| format!("tasklist 실행 실패: {}", e))?;

        let all_processes = String::from_utf8_lossy(&check_all_processes.stdout);

        // 'Claude'라는 이름이 포함된 모든 프로세스를 찾기
        for line in all_processes.lines() {
            if line.to_lowercase().contains("claude") {
                let parts: Vec<&str> = line.split(',').collect();
                if parts.len() >= 2 {
                    let process_name = parts[0].trim_matches('"');
                    println!("Claude 관련 프로세스 발견: {}", process_name);

                    // 해당 프로세스 종료 시도
                    let _ = StdCommand::new("taskkill")
                        .args(["/F", "/IM", process_name])
                        .output();
                }
            }
        }
    }

    // 작은 지연 추가 (프로세스가 완전히 종료될 시간을 줌)
    tokio::time::sleep(tokio::time::Duration::from_millis(1500)).await;

    // 가능한 Claude Desktop 설치 경로들
    let local_appdata = std::env::var("LOCALAPPDATA").unwrap_or_default();
    let appdata = std::env::var("APPDATA").unwrap_or_default();

    let possible_paths = [
        // 사용자가 지정한 위치
        format!("{}\\AnthropicClaude\\Claude.exe", local_appdata),
    ];

    // 가능한 모든 경로 출력 (디버깅 용도)
    for path in &possible_paths {
        println!(
            "확인 중인 경로: {}, 존재함: {}",
            path,
            Path::new(path).exists()
        );
    }

    // 존재하는 Claude.exe 파일 찾기
    let claude_path = possible_paths
        .iter()
        .find(|path| Path::new(path).exists())
        .ok_or_else(|| {
            "Claude Desktop 실행 파일을 찾을 수 없습니다. 설치 경로를 확인해주세요.".to_string()
        })?;

    // 백그라운드에서 Claude Desktop 시작
    match StdCommand::new(claude_path).spawn() {
        Ok(_) => {
            println!(
                "Claude Desktop을 성공적으로 재시작했습니다. 경로: {}",
                claude_path
            );
            Ok(())
        }
        Err(e) => Err(format!("Failed to start Claude Desktop: {}", e)),
    }
}

/// 주어진 tasklist 출력에서 PID 목록을 추출합니다.
fn extract_pids_from_tasklist(tasklist_output: &str) -> Option<Vec<String>> {
    let mut pids = Vec::new();

    // CSV 형식의 출력 파싱 (헤더 건너뛰기)
    for line in tasklist_output.lines().skip(1) {
        // CSV 형식에서 두 번째 값이 PID
        let parts: Vec<&str> = line.split(',').collect();
        if parts.len() >= 2 {
            // 따옴표 제거
            let pid = parts[1].trim_matches('"').to_string();
            pids.push(pid);
        }
    }

    if pids.is_empty() {
        None
    } else {
        Some(pids)
    }
}
