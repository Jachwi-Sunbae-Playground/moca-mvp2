package com.jachwisunbae.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jachwiSunbaeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("자취 선배 API")
                .version("v1")
                .description("후보 매물과 체크리스트를 관리하는 API입니다."));
    }
}
