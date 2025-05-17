package kr.co.mcplink.domain.schedule.v3.repository;

import kr.co.mcplink.domain.schedule.v3.entity.GeminiPendingQueueV3;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeminiPendingQueueV3Repository extends MongoRepository<GeminiPendingQueueV3, String> {

    boolean existsByServerId(String serverId);

    long countByProcessedFalse();

    Optional<GeminiPendingQueueV3> findByServerId(String serverId);

    Optional<GeminiPendingQueueV3> findTop1ByProcessedFalseOrderBySeqAsc();

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'processed': ?1, 'updated_at': new Date() } }")
    long updateProcessedById(String _id, boolean processed);
}