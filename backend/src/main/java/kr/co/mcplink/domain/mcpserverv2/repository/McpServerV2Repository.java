package kr.co.mcplink.domain.mcpserverv2.repository;


import kr.co.mcplink.domain.mcpserver.entity.SecurityRank;
import kr.co.mcplink.domain.mcpserverv2.entity.McpServerV2;
import org.springframework.data.mongodb.repository.CountQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface McpServerV2Repository extends MongoRepository<McpServerV2, String>, McpServerV2CustomRepository {

    @CountQuery("{}")
    long countAll();

    @CountQuery("{ $or: [ {'mcpServers.name': { $regex: ?0, $options: 'i' }}, {'mcpServers.description': { $regex: ?0, $options: 'i' }} ]}")
    long countByName(String nameRegex);

    Optional<McpServerV2> findBySeq(Long seq);

    boolean existsByUrl(String url);

    @Query(value = "{'mcpServers.description': {$regex: ?0, $options: 'i'}}", fields = "{'_id': { $toString: '$_id' }}")
    List<String> findIdsByDetailDescriptionContaining(String text);

    @Update("{ '$inc': { 'views': 1 } }")
    long findAndIncrementViewsBySeq(Long seq);

    /*
    vvvvvvvvvvvvvvvvvvvv McpSecurity vvvvvvvvvvvvvvvvvvvv
     */

    List<McpServerV2> findByOfficialFalse();

    List<McpServerV2> findByScannedFalse();

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'scanned': true } }")
    long updateScannedStatusById(String _id);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'securityRank': ?1 } }")
    long updateSecurityRankById(String _id, SecurityRank securityRank);
}