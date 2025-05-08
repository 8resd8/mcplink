package kr.co.mcplink.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
			.addServersItem(new Server().url("https://mcplink.co.kr/api").description("live server"))
			.addServersItem(new Server().url("http://localhost:8080/api").description("Local server"))
			.info(new Info()
				.title("MCP API")
				.version("v1")
				.description("MCP 서버 조회용 REST API"));
	}
}