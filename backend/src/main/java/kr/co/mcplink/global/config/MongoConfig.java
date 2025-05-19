package kr.co.mcplink.global.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
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
import java.util.Collections;
import java.util.List;

@Configuration
@EnableMongoAuditing 
@EnableMongoRepositories(
    basePackages = {
            "kr.co.mcplink.domain.mcpserver.v1.repository",
            "kr.co.mcplink.domain.mcpserver.v2.repository",
            "kr.co.mcplink.domain.mcpserver.v3.repository",
            "kr.co.mcplink.domain.schedule.v2.repository",
            "kr.co.mcplink.domain.schedule.v3.repository"
    },
    mongoTemplateRef = "mongoTemplate"
)
public class MongoConfig {

    @Value("${spring.data.mongodb.host}")
    private String host;

    @Value("${spring.data.mongodb.port}")
    private int port;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Value("${spring.data.mongodb.username}")
    private String username;

    @Value("${spring.data.mongodb.password}")
    private String password;

    @Primary
    @Bean(name = "mongoClient")
    public MongoClient mongoClient() {

        if (username != null && !username.isEmpty()) {
            MongoCredential credential = MongoCredential.createCredential(
                    username, database, password.toCharArray());

            return MongoClients.create(MongoClientSettings.builder()
                    .applyToClusterSettings(builder ->
                            builder.hosts(Collections.singletonList(new ServerAddress(host, port))))
                    .credential(credential)
                    .build());
        } else {

            return MongoClients.create(String.format("mongodb://%s:%d", host, port));
        }
    }

    @Primary
    @Bean(name = "mongoDatabaseFactory")
    public MongoDatabaseFactory mongoDatabaseFactory(@Qualifier("mongoClient") MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }

    @Primary
    @Bean(name = "mongoTemplate")
    public MongoTemplate mongoTemplate(
            @Qualifier("mongoDatabaseFactory") MongoDatabaseFactory mongoDatabaseFactory,
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

    @Primary
    @Bean(name = "mongoTransactionManager")
    public MongoTransactionManager mongoTransactionManager(
            @Qualifier("mongoDatabaseFactory") MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTransactionManager(mongoDatabaseFactory);
    }
}