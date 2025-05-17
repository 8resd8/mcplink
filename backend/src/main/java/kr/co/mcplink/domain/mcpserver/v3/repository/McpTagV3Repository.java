package kr.co.mcplink.domain.mcpserver.v3.repository;

import kr.co.mcplink.domain.mcpserver.v3.entity.McpTagV3;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public interface McpTagV3Repository extends MongoRepository<McpTagV3, String> {

    default List<String> listAll() {
        return findAll().stream()
                .map(McpTagV3::getTag)
                .collect(Collectors.toList());
    }

    boolean existsByTag(String tag);
}