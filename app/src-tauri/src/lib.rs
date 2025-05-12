// app/src-tauri/src/lib.rs

use axum::{extract::State as AxumState, http::StatusCode, routing::post, Json, Router};
use reqwest::Client;
use serde::Deserialize;
use std::{env, net::SocketAddr, sync::Arc};
use tauri::Emitter;
use tauri::{
    menu::{MenuBuilder, MenuItemBuilder},
    tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent},
    AppHandle, Manager,
};
use tauri_plugin_notification::{NotificationExt, PermissionState};
use tokio::sync::Mutex;

pub mod commands;
use crate::commands::AppState;

// 키워드 페이로드를 위한 구조체 정의
#[derive(Deserialize, Debug)]
pub struct KeywordsPayload {
    keywords: Vec<String>,
}

// Axum 서버 상태를 위한 구조체
#[derive(Clone)]
pub struct RecommendationServerState {
    app_handle: Arc<Mutex<Option<AppHandle>>>,
}

impl RecommendationServerState {
    pub fn new() -> Self {
        Self {
            app_handle: Arc::new(Mutex::new(None)),
        }
    }

    pub async fn set_app_handle(&self, app_handle: AppHandle) {
        let mut handle = self.app_handle.lock().await;
        *handle = Some(app_handle);
    }
}

// 키워드 추천 요청을 처리하는 핸들러
async fn handle_recommendations(
    AxumState(state): AxumState<RecommendationServerState>,
    Json(payload): Json<KeywordsPayload>,
) -> StatusCode {
    println!("Received keywords: {:?}", payload.keywords);

    // 키워드를 쉼표로 구분된 문자열로 변환
    let keywords_str = payload.keywords.join(", ");

    // AppHandle을 사용하여 알림 보내기
    if let Some(app_handle) = &*state.app_handle.lock().await {
        // 기존 알림 로직 활용
        let notification_body = format!("선택된 키워드: {}. 클릭하여 확인하세요.", keywords_str);

        let builder = app_handle
            .notification()
            .builder()
            .title("새로운 추천 키워드")
            .body(&notification_body)
            .icon("icons/icon.png");

        match builder.show() {
            Ok(_) => println!("Notification sent successfully."),
            Err(e) => eprintln!("Failed to send notification: {}", e),
        }

        // emit_all 대신 emit 사용 - Emitter trait 명시적 사용
        {
            use tauri::Emitter;
            let _ = app_handle.emit("new-keywords", payload.keywords.clone());
        }
    } else {
        eprintln!("AppHandle not set, cannot send notification");
    }

    StatusCode::OK
}

// Axum 서버 시작 함수
pub async fn start_axum_server(app_state: RecommendationServerState) {
    // 환경 변수에서 GUI API URL 설정 가져오기
    let gui_api_host = env::var("GUI_API_HOST").unwrap_or_else(|_| "0.0.0.0".to_string());
    let gui_api_port = env::var("GUI_API_PORT").unwrap_or_else(|_| "8082".to_string());

    let gui_be_api_base_url = env::var("GUI_BE_API_BASE_URL")
    .unwrap_or_else(|_| "http://localhost:8082/api/v1".to_string());
    
    println!("GUI_BE_API_BASE_URL: {}", gui_be_api_base_url);

    let addr_str = format!("{}:{}", gui_api_host, gui_api_port);
    let addr: SocketAddr = addr_str.parse().expect("Failed to parse address");

    // Axum 라우터 설정
    let app = Router::new()
        .route("/recommendations", post(handle_recommendations))
        .with_state(app_state);

    println!("GUI Backend API server listening on {}", addr);

    // Axum 서버 시작
    match axum::serve(tokio::net::TcpListener::bind(addr).await.unwrap(), app).await {
        Ok(_) => println!("Axum server shut down gracefully"),
        Err(e) => eprintln!("Axum server error: {}", e),
    }
}


