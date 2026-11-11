package org.example.aispringboot.config;

import cn.hutool.core.text.AntPathMatcher;
import jakarta.servlet.DispatcherType;
import org.example.aispringboot.utils.JwtAuthicationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private static final String[] PUBLIC_PATHS = {
            "/",
            "/api/test",
            "/api/user/login",
            "/api/user/add",
            "/files/**"
    };

    public static boolean isPublicPath(String requestUri) {
        for (String publicPath : PUBLIC_PATHS){
            if (antPathMatcher.match(publicPath, requestUri)){
                return true;
            }
        }
        return false;
    }
    @Bean
    public JwtAuthicationFilter jwtAuthicationFilter() {
        return new JwtAuthicationFilter();
    }
    // bean对象，其他类中可以直接使用，自动注入过程
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用csrf保护（API服务一般不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 配置会话管理为无状态（JWT需要）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求的授权规则
                .authorizeHttpRequests(auth -> auth
                    // 异步分发（SSE流式）放行，避免流结束后二次鉴权报Access Denied
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                    // 公开的路径，无需登录即可访问
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // 其他的请求都需要认证
                        .anyRequest().authenticated()
                )
                 // 添加JWT认证过滤器
                .addFilterBefore(jwtAuthicationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
