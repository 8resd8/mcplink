package kr.co.mcplink.domain.gemini.dto;

import java.util.Collections;
import java.util.List;

public record ReadmeSummaryDto(
        String summary,
        List<String> tags
) {
    public ReadmeSummaryDto {
        if (tags == null) {
            tags = Collections.emptyList();
        }
    }
}