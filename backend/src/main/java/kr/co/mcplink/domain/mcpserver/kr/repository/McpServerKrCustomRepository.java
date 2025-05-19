package kr.co.mcplink.domain.mcpserver.kr.repository;

import kr.co.mcplink.domain.mcpserver.v3.entity.McpServerV3;

import java.util.List;

public interface McpServerKrCustomRepository {

    long countRemaining(Long cursor);
    List<McpServerV3> listAll(int size, Long cursor);
    List<McpServerV3> listAllWithOffset(int size, int offset);
    long countByName(String name);
    long countRemainingByName(String name, Long cursor);
    List<McpServerV3> searchByName(String name, int size, Long cursor);
    List<McpServerV3> searchByNameWithOffset(String name, int size, int offset);
    long updateSummary(String id, String description, List<String> tags);
}