import { invoke } from "@tauri-apps/api/core"

export interface MCPCard {
  id: number
  title: string
  description: string
}

/**
 * Fetch MCP card data from the external server via the Rust backend.
 * @returns MCP card data array
 */
export async function fetchMCPCards(): Promise<MCPCard[]> {
  try {
    // Call the get_mcp_data function in the Rust backend
    const response = await invoke<MCPCard[]>("get_mcp_data")
    return response
  } catch (error) {
    console.error("Error fetching MCP card data:", error)
    throw error
  }
}
