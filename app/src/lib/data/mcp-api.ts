import { invoke } from "@tauri-apps/api/core"

export interface MCPCard {
  id: number
  title: string
  description: string
}

/**
 * Rust 백엔드를 통해 외부 서버에서 MCP 카드 데이터를 가져옵니다.
 * @returns MCP 카드 데이터 배열
 */
export async function fetchMCPCards(): Promise<MCPCard[]> {
  try {
    // Rust 백엔드의 get_mcp_data 함수 호출
    const response = await invoke<MCPCard[]>("get_mcp_data")
    return response
  } catch (error) {
    console.error("MCP 카드 데이터를 가져오는 중 오류 발생:", error)
    throw error
  }
}
