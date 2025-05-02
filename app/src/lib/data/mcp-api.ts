import { invoke } from "@tauri-apps/api/core"

export interface MCPCard {
  id: number
  title: string
  description: string
  url: string // GitHub 리포지토리 URL
  stars: number // GitHub 스타 수
}

/**
 * Rust 백엔드를 통해 외부 서버에서 MCP 카드 데이터를 가져옵니다.
 * @returns MCP 카드 데이터 배열
 */
export async function fetchMCPCards(searchTerm?: string): Promise<MCPCard[]> {
  try {
    // searchTerm이 있을 때만 파라미터로 전달
    const response = searchTerm ? await invoke<MCPCard[]>("get_mcp_data", { searchTerm }) : await invoke<MCPCard[]>("get_mcp_data")
    return response
  } catch (error) {
    console.error("MCP 카드 데이터를 가져오는 중 오류 발생:", error)
    throw error
  }
}
