package org.example.aispringboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


// 进行jwt配置--->下一步就是通过这些配置生成jwt token
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;
    private long expiration;
    private long refreshExpiration;
    private String header;
    private String tokenPrefix;
}
