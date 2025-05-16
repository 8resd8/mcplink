package kr.co.mcplink.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
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
@EnableMongoAuditing 
@EnableMongoRepositories(
    basePackages = {
            "kr.co.mcplink.domain.mcpserver.v1.repository",
            "kr.co.mcplink.domain.mcpserver.v2.repository",
            "kr.co.mcplink.domain.mcpserver.v3.repository",
            "kr.co.mcplink.domain.schedule.v2.repository",
            "kr.co.mcplink.domain.schedule.v3.repository"
    }
)
public class MongoConfig {

    @Primary 
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory, MongoMappingContext context) {

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
    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTransactionManager(mongoDatabaseFactory);
    }
}
