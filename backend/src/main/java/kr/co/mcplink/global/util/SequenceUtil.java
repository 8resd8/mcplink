package kr.co.mcplink.global.util;

import kr.co.mcplink.global.entity.SequenceCounter;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class SequenceUtil {

    private final MongoOperations mongoOps;

    public SequenceUtil(MongoOperations mongoOps) {
        this.mongoOps = mongoOps;
    }

    public long generateSequence(String seqName) {
        BasicQuery query = new BasicQuery(
                String.format("{ \"_id\" : \"%s\" }", seqName)
        );
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions
                .options()
                .returnNew(true)
                .upsert(true);

        SequenceCounter counter = mongoOps.findAndModify(
                query,
                update,
                options,
                SequenceCounter.class
        );
        return (counter != null) ? counter.getSeq() : 1L;
    }
}