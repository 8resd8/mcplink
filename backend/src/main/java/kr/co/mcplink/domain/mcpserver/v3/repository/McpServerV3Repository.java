package kr.co.mcplink.domain.mcpserver.v3.repository;

import kr.co.mcplink.domain.mcpserver.v1.entity.SecurityRank;
import kr.co.mcplink.domain.mcpserver.v3.entity.McpServerV3;
import org.springframework.data.mongodb.repository.CountQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface McpServerV3Repository extends MongoRepository<McpServerV3, String>, McpServerV3CustomRepository {

    @CountQuery("{}")
    long countAll();

    @CountQuery("{ $or: [ {'mcpServers.name': { $regex: ?0, $options: 'i' }}, {'mcpServers.description': { $regex: ?0, $options: 'i' }} ]}")
    long countByName(String nameRegex);

    Optional<McpServerV3> findBySeq(Long seq);

    boolean existsByUrl(String url);

    @Query(value = "{'mcpServers.description': {$regex: ?0, $options: 'i'}}", fields = "{'_id': { $toString: '$_id' }}")
    List<String> findIdsByDetailDescriptionContaining(String text);

    @Update("{ '$inc': { 'views': 1 }, '$set': { 'updated_at': new Date() } }")
    long findAndIncrementViewsBySeq(Long seq);

    /*
    vvvvvvvvvvvvvvvvvvvv McpSecurity vvvvvvvvvvvvvvvvvvvv
     */

    List<McpServerV3> findByOfficialFalse();

    List<McpServerV3> findByScannedFalse();

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'scanned': true, 'updated_at': new Date() } }")
    long updateScannedStatusById(String _id);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'securityRank': ?1, 'updated_at': new Date() } }")
    long updateSecurityRankById(String _id, SecurityRank securityRank);
}