pub fn run() {
    // AppState 생성 (API 요청용 client 유지)
    let app_state = AppState {
        client: Client::new(),
    };

    // Axum 서버를 위한 AppState 생성
    let recommendation_server_state = RecommendationServerState::new();
    let recommendation_server_state_clone = recommendation_server_state.clone();

    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_os::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_process::init())
        .plugin(tauri_plugin_notification::init()) // 알림 플러그인 초기화
        .setup(|app| {
            // 메뉴 아이템 생성
            let open_item = MenuItemBuilder::with_id("open", "Open").build(app)?;
            let quit_item = MenuItemBuilder::with_id("quit", "Quit").build(app)?;
            let hide_item = MenuItemBuilder::with_id("hide", "Hide").build(app)?;
            let show_item = MenuItemBuilder::with_id("show", "Show").build(app)?;

            // 메뉴 생성
            let menu = MenuBuilder::new(app)
                .item(&open_item)
                .separator()
                .item(&hide_item)
                .item(&show_item)
                .separator()
                .item(&quit_item)
                .build()?;

            // 트레이 아이콘 생성
            let _tray = TrayIconBuilder::new()
                .tooltip("Keyword Search")
                .icon(app.default_window_icon().cloned().unwrap())
                .menu(&menu)
                .on_menu_event(move |app_handle, event| match event.id().as_ref() {
                    "quit" => {
                        app_handle.exit(0);
                    }
                    "open" | "show" => {
                        if let Some(window) = app_handle.get_webview_window("main") {
                            let _ = window.show();
                            let _ = window.unminimize();
                            let _ = window.set_focus();
                        }
                    }
                    "hide" => {
                        if let Some(window) = app_handle.get_webview_window("main") {
                            let _ = window.hide();
                        }
                    }
                    _ => {}
                })
                .on_tray_icon_event(|tray_handle, event| {
                    if let TrayIconEvent::Click {
                        button: MouseButton::Left,
                        button_state: MouseButtonState::Up,
                        ..
                    } = event
                    {
                        let app_handle = tray_handle.app_handle();
                        if let Some(window) = app_handle.get_webview_window("main") {
                            let _ = window.show();
                            let _ = window.unminimize();
                            let _ = window.set_focus();
                        }
                    }
                })
                .build(app)?;

            // --- 알림 클릭 핸들러 수정 시작 ---
            let app_handle = app.handle().clone(); // app_handle을 여기서 클론하여 아래 클로저에서 사용

            // 알림 권한 상태 확인 및 로깅
            if let Ok(permission_state) = app_handle.notification().permission_state() {
                println!(
                    "[SETUP] Notification permission state: {:?}",
                    permission_state
                );
                if permission_state != PermissionState::Granted {
                    println!("[SETUP] Notification permission not granted, requesting...");
                    if let Ok(new_state) = app_handle.notification().request_permission() {
                        println!("[SETUP] New notification permission state: {:?}", new_state);
                    }
                }
            }

            // 태그 추출 공통 함수
            fn extract_tag_from_body(body: &str) -> Option<String> {
                // "선택된 키워드: TAG. 클릭하여 확인하세요."에서 TAG 부분 추출
                if let Some(start) = body.find("선택된 키워드: ") {
                    let start_idx = start + "선택된 키워드: ".len();
                    if let Some(end) = body[start_idx..].find(". ") {
                        let tag = body[start_idx..(start_idx + end)].to_string();
                        println!("Tag extracted from body: {}", tag);
                        return Some(tag);
                    }
                }
                None
            }

            // 알림 처리 함수
            fn handle_notification(body: &str, app_handle: &tauri::AppHandle) {
                println!("Notification received with body: {}", body);

                // 메인 윈도우 가져오기
                if let Some(window) = app_handle.get_webview_window("main") {
                    // 1. 창 활성화 (동일한 순서로 처리)
                    let _ = window.show();
                    let _ = window.unminimize();
                    let _ = window.set_focus();

                    // 2. 창을 맨 앞으로 가져오기
                    let _ = window.set_always_on_top(true);
                    let _ = window.set_focus();
                    let _ = window.set_always_on_top(false);

                    // 3. 태그 추출 로직
                    let final_tag = extract_tag_from_body(body).unwrap_or_else(|| {
                        println!("No tag found in notification body, using default: GOOGLE");
                        "GOOGLE".to_string()
                    });

                    // 4. 이벤트 발생
                    // MCP-list 페이지로 이동
                    let target_url = format!("/MCP-list?keyword={}", final_tag);
                    println!("Emitting navigate-to event with URL: {}", target_url);

                    // 이벤트 발생 - navigate-to로 페이지 이동 (emit_all 대신 emit 사용)
                    // Emitter 트레이트를 사용하기 위해 명시적으로 use 선언 추가
                    {
                        use tauri::Emitter;
                        let _ = window.emit("navigate-to", target_url.clone());
                        let _ = window.emit("navigate-to-mcp-list-with-keyword", target_url);
                    }
                }
            }

            // Tauri v2에서는 알림 클릭 이벤트 처리 방식이 변경됨
            // 앱이 실행 중일 때 알림을 클릭하면 이벤트가 발생하지만,
            // 실제로는 클라이언트 측 시뮬레이션으로 알림 클릭 동작을 대체함

            println!("알림 클릭 처리는 프론트엔드에서 시뮬레이션됩니다.");
            println!("알림을 수신한 후 test/+page.svelte에서 시뮬레이션 클릭 핸들러가 실행됩니다.");
            // --- 알림 클릭 핸들러 수정 끝 ---

            // --- Axum 서버 시작 코드 추가 시작 ---
            let app_handle_for_axum = app.handle().clone();

            // AppHandle 설정 및 Axum 서버 시작
            tauri::async_runtime::spawn(async move {
                // AppHandle 설정
                recommendation_server_state_clone
                    .set_app_handle(app_handle_for_axum)
                    .await;

                // Axum 서버 시작
                println!("Starting Axum server for recommendations API...");
                start_axum_server(recommendation_server_state_clone).await;
            });
            // --- Axum 서버 시작 코드 추가 끝 ---

            Ok(())
        })
        .manage(app_state)
        .invoke_handler(tauri::generate_handler![
            commands::show_popup,
            commands::get_mcp_data,
            commands::get_mcp_detail_data,
            commands::add_mcp_server_config,
            commands::remove_mcp_server_config,
            commands::restart_claude_desktop,
            commands::get_installed_mcp_data,
            commands::read_mcplink_config_content,
            commands::check_claude_config_exists,
            commands::check_mcplink_config_exists,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
