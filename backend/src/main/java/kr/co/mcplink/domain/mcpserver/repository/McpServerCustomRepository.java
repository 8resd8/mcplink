package kr.co.mcplink.domain.mcpserver.repository;


import kr.co.mcplink.domain.mcpserver.entity.McpServer;

import java.util.List;

public interface McpServerCustomRepository {

    long countAll();
    List<McpServer> listAll(int limit, Long cursor);
    long countByName(String q);
    List<McpServer> searchByName(String q, int limit, Long cursor);
    void incrementViews(Long seq);
}