package kr.co.mcplink.domain.mcpserver.v3.entity;

import kr.co.mcplink.global.annotation.AutoSequence;
import kr.co.mcplink.global.common.BaseTimeMongoEntity;
import kr.co.mcplink.global.common.Constants;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AutoSequence(collection = Constants.COLLECTION_MCP_TAGS_V3)
@Document(Constants.COLLECTION_MCP_TAGS_V3)
public class McpTagV3 extends BaseTimeMongoEntity {

    @Id
    private String id;
    private Long seq;

    @Indexed(unique = true)
    private String tag;
}