use serde::{Deserialize, Serialize};
use reqwest::Client;
use tauri::State;
use sqlx::PgPool;
use urlencoding;
use serde_json;

/// MCP 카드 데이터
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MCPCard {
    pub id: i32,
    pub title: String, 
    pub description: String,
    pub url: String,     // GitHub 리포지토리 URL
    pub stars: i32,      // GitHub 스타 수
}

/// 앱 상태 관리를 위함함
pub struct AppState {
    pub client: Client,
    pub db_pool: PgPool,  // DB 연결 풀 추가
}

/// MCP 카드 데이터를 가져와 (검색어 필터링 포함)
#[tauri::command]
pub async fn get_mcp_data(state: State<'_, AppState>, search_term: Option<String>) -> Result<Vec<MCPCard>, String> {
    let url = if let Some(term) = &search_term {
        let encoded_term = urlencoding::encode(term);
        format!("http://localhost:8080/api/mcp-cards?search={}", encoded_term)
    } else {
        "http://localhost:8080/api/mcp-cards".to_string()
    };
    
    // API 요청 보내기
    match state.client.get(url).send().await {
        Ok(response) => {
            if response.status().is_success() {
                // JSON 응답을 MCPCard로 직접 파싱
                match response.json::<Vec<MCPCard>>().await {
                    Ok(cards) => Ok(cards), // 서버에서 받은 데이터 사용
                    Err(e) => Err(format!("JSON parsing error: {}", e))
                }
            } else {
                Err(format!("server error: {}", response.status()))
            }
        },
        Err(e) => Err(format!("request error: {}", e))
    }
}