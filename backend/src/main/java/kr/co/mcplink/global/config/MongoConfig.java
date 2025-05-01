package kr.co.mcplink.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.host}")
    private String mongoHost;

    @Value("${spring.data.mongodb.port}")
    private int mongoPort;

    @Value("${spring.data.mongodb.database}")
    private String mongoDb;

    @Value("${spring.data.mongodb.username}")
    private String mongoUser;

    @Value("${spring.data.mongodb.password}")
    private String mongoPassword;
}
