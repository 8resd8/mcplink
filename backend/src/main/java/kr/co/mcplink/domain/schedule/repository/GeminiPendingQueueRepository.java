package kr.co.mcplink.domain.schedule.repository;

import kr.co.mcplink.domain.schedule.entity.GeminiPendingQueue;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeminiPendingQueueRepository extends MongoRepository<GeminiPendingQueue, String> {

    boolean existsByServerId(String serverId);

    long countByProcessedFalse();

//    long countByProcessedTrue();

//    long deleteByProcessedTrue();

    Optional<GeminiPendingQueue> findByServerId(String serverId);

    Optional<GeminiPendingQueue> findTop1ByProcessedFalseOrderBySeqAsc();

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'processed': ?1 } }")
    long updateProcessedById(String _id, boolean processed);
}