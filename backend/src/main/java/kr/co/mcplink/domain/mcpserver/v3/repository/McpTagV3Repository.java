package kr.co.mcplink.domain.mcpserver.v3.repository;

import kr.co.mcplink.domain.mcpserver.v3.entity.McpTagV3;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    default List<String> findTagsByPage(int queryNum) {
        int pageSize = 100;
        Sort sort = Sort.by(Sort.Direction.ASC, "seq");
        Pageable pageable = PageRequest.of(queryNum, pageSize, sort);
        Page<McpTagV3> tagPage = findAll(pageable);

        return tagPage.getContent().stream()
                .map(McpTagV3::getTag)
                .collect(Collectors.toList());
    }

    boolean existsByTag(String tag);
}