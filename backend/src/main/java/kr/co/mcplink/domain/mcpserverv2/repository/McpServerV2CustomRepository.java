package kr.co.mcplink.domain.mcpserverv2.repository;

import kr.co.mcplink.domain.mcpserverv2.entity.McpServerV2;

import java.util.List;
import java.util.Map;

public interface McpServerV2CustomRepository {

    long countRemaining(Long cursor);
    List<McpServerV2> listAll(int size, Long cursor);
    long countRemainingByName(String name, Long cursor);
    List<McpServerV2> searchByName(String name, int size, Long cursor);
    long updateMetaData(
        String id,
        String url,
        int stars,
        boolean official,
        String name,
        String command,
        List<String> args,
        Map<String, String> env
    );

    long updateSummary(
        String id,
        String description,
        List<String> tags
    );
}