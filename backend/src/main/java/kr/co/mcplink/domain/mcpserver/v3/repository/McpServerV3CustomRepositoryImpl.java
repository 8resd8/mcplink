package kr.co.mcplink.domain.mcpserver.v3.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import kr.co.mcplink.domain.mcpserver.v3.entity.McpServerV3;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Date;

@Repository
@RequiredArgsConstructor
public class McpServerV3CustomRepositoryImpl implements McpServerV3CustomRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public long countRemaining(Long cursor) {
        Aggregation agg = Aggregation.newAggregation(
                createCursorMatch(cursor),
                context -> new Document("$count", "count")
        );

        return aggregateCount(agg);
    }

    @Override
    public List<McpServerV3> listAll(int size, Long cursor) {
        Aggregation agg = Aggregation.newAggregation(
                createCursorMatch(cursor),
                context -> new Document("$sort",
                        new Document("stars", -1).append("seq", 1)),
                context -> new Document("$limit", size)
        );

        return mongoTemplate
                .aggregate(agg, mongoTemplate.getCollectionName(McpServerV3.class), McpServerV3.class)
                .getMappedResults();
    }

    @Override
    public long countRemainingByName(String name, Long cursor) {
        Aggregation agg = Aggregation.newAggregation(
                createNameAndDescriptionMatch(name),
                createCursorMatch(cursor),
                context -> new Document("$count", "count")
        );

        return aggregateCount(agg);
    }

    @Override
    public List<McpServerV3> searchByName(String name, int size, Long cursor) {
        Aggregation agg = Aggregation.newAggregation(
                createNameAndDescriptionMatch(name),
                createCursorMatch(cursor),
                context -> new Document("$sort",
                        new Document("stars", -1).append("seq", 1)),
                context -> new Document("$limit", size)
        );

        return mongoTemplate
                .aggregate(agg, mongoTemplate.getCollectionName(McpServerV3.class), McpServerV3.class)
                .getMappedResults();
    }

    @Override
    public long updateSummary(String id, String description, List<String> tags) {
        Document filter = new Document("_id", new ObjectId(id));

        Date now = new Date();
        Document set = new Document()
                .append("mcpServers.description", description)
                .append("tags", tags)
                .append("updated_at", now);
        Document update = new Document("$set", set);

        MongoCollection<Document> coll = mongoTemplate.getCollection(
                mongoTemplate.getCollectionName(McpServerV3.class)
        );
        UpdateResult result = coll.updateOne(filter, update);

        return result.getModifiedCount();
    }

    private AggregationOperation createCursorMatch(Long cursor) {
        return context -> {
            if (cursor == null || cursor <= 0) {
                return new Document("$match", new Document());
            }
            String queryStr = String.format("{ \"seq\" : %d }", cursor);
            Query query = new BasicQuery(queryStr);
            McpServerV3 last = mongoTemplate.findOne(query, McpServerV3.class);

            if (last == null) {
                return new Document("$match", new Document("seq", new Document("$exists", false)));
            }
            long lastStars = last.getStars();
            Document orCond = new Document("$or", Arrays.asList(
                    new Document("stars", new Document("$lt", lastStars)),
                    new Document("$and", Arrays.asList(
                            new Document("stars", lastStars),
                            new Document("seq", new Document("$gt", cursor))
                    ))
            ));

            return new Document("$match", orCond);
        };
    }

    private AggregationOperation createNameAndDescriptionMatch(String name) {
        String regex = ".*" + Pattern.quote(name) + ".*";
        Document pattern = new Document("$regex", regex).append("$options", "i");

        return context -> new Document("$match",
                new Document("$or", Arrays.asList(
                        new Document("mcpServers.name", pattern),
                        new Document("mcpServers.description", pattern)
                ))
        );
    }

    private long aggregateCount(Aggregation agg) {
        Document result = mongoTemplate
                .aggregate(agg, mongoTemplate.getCollectionName(McpServerV3.class), Document.class)
                .getUniqueMappedResult();
        if (result == null) {
            return 0L;
        }
        Number countNum = result.get("count", Number.class);

        return countNum == null ? 0L : countNum.longValue();
    }
}