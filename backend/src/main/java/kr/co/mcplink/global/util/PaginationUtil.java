package kr.co.mcplink.global.util;

import kr.co.mcplink.domain.mcpserver.dto.PageInfoDto;
import kr.co.mcplink.domain.mcpserver.entity.McpServer;

import java.util.List;

public class PaginationUtil {

    public static PageInfoDto buildPageInfo(List<McpServer> items, long totalItems, long remainings) {
        long startCursor = items.isEmpty() ? 0 : items.get(0).getSeq();
        long endCursor   = items.isEmpty() ? 0 : items.get(items.size() - 1).getSeq();
        boolean hasNext  = remainings > 0;

        return PageInfoDto.builder()
                .startCursor(startCursor)
                .endCursor(endCursor)
                .hasNextPage(hasNext)
                .totalItems(totalItems)
                .build();
    }
}