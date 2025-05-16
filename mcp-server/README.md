# @dw8k/mcp-fallback-server

MCP fallback server that extracts keywords from prompts and triggers tool recommendations.
This server is typically managed and configured via a dedicated GUI application.

## Installation

For direct use or development:

```bash
npm install @dw8k/mcp-fallback-server
```

## Usage

The server can be run using npx (primarily for testing or standalone use):

```bash
npx @dw8k/mcp-fallback-server
```

### Environment Variables

This server relies on environment variables for its configuration, which are expected to be provided by the managing GUI application. Key variables include:

*   `CRAWLER_API_BASE_URL`: Specifies the endpoint for the Crawler API.
*   `GUI_BE_API_BASE_URL`: Specifies the endpoint for the GUI App Backend API.

For detailed information on environment variable setup, please refer to the documentation of the GUI application.

## License

MIT