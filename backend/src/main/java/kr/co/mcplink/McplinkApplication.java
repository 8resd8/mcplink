package kr.co.mcplink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import kr.co.mcplink.global.config.JwtProperties;
import kr.co.mcplink.global.config.SsafyOauthProperties;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, SsafyOauthProperties.class})
public class McplinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(McplinkApplication.class, args);
	}

}
