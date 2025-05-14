import { invoke } from "@tauri-apps/api/core"

export interface MCPCard {
  id: number
  title: string
  description: string
  url: string // GitHub 리포지토리 URL
  stars: number // GitHub 스타 수
  installed?: boolean // 설치 여부 (선택적 속성으로 추가)
}

export interface PageInfo {
  hasNextPage: boolean
  endCursor: number | null
  totalItems: number
}

export interface MCPCardResponse {
  cards: MCPCard[]
  pageInfo: PageInfo
}

// MCP 카드의 상세 정보 인터페이스
export interface MCPCardDetail extends MCPCard {
  args?: string[]
  env?: Record<string, any>
  command?: string
}

/**
 * Fetch MCP card data from the external server via the Rust backend.
 * @returns MCP card data array
 */
export async function fetchMCPCards(searchTerm?: string, cursorId?: number): Promise<MCPCardResponse> {
  try {
    if (searchTerm === undefined || searchTerm.trim() === "") {
      if (cursorId) {
        console.log(`[mcp-api.ts] fetchMCPCards: Loading next page with cursor ${cursorId}`)
        return await invoke<MCPCardResponse>("get_mcp_data", { cursorId })
      } else {
        console.log("[mcp-api.ts] fetchMCPCards: Loading first page")
        return await invoke<MCPCardResponse>("get_mcp_data")
      }
    } else {
      console.log(`[mcp-api.ts] fetchMCPCards: Searching for "${searchTerm}"`)
      return await invoke<MCPCardResponse>("get_mcp_data", { searchTerm })
    }
  } catch (error) {
    console.error("[mcp-api.ts] Error fetching MCP card data:", error)
    throw error
  }
}

/**
 * Fetch detailed MCP card data from the Rust backend.
 * @param id The ID of the MCP card
 * @returns Detailed MCP card data
 */
export async function fetchMCPCardDetail(id: number): Promise<MCPCardDetail> {
  try {
    console.log(`[mcp-api.ts] fetchMCPCardDetail: Fetching detail for ID ${id}.`)
    return await invoke<MCPCardDetail>("get_mcp_detail_data", { id }) // id를 객체로 감싸서 전달
  } catch (error) {
    console.error(`[mcp-api.ts] Error fetching MCP card detail for ID ${id}:`, error)
    throw error
  }
}
