package kr.co.mcplink.global.common;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = Constants.COLLECTION_SEQUENCE_COUNTER)
@Getter
public class SequenceCounterEntity {
    @Id
    private String id;
    private long seq;
}