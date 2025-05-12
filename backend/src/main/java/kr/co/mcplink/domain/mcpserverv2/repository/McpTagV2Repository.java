package kr.co.mcplink.domain.mcpserverv2.repository;

import kr.co.mcplink.domain.mcpserverv2.entity.McpTagV2;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public interface McpTagV2Repository extends MongoRepository<McpTagV2, String> {

    default List<String> listAll() {
        return findAll().stream()
                .map(McpTagV2::getTag)
                .collect(Collectors.toList());
    }

    boolean existsByTag(String tag);
}