package kr.co.mcplink.domain.mcpserver.v3.repository;

import kr.co.mcplink.domain.mcpserver.v3.entity.McpServerV3;

import java.util.List;

public interface McpServerV3CustomRepository {

    long countRemaining(Long cursor);
    List<McpServerV3> listAll(int size, Long cursor);
    long countRemainingByName(String name, Long cursor);
    List<McpServerV3> searchByName(String name, int size, Long cursor);
    long updateSummary(String id, String description, List<String> tags);
}