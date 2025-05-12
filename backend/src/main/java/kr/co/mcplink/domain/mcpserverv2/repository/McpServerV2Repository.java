package kr.co.mcplink.domain.mcpserverv2.repository;


import kr.co.mcplink.domain.mcpserver.entity.SecurityRank;
import kr.co.mcplink.domain.mcpserverv2.entity.McpServerV2;
import org.springframework.data.mongodb.repository.CountQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public interface McpServerV2Repository extends MongoRepository<McpServerV2, String>, McpServerV2CustomRepository {

    @CountQuery("{}")
    long countAll();

    @CountQuery("{ 'name': { $regex: ?0, $options: 'i' } }")
    long countByName(String nameRegex);

    Optional<McpServerV2> findBySeq(Long seq);

    @Query("{ 'seq': { $in: ?0 } }")
    List<McpServerV2> findBySeqIn(Collection<Long> seqs);

    default List<McpServerV2> findBySeqInOrder(List<Long> seqs) {
        Map<Long, McpServerV2> map = findBySeqIn(seqs)
                .stream()
                .collect(Collectors.toMap(McpServerV2::getSeq, Function.identity()));

        return seqs.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    boolean existsByUrl(String url);

    @Update("{ '$inc': { 'views': 1 } }")
    long findAndIncrementViewsBySeq(Long seq);

    /*
    vvvvvvvvvvvvvvvvvvvv McpSecurity vvvvvvvvvvvvvvvvvvvv
     */

    List<McpServerV2> findByOfficialFalse();

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'scanned': true } }")
    long updateScannedStatusById(String _id);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'securityRank': ?1 } }")
    long updateSecurityRankById(String _id, SecurityRank securityRank);
}