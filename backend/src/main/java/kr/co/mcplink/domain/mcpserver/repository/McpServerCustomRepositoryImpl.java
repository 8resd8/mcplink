package kr.co.mcplink.domain.mcpserver.repository;

import kr.co.mcplink.domain.mcpserver.entity.McpServer;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Repository
public class McpServerCustomRepositoryImpl implements McpServerCustomRepository {

    private final MongoTemplate mongoTemplate;

    public McpServerCustomRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public long countAll() {
        return mongoTemplate.count(new Query(), McpServer.class);
    }

    @Override
    public long countRemaining(Long cursor) {
        Query query = new Query();

        if (cursor != null) {
            McpServer last = mongoTemplate.findOne(
                    Query.query(Criteria.where("seq").is(cursor)),
                    McpServer.class
            );
            if (last != null) {
                long lastStars = last.getStars();

                query.addCriteria(new Criteria().orOperator(
                        Criteria.where("stars").lt(lastStars),
                        new Criteria().andOperator(
                                Criteria.where("stars").is(lastStars),
                                Criteria.where("seq").gt(cursor)
                        )
                ));
            }
        }

        return mongoTemplate.count(query, McpServer.class);
    }

    @Override
    public List<McpServer> listAll(int size, Long cursor) {
        Query query = new Query();

        if (cursor != null) {
            McpServer last = mongoTemplate.findOne(
                    Query.query(Criteria.where("seq").is(cursor)),
                    McpServer.class
            );
            if (last != null) {
                long lastStars = last.getStars();

                query.addCriteria(new Criteria().orOperator(
                        Criteria.where("stars").lt(lastStars),
                        new Criteria().andOperator(
                                Criteria.where("stars").is(lastStars),
                                Criteria.where("seq").gt(cursor)
                        )
                ));
            }
        }

        query.with(Sort.by(
                        Sort.Order.desc("stars"),
                        Sort.Order.asc("seq")
                ))
                .limit(size);

        return mongoTemplate.find(query, McpServer.class);
    }

    @Override
    public long countByName(String name) {
        Pattern p = Pattern.compile(".*" + Pattern.quote(name) + ".*", Pattern.CASE_INSENSITIVE);
        Query regexCount = new Query()
                .addCriteria(Criteria.where("mcpServers.name").regex(p));

        return mongoTemplate.count(regexCount, McpServer.class);
    }

    @Override
    public long countRemainingByName(String name, Long cursor) {
        Pattern p = Pattern.compile(".*" + Pattern.quote(name) + ".*", Pattern.CASE_INSENSITIVE);
        Query query = new Query()
                .addCriteria(Criteria.where("mcpServers.name").regex(p));

        if (cursor != null) {
            McpServer last = mongoTemplate.findOne(
                    Query.query(Criteria.where("seq").is(cursor)),
                    McpServer.class
            );
            if (last != null) {
                long lastStars = last.getStars();

                query.addCriteria(new Criteria().orOperator(
                        Criteria.where("stars").lt(lastStars),
                        new Criteria().andOperator(
                                Criteria.where("stars").is(lastStars),
                                Criteria.where("seq").gt(cursor)
                        )
                ));
            }
        }

        return mongoTemplate.count(query, McpServer.class);
    }

    @Override
    public List<McpServer> searchByName(String name, int size, Long cursor) {
        Pattern p = Pattern.compile(".*" + Pattern.quote(name) + ".*", Pattern.CASE_INSENSITIVE);
        Query query = new Query()
                .addCriteria(Criteria.where("mcpServers.name").regex(p));

        if (cursor != null) {
            McpServer last = mongoTemplate.findOne(
                    Query.query(Criteria.where("seq").is(cursor)),
                    McpServer.class
            );
            if (last != null) {
                long lastStars = last.getStars();
                query.addCriteria(new Criteria().orOperator(
                        Criteria.where("stars").lt(lastStars),
                        new Criteria().andOperator(
                                Criteria.where("stars").is(lastStars),
                                Criteria.where("seq").gt(cursor)
                        )
                ));
            }
        }

        query.with(Sort.by(
                        Sort.Order.desc("stars"),
                        Sort.Order.asc("seq")
                ))
                .limit(size);

        return mongoTemplate.find(query, McpServer.class);
    }

    public List<McpServer> findBySeqInOrder(List<Long> pageIds) {
        Query query = new Query()
                .addCriteria(Criteria.where("seq").in(pageIds));
        List<McpServer> servers = mongoTemplate.find(query, McpServer.class);

        Map<Long, McpServer> serverMap = servers.stream()
                .collect(Collectors.toMap(McpServer::getSeq, Function.identity()));

        return pageIds.stream()
                .map(serverMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void incrementViews(Long seq) {
        Query query = Query.query(Criteria.where("seq").is(seq));
        Update update = new Update().inc("views", 1);
        mongoTemplate.updateFirst(query, update, McpServer.class);
    }

    @Override
    public void updateScanned(Long seq) {
        Query query = Query.query(Criteria.where("seq").is(seq));
        Update update = new Update().set("scanned", true);
        mongoTemplate.updateFirst(query, update, McpServer.class);
    }

    @Override
    public void updateNotScanned(Long seq) {
        Query query = Query.query(Criteria.where("seq").is(seq));
        Update update = new Update().set("scanned", false);
        mongoTemplate.updateFirst(query, update, McpServer.class);
    }
}