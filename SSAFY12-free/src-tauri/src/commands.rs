use serde::{Deserialize, Serialize};
use reqwest::Client;
use tauri::State;

/// MCP 카드 데이터
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPCard {
    pub id: i32,
    pub title: String, 
    pub description: String,
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
                    Err(e) => Err(format!("JSON parsing error: {}", e))
                }
            } else {
                Err(format!("server error: {}", response.status()))
            }
        },
        Err(e) => Err(format!("request error: {}", e))
    }
} 