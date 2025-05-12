package kr.co.mcplink.global.entity;

import kr.co.mcplink.global.common.Constants;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = Constants.COLLECTION_SEQUENCE_COUNTER)
@Getter
public class SequenceCounter {
    @Id
    private String id;
    private long seq;
}