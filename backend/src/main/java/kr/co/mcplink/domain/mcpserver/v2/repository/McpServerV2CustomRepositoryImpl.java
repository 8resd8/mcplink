package kr.co.mcplink.domain.mcpserver.v2.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import kr.co.mcplink.domain.mcpserver.v2.entity.McpServerV2;
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

@Repository
public class McpServerV2CustomRepositoryImpl implements McpServerV2CustomRepository {

    private final MongoTemplate mongoTemplate;

    public McpServerV2CustomRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public long countRemaining(Long cursor) {
        Aggregation agg = Aggregation.newAggregation(
                createCursorMatch(cursor),
                context -> new Document("$count", "count")
        );

        return aggregateCount(agg);
    }

    @Override
    public List<McpServerV2> listAll(int size, Long cursor) {
        Aggregation agg = Aggregation.newAggregation(
                createCursorMatch(cursor),
                context -> new Document("$sort",
                        new Document("stars", -1).append("seq", 1)),
                context -> new Document("$limit", size)
        );

        return mongoTemplate
                .aggregate(agg, mongoTemplate.getCollectionName(McpServerV2.class), McpServerV2.class)
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
    public List<McpServerV2> searchByName(String name, int size, Long cursor) {
        Aggregation agg = Aggregation.newAggregation(
                createNameAndDescriptionMatch(name),
                createCursorMatch(cursor),
                context -> new Document("$sort",
                        new Document("stars", -1).append("seq", 1)),
                context -> new Document("$limit", size)
        );

        return mongoTemplate
                .aggregate(agg, mongoTemplate.getCollectionName(McpServerV2.class), McpServerV2.class)
                .getMappedResults();
    }

    private AggregationOperation createCursorMatch(Long cursor) {
        return context -> {
            if (cursor == null || cursor <= 0) {
                return new Document("$match", new Document());
            }
            String queryStr = String.format("{ \"seq\" : %d }", cursor);
            Query query = new BasicQuery(queryStr);
            McpServerV2 last = mongoTemplate.findOne(query, McpServerV2.class);

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
                .aggregate(agg, mongoTemplate.getCollectionName(McpServerV2.class), Document.class)
                .getUniqueMappedResult();
        if (result == null) {
            return 0L;
        }
        Number countNum = result.get("count", Number.class);

        return countNum == null ? 0L : countNum.longValue();
    }

//    @Override
//    public long updateMetaData(
//        String id,
//        String url,
//        int stars,
//        boolean official,
//        String name,
//        String command,
//        List<String> args,
//        Map<String, String> env
//    ) {
//        Document filter = new Document("_id", id);
//        Document set = new Document()
//                .append("url", url)
//                .append("stars", stars)
//                .append("official", official)
//                .append("mcpServers.name", name)
//                .append("mcpServers.command", command)
//                .append("mcpServers.args", args)
//                .append("mcpServers.env", env);
//        Document update = new Document("$set", set);
//
//        MongoCollection<Document> coll = mongoTemplate.getCollection(
//            mongoTemplate.getCollectionName(McpServerV2.class)
//        );
//        UpdateResult result = coll.updateOne(filter, update);
//
//        return result.getModifiedCount();
//    }

    @Override
    public long updateSummary(
        String id,
        String description,
        List<String> tags
    ) {
        Document filter = new Document("_id", new ObjectId(id));

        Document set = new Document()
                .append("mcpServers.description", description)
                .append("tags", tags);
        Document update = new Document("$set", set);

        MongoCollection<Document> coll = mongoTemplate.getCollection(
            mongoTemplate.getCollectionName(McpServerV2.class)
        );
        UpdateResult result = coll.updateOne(filter, update);

        return result.getModifiedCount();
    }
}