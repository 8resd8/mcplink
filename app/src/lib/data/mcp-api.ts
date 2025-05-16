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
    let response: MCPCardResponse
    if (searchTerm === undefined || searchTerm.trim() === "") {
      if (cursorId) {
        response = await invoke<MCPCardResponse>("get_mcp_data", { cursorId })
      } else {
        response = await invoke<MCPCardResponse>("get_mcp_data")
      }
    } else {
      response = await invoke<MCPCardResponse>("get_mcp_data", { searchTerm })
    }

    // Check installation status and add 'installed' property
    for (const card of response.cards) {
      try {
        const isInstalled = await invoke<boolean>("is_mcp_server_installed", {
          serverName: card.title,
        })
        card.installed = isInstalled
      } catch (err) {
        console.error(`Failed to check installation status for ${card.title}:`, err)
        card.installed = false
      }
    }

    return response
  } catch (error) {
    throw error
  }
}

/**
 * Define the MCPServerConfig interface
 */
export interface MCPServerConfig {
  command: string
  args?: string[]
  env?: Record<string, any>
  cwd?: string | null
}

/**
 * Fetch detailed MCP card data from the Rust backend.
 * @param id The ID of the MCP card
 * @param title The title (name) of the MCP card (for checking installation status)
 * @returns Detailed MCP card data
 */
export async function fetchMCPCardDetail(id: number, title?: string): Promise<MCPCardDetail> {
  try {
    // Fetch basic information from the API
    const detailFromAPI = await invoke<MCPCardDetail>("get_mcp_detail_data", { id })

    // If title is provided, check installation status
    if (title) {
      try {
        const isInstalled = await invoke<boolean>("is_mcp_server_installed", { serverName: title })

        // If already installed, get values from the configuration file
        if (isInstalled) {
          try {
            const config = await invoke<MCPServerConfig>("read_mcp_server_config", { serverName: title })

            // Merge API results with installed configuration (installed configuration takes precedence)
            return {
              ...detailFromAPI,
              command: config.command || detailFromAPI.command || "",
              args: config.args || detailFromAPI.args || [],
              env: config.env || detailFromAPI.env || {},
            }
          } catch (configErr) {
            console.warn(`Installed config exists but failed to read: ${configErr}`)
            // If configuration read fails, return API data only
            return detailFromAPI
          }
        }
      } catch (installErr) {
        console.warn(`Failed to check installation status: ${installErr}`)
      }
    }

    // Return API results by default
    return detailFromAPI
  } catch (error) {
    throw error
  }
}
