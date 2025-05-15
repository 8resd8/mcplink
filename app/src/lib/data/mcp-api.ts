import { invoke } from "@tauri-apps/api/core"

export interface MCPCard {
  id: number
  title: string
  description: string
  url: string // GitHub repository URL
  stars: number // GitHub stars count
  installed?: boolean // Installation status (optional property added)
}

export interface PageInfo {
  has_next_page: boolean
  end_cursor: number | null
  total_items: number
}

export interface MCPCardResponse {
  cards: MCPCard[]
  page_info: PageInfo
}

// Interface for detailed MCP card information
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
        return await invoke<MCPCardResponse>("get_mcp_data", { cursorId })
      } else {
        return await invoke<MCPCardResponse>("get_mcp_data")
      }
    } else {
      return await invoke<MCPCardResponse>("get_mcp_data", { searchTerm })
    }
  } catch (error) {
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
    return await invoke<MCPCardDetail>("get_mcp_detail_data", { id }) // Pass id wrapped in an object
  } catch (error) {
    throw error
  }
}
