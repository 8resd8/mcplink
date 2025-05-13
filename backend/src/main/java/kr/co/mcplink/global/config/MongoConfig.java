package kr.co.mcplink.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing 
@EnableMongoRepositories(
    basePackages = {
            "kr.co.mcplink.domain.mcpserver.repository",
            "kr.co.mcplink.domain.mcpserverv2.repository",
            "kr.co.mcplink.domain.schedule.repository",
            "kr.co.mcplink.global.util"
    }
)
public class MongoConfig {

    @Primary 
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory, MongoMappingContext context) {

        MappingMongoConverter converter = new MappingMongoConverter(
            new DefaultDbRefResolver(mongoDatabaseFactory), context);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));

		return new MongoTemplate(mongoDatabaseFactory, converter);
    }

    @Primary 
    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTransactionManager(mongoDatabaseFactory);
    }
}
