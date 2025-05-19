package kr.co.mcplink.domain.mcpserver.kr.entity;

import kr.co.mcplink.global.common.Constants;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = Constants.COLLECTION_SYNONYM_MAPPING)
@CompoundIndexes({
        @CompoundIndex(name = "synonym_unique", def = "{'synonyms.0': 1}", unique = true)
})
public class SynonymMapping {

    @Id
    private String id;

    @Builder.Default
    private String mappingType = "explicit";

    private List<String> input;
    private List<String> synonyms;
}