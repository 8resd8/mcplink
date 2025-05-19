package kr.co.mcplink.domain.mcpserver.kr.repository;

import kr.co.mcplink.domain.mcpserver.kr.entity.SynonymMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SynonymMappingRepository extends MongoRepository<SynonymMapping, String> {

    @Query(value = "{ 'synonyms.0': ?0 }", exists = true)
    boolean existsByOriginalWord (String originalWord);
}