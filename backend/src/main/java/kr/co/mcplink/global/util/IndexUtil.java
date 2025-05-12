package kr.co.mcplink.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class IndexUtil {

	private final MongoOperations mongoOperations;

	public void createCompoundIndex(String collection, String indexName, Map<String, Integer> fields) {
		Index idx = new Index().named(indexName);
		fields.forEach((field, dir) -> {
			if (dir > 0) {
				idx.on(field, Sort.Direction.ASC);
			} else {
				idx.on(field, Sort.Direction.DESC);
			}
		});
		mongoOperations.indexOps(collection).ensureIndex(idx);
	}

	public void createTextIndex(String collection, String indexName, String field) {
		TextIndexDefinition txtIdx = new TextIndexDefinition.TextIndexDefinitionBuilder()
			.onField(field)
			.named(indexName)
			.build();
		mongoOperations.indexOps(collection).ensureIndex(txtIdx);
	}

	public void createTextIndexV2(String collection, String indexName, String... fields) {
		TextIndexDefinition.TextIndexDefinitionBuilder builder =
				new TextIndexDefinition.TextIndexDefinitionBuilder();
		for (String field : fields) {
			builder.onField(field);
		}
		TextIndexDefinition txtIdx = builder
				.named(indexName)
				.build();
		mongoOperations.indexOps(collection).ensureIndex(txtIdx);
	}
}