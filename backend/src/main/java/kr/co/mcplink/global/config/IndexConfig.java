package kr.co.mcplink.global.config;

import kr.co.mcplink.global.annotation.AutoIndex;
import kr.co.mcplink.global.common.Constants;
import kr.co.mcplink.global.util.IndexUtil;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IndexConfig
        implements ApplicationListener<ContextRefreshedEvent> {

    private final MongoMappingContext mappingContext;
    private final IndexUtil indexUtil;

    public IndexConfig(MongoMappingContext mappingContext, IndexUtil indexUtil) {
        this.mappingContext = mappingContext;
        this.indexUtil = indexUtil;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        mappingContext.getPersistentEntities().forEach(pe -> {
            Class<?> clazz = pe.getType();
            AutoIndex ann  = clazz.getAnnotation(AutoIndex.class);
            if (ann == null) {
                return;
            }
            String collection = ann.collection();

            indexUtil.createCompoundIndex(
                    collection,
                    Constants.IDX_MCP_SERVERS_SORT,
                    Map.of("stars", -1, "seq", 1)
            );

            indexUtil.createTextIndex(
                    collection,
                    Constants.IDX_MCP_SERVERS_NAME_SEARCH,
                    "mcpServers.name"
            );
        });
    }
}