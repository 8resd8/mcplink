package kr.co.mcplink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableJpaAuditing
public class McplinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(McplinkApplication.class, args);
	}

}
