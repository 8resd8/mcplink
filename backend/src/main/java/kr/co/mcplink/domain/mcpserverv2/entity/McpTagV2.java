package kr.co.mcplink.domain.mcpserverv2.entity;

import kr.co.mcplink.global.annotation.AutoSequence;
import kr.co.mcplink.global.common.Constants;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AutoSequence(collection = Constants.COLLECTION_MCP_TAGS_V2)
@Document(Constants.COLLECTION_MCP_TAGS)
public class McpTagV2 {

    @Id
    private String id;
    private Long seq;

    @Indexed(unique = true)
    private String tag;
}