fn main() {
    tauri_build::build()
}

// 필요한 명령 목록 정의
const COMMANDS: &[&str] = &["get_mcp_data"];
