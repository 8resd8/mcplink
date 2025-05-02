package kr.co.mcplink.domain.mcpserver.repository;

import kr.co.mcplink.domain.mcpserver.entity.McpServer;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    public List<McpServer> listAll(int limit, Long cursor) {
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
                .limit(limit);

        return mongoTemplate.find(query, McpServer.class);
    }

    @Override
    public long countByName(String q) {
        TextCriteria tc = TextCriteria.forDefaultLanguage().matching(q);
        return mongoTemplate.count(TextQuery.queryText(tc),
                McpServer.class);
    }

    @Override
    public List<McpServer> searchByName(String q, int limit, Long cursor) {
        TextCriteria tc = TextCriteria.forDefaultLanguage().matching(q);
        Query query = TextQuery.queryText(tc);

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
                .limit(limit);

        return mongoTemplate.find(query, McpServer.class);
    }

    @Override
    public void incrementViews(Long seq) {
        Query query = Query.query(Criteria.where("seq").is(seq));
        Update update = new Update().inc("views", 1);
        mongoTemplate.updateFirst(query, update, McpServer.class);
    }
}