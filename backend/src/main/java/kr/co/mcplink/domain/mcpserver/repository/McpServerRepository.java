package kr.co.mcplink.domain.mcpserver.repository;

import kr.co.mcplink.domain.mcpserver.entity.McpServer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface McpServerRepository extends MongoRepository<McpServer, String>, McpServerCustomRepository {

    @Override
    McpServer save(McpServer server);

    McpServer findBySeq(Long seq);

    @Query(value = "{ 'seq': { $gt: ?0 } }", count = true)
    long countBySeqGreaterThan(Long seq);
}