// app/src-tauri/src/lib.rs

use axum::{
    extract::State as AxumState,
    http::{Method, Request, StatusCode}, // Added Method and Request
    middleware::{self, Next},            // Added middleware and Next
    response::Response,                  // Added Response
    routing::post,
    Json,
    Router,
};
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

// POST request logging middleware function
async fn log_post_requests(
    req: Request<axum::body::Body>,
    next: Next,
) -> Result<Response, StatusCode> {
    if req.method() == Method::POST {
        let _uri = req.uri().clone(); // _uri to avoid warning, or log it
        let _headers = req.headers().clone(); // _headers to avoid warning, or log them
                                              // Note: Logging the request body requires caution.
                                              // Here, only URI and headers are (potentially) logged.
    }
    // Pass the request to the next handler or middleware
    Ok(next.run(req).await)
}

// Struct for keyword payload
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

// Handler for keyword recommendation requests
async fn handle_recommendations(
    AxumState(state): AxumState<RecommendationServerState>,
    Json(payload): Json<KeywordsPayload>,
) -> StatusCode {
    // Convert keywords to a comma-separated string
    let keywords_str = payload.keywords.join(", ");

    // Send notification using AppHandle
    if let Some(app_handle) = &*state.app_handle.lock().await {
        // Utilize existing notification logic
        let notification_body = format!("Selected keywords: {}. Click to check.", keywords_str);

        let builder = app_handle
            .notification()
            .builder()
            .title("New Recommended Keywords") // Title in English
            .body(&notification_body)
            .icon("icons/icon.png");

        match builder.show() {
            Ok(_) => {}   // println!("Notification sent successfully."), // Log removed
            Err(_e) => {} // eprintln!("Failed to send notification: {}", e), // Log removed
        }

        // Use emit instead of emit_all - Explicitly use Emitter trait
        {
            use tauri::Emitter;
            let _ = app_handle.emit("new-keywords", payload.keywords.clone());
        }
    } else {
        // eprintln!("AppHandle not set, cannot send notification"); // Log removed
    }

    StatusCode::OK
}

// Handler for /api/v1 requests
async fn handle_api_v1_request(
    AxumState(state): AxumState<RecommendationServerState>,
) -> StatusCode {
    // Send notification using AppHandle
    if let Some(app_handle) = &*state.app_handle.lock().await {
        // Set notification body
        let notification_body = String::from("New data received. (From /api/v1)");

        // Create and display notification
        let builder = app_handle
            .notification()
            .builder()
            .title("API Notification") // Title in English
            .body(&notification_body)
            .icon("icons/icon.png");

        // Attempt to display notification and log result
        match builder.show() {
            Ok(_) => {}   // println!("Notification sent successfully for /api/v1."), // Log removed
            Err(_e) => {} // eprintln!("Failed to send notification for /api/v1: {}", e), // Log removed
        }
    } else {
        // eprintln!("AppHandle not set, cannot send notification for /api/v1"); // Log removed
    }

    StatusCode::OK
}

// Function to start Axum server
pub async fn start_axum_server(
    app_state: RecommendationServerState,
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    // Get GUI API URL settings from environment variables
    let gui_api_host = env::var("GUI_API_HOST").unwrap_or_else(|_| "0.0.0.0".to_string());
    let gui_api_port = env::var("GUI_API_PORT").unwrap_or_else(|_| "8082".to_string());

    let addr_str = format!("{}:{}", gui_api_host, gui_api_port);
    let addr: SocketAddr = match addr_str.parse() {
        Ok(addr) => addr,
        Err(e) => {
            // eprintln!("[DEBUG] Failed to parse address: {}", e); // Log removed
            return Err(Box::new(e));
        }
    };

    // Configure Axum router
    let app = Router::new()
        .route("/recommendations", post(handle_recommendations))
        .route("/api/v1", post(handle_api_v1_request)) // Add handler for /api/v1 path
        .route("/api/v1/recommendations", post(handle_recommendations)) // Additional path for requests from mcp-server (based on app .env)
        // Removed duplicate path: "/recommendations" path was already added above
        .layer(middleware::from_fn(log_post_requests)) // Apply POST logging middleware
        .with_state(app_state);

    // Attempt to bind TcpListener
    let listener = match tokio::net::TcpListener::bind(addr).await {
        Ok(listener) => listener,
        Err(e) => {
            // eprintln!("[DEBUG] Failed to bind to {}: {}", addr, e); // Log removed
            return Err(Box::new(e));
        }
    };

    // Start Axum server
    match axum::serve(listener, app).await {
        Ok(_) => Ok(()),
        Err(e) => {
            // eprintln!("[DEBUG] Axum server error: {}", e); // Log removed
            Err(Box::new(e))
        }
    }
}

pub fn run() {
    // Create AppState (maintains client for API requests)
    let app_state = AppState {
        client: Client::new(),
    };

    // Create AppState for Axum server
    let recommendation_server_state = RecommendationServerState::new();
    let recommendation_server_state_clone = recommendation_server_state.clone();

    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_os::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_process::init())
        .plugin(tauri_plugin_notification::init()) // Initialize notification plugin
        .plugin(tauri_plugin_single_instance::init(|app, argv, cwd| {
            // 새로운 인스턴스가 실행됐을 때, 기존 창에 포커스
            app.get_webview_window("main")
                .expect("메인 창을 찾을 수 없습니다")
                .set_focus()
                .unwrap();

            // 필요시 창 복원 및 전면으로 가져오기
            #[cfg(target_os = "windows")]
            {
                use tauri::Manager;
                if let Some(window) = app.get_webview_window("main") {
                    let _ = window.unminimize();
                    let _ = window.show();
                    let _ = window.set_focus();
                }
            }
        }))
        .setup(|app| {
            // Create menu items
            let open_item = MenuItemBuilder::with_id("open", "Open").build(app)?;
            let quit_item = MenuItemBuilder::with_id("quit", "Quit").build(app)?;
            let hide_item = MenuItemBuilder::with_id("hide", "Hide").build(app)?;
            let show_item = MenuItemBuilder::with_id("show", "Show").build(app)?;

            // Create menu
            let menu = MenuBuilder::new(app)
                .item(&open_item)
                .separator()
                .item(&hide_item)
                .item(&show_item)
                .separator()
                .item(&quit_item)
                .build()?;

            // Create tray icon
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

            // --- Start of notification click handler modification ---
            let app_handle = app.handle().clone(); // Clone app_handle here to use in the closure below

            // Check and log notification permission state on app start
            if let Ok(permission_state) = app_handle.notification().permission_state() {
                if permission_state != PermissionState::Granted {
                    // If permission not granted, request it
                    if let Ok(_new_state) = app_handle.notification().request_permission() {
                        // Permission request sent, new state can be handled if needed
                    }
                }
            }

            // Common function to extract tag
            fn extract_tag_from_body(body: &str) -> Option<String> {
                // Extracts TAG from "선택된 키워드: TAG. 클릭하여 확인하세요." (Selected keywords: TAG. Click to check.)
                if let Some(start) = body.find("선택된 키워드: ") {
                    // Find the Korean part for tag extraction
                    let start_idx = start + "선택된 키워드: ".len();
                    if let Some(end) = body[start_idx..].find(". ") {
                        let tag = body[start_idx..(start_idx + end)].to_string();
                        return Some(tag);
                    }
                }
                None
            }

            // Notification handler function
            fn handle_notification(body: &str, app_handle: &tauri::AppHandle) {
                // Get main window
                if let Some(window) = app_handle.get_webview_window("main") {
                    // 1. Activate window (process in the same order)
                    let _ = window.show();
                    let _ = window.unminimize();
                    let _ = window.set_focus();

                    // 2. Bring window to front
                    let _ = window.set_always_on_top(true);
                    let _ = window.set_focus(); // Redundant if already focused, but ensures focus
                    let _ = window.set_always_on_top(false);

                    // 3. Tag extraction logic
                    let final_tag = extract_tag_from_body(body).unwrap_or_else(|| {
                        // Default tag if extraction fails
                        "GOOGLE".to_string()
                    });

                    // 4. Emit events
                    // Navigate to MCP-list page
                    let target_url = format!("/MCP-list?keyword={}", final_tag);
                    // Emit event - navigate to page using navigate-to (use emit instead of emit_all)
                    // Explicitly use Emitter trait
                    {
                        use tauri::Emitter;
                        let _ = window.emit("navigate-to", target_url.clone());
                        let _ = window.emit("navigate-to-mcp-list-with-keyword", target_url);
                    }
                }
            }
            // --- End of notification click handler modification ---

            // --- Start of Axum server startup code addition ---
            let app_handle_for_axum = app.handle().clone();

            // Set AppHandle and start Axum server
            tauri::async_runtime::spawn(async move {
                // Set AppHandle
                recommendation_server_state_clone
                    .set_app_handle(app_handle_for_axum)
                    .await;

                // Start Axum server
                match start_axum_server(recommendation_server_state_clone).await {
                    Ok(_) => {}   // println!("[DEBUG] Axum server completed successfully"), // Log removed
                    Err(_e) => {} // eprintln!("[DEBUG] Error in Axum server: {:?}", e), // Log removed
                }
            });

            // --- End of Axum server startup code addition ---

            Ok(())
        })
        .manage(app_state) // Manage AppState with Tauri
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
