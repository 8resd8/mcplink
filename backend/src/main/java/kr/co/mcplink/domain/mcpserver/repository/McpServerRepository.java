package kr.co.mcplink.domain.mcpserver.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import kr.co.mcplink.domain.mcpserver.entity.McpServer;

@Repository
public interface McpServerRepository extends MongoRepository<McpServer, String>, McpServerCustomRepository {
    Optional<McpServer> findBySeq(Long seq);

    List<McpServer> findByDetailNameContaining(String name);
}