package kr.co.mcplink.domain.mcpserver.repository;

import kr.co.mcplink.domain.mcpserver.entity.McpServer;
import kr.co.mcplink.domain.mcpserver.entity.SecurityRank;
import org.springframework.data.mongodb.repository.CountQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public interface McpServerRepository extends MongoRepository<McpServer, String>, McpServerCustomRepository {

    @CountQuery("{}")
    long countAll();

    @CountQuery("{ 'name': { $regex: ?0, $options: 'i' } }")
    long countByName(String nameRegex);

    Optional<McpServer> findBySeq(Long seq);

    @Query("{ 'seq': { $in: ?0 } }")
    List<McpServer> findBySeqIn(Collection<Long> seqs);

    default List<McpServer> findBySeqInOrder(List<Long> seqs) {
        Map<Long, McpServer> map = findBySeqIn(seqs)
                .stream()
                .collect(Collectors.toMap(McpServer::getSeq, Function.identity()));

        return seqs.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Update("{ '$inc': { 'views': 1 } }")
    long findAndIncrementViewsBySeq(Long seq);

    // Repository method to fetch servers where official is false
    List<McpServer> findByOfficialFalse();

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'scanned': true } }")
    long updateScannedStatusById(String mcpServerId);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'securityRank': ?1 } }")
    long updateSecurityRankById(String mcpServerId, SecurityRank securityRank);
}