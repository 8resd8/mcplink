package kr.co.mcplink.domain.schedule.v3.repository;

import kr.co.mcplink.domain.schedule.v3.entity.GithubPendingQueueV3;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GithubPendingQueueV3Repository extends MongoRepository<GithubPendingQueueV3, String> {

    boolean existsByName(String name);

    long countByProcessedFalse();

    List<GithubPendingQueueV3> findTop10ByProcessedFalseOrderBySeqAsc();

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'processed': ?1, 'updated_at': new Date() } }")
    long updateProcessedById(String _id, boolean processed);
}