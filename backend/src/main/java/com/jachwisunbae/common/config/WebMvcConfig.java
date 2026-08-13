package com.jachwisunbae.common.config;

import com.jachwisunbae.common.resolver.AuthenticatedMemberIdArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticatedMemberIdArgumentResolver authenticatedMemberIdArgumentResolver;

    public WebMvcConfig(final AuthenticatedMemberIdArgumentResolver authenticatedMemberIdArgumentResolver) {
        this.authenticatedMemberIdArgumentResolver = authenticatedMemberIdArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedMemberIdArgumentResolver);
    }
}
