package kr.co.mcplink.global.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableMongoRepositories(
        basePackages = {
//                "kr.co.mcplink.domain.mcpserver.v3.repository",
//                "kr.co.mcplink.domain.schedule.v3.repository"
        },
        mongoTemplateRef = "atlasMongoTemplate"
)
public class MongoAtlasConfig {

    @Value("${mongodb.atlas.uri}")
    private String mongoAtlasUri;

    @Value("${mongodb.atlas.database}")
    private String mongoAtlasDatabase;

    @Bean(name = "atlasMongoClient")
    public MongoClient atlasMongoClient() {
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoAtlasUri))
                .serverApi(serverApi)
                .build();

        return MongoClients.create(settings);
    }

    @Bean(name = "atlasMongoDatabaseFactory")
    public MongoDatabaseFactory atlasMongoDatabaseFactory(@Qualifier("atlasMongoClient") MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, mongoAtlasDatabase);
    }

    @Bean(name = "atlasMongoTemplate")
    public MongoTemplate atlasMongoTemplate(
            @Qualifier("atlasMongoDatabaseFactory") MongoDatabaseFactory mongoDatabaseFactory,
            MongoMappingContext context) {

        MappingMongoConverter converter = new MappingMongoConverter(
                new DefaultDbRefResolver(mongoDatabaseFactory), context);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));

        Collection<Converter<?, ?>> jsr310Converters = Jsr310Converters.getConvertersToRegister();
        List<Converter<?, ?>> converterList = new ArrayList<>(jsr310Converters);
        converter.setCustomConversions(new MongoCustomConversions(converterList));
        converter.afterPropertiesSet();

        return new MongoTemplate(mongoDatabaseFactory, converter);
    }

    @Bean(name = "atlasMongoTransactionManager")
    public MongoTransactionManager atlasMongoTransactionManager(
            @Qualifier("atlasMongoDatabaseFactory") MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTransactionManager(mongoDatabaseFactory);
    }
}