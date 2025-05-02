use serde::{Deserialize, Serialize};
use reqwest::Client;
use tauri::State;
use urlencoding;
use serde_json;
use serde_json::Value;

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
                // 일단 Value로 파싱 (유연한 처리를 위해)
                match response.json::<Value>().await {
                    Ok(json_value) => {
                        // API 응답이 배열인지 확인
                        if let Some(array) = json_value.as_array() {
                            // 각 항목을 MCPCard로 변환
                            let mut cards = Vec::new();
                            
                            for item in array {
                                if let Some(obj) = item.as_object() {
                                    // 필수 필드
                                    let id = obj.get("id").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                                    let title = obj.get("title").and_then(|v| v.as_str()).unwrap_or("Unknown").to_string();
                                    let description = obj.get("description").and_then(|v| v.as_str()).unwrap_or("").to_string();
                                    
                                    // 옵션 필드 (없으면 기본값 사용)
                                    let url = obj.get("url").and_then(|v| v.as_str()).unwrap_or("").to_string();
                                    let stars = obj.get("stars").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                                    
                                    cards.push(MCPCard {
                                        id,
                                        title,
                                        description,
                                        url,
                                        stars,
                                    });
                                }
                            }
                            
                            Ok(cards)
                        } else {
                            Err("API 응답이 배열 형식이 아닙니다".to_string())
                        }
                    },
                    Err(e) => Err(format!("JSON parsing error: {}", e))
                }
            } else {
                Err(format!("server error: {}", response.status()))
            }
        },
        Err(e) => Err(format!("request error: {}", e))
    }
}