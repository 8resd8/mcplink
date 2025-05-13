package kr.co.mcplink.domain.schedule.repository;

import kr.co.mcplink.domain.schedule.entity.GithubPendingQueue;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface GithubPendingQueueRepository extends MongoRepository<GithubPendingQueue, String> {

    boolean existsByName(String name);

    long countByCompletedAtNotNull();

    long deleteByCompletedAtNotNull();

    List<GithubPendingQueue> findByCompletedAtNullOrderBySeqAsc();

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'completedAt': ?1 } }")
    long updateCompletedAtById(String _id, Instant completedAt);
}