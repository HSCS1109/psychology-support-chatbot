package org.example.aispringboot.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.example.aispringboot.config.JwtConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

@Component
public class JwtTokenUtil implements ApplicationContextAware {
    private static final String ISSUER = "mental-health-assistant";

    private static ApplicationContext applicationContext;

    // 用于在静态工具类中获取spring容器管理的bean
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        JwtTokenUtil.applicationContext = applicationContext;
    }

    private static JwtConfig getJwtConfig() {
        return applicationContext.getBean(JwtConfig.class);
    }

    // 生成token的方法
    public static String generateToken(Long userId, String userName, Integer roleType) {
        try {
            // 生成token的逻辑，获取jwt配置
            JwtConfig jwtConfig = getJwtConfig();
            // 生成签名的算法
            Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
            // 生成过期的时间
            Date expiration = new Date(System.currentTimeMillis() + jwtConfig.getExpiration());
            // 生成token
            String token = JWT.create()
                    .withClaim("userId", userId)
                    .withClaim("userName", userName)
                    .withClaim("roleType", roleType)
                    .withExpiresAt(expiration)   // 过期时间
                    .withIssuedAt(new Date()) // 设置签发时间
                    .withIssuer(ISSUER)  // 设置签发者
                    .sign(algorithm);  // 生成签名
            return token;
        } catch (Exception e) {
            throw new RuntimeException("生成token 失败:", e);
        }
    }

    // 提取token的方法
    public static String extractTokenFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String tokenHeader = request.getHeader("token");
        if (StringUtils.hasText(tokenHeader)) {
            return tokenHeader;
        }
        return null;
    }

    // 获取当前的token
    public static String getCurrentToken(){
        ServletRequestAttributes attribute = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if ( attribute != null){
            HttpServletRequest request = attribute.getRequest();
            String token = (String) request.getAttribute("jwtToken");
            if (token != null){
                return token;
            }

            // 从请求头直接获取
            String headerToken = extractTokenFromRequest(request);
            return headerToken;
        }
        return null;
    }

    // 验证token
    public static TokenVerificationResult validateToken(String token){
        // 获取解密后的jwt对象
        DecodedJWT decodedJWT = verifyToken(token);
        // 从jwt对象中获取用户id、用户名、角色类型
        Long userId = decodedJWT.getClaim("userId").asLong();
        String userName = decodedJWT.getClaim("userName").asString();

        // 角色类型
        Integer roleType = null;
        try{
            roleType = decodedJWT.getClaim("roleType").asInt();
        } catch (Exception e) {
            String roleTypeStr = decodedJWT.getClaim("roleType").asString();
            if (StringUtils.hasText(roleTypeStr)) {
                roleType = Integer.valueOf(roleTypeStr);
            }
        }
        if (userId != null && StringUtils.hasText(userName) && roleType != null) {
            return new TokenVerificationResult(userId, userName, roleType, true);
        }
        return null;
    }

    // 验证token的有效性
    public static DecodedJWT verifyToken(String token){
        if (!StringUtils.hasText(token)){
            throw new JWTVerificationException("token不能为空");
        }
        // token解码
        JwtConfig jwtConfig = getJwtConfig();
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        // 获取解密的对象
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        return verifier.verify(token);
    }


    // token验证结果封装类
    @Getter
    public static class TokenVerificationResult {
        private final Long userId;
        private final String userName;
        private final Integer roleType;
        private final boolean valid;

        public TokenVerificationResult(Long userId, String userName, Integer roleType, boolean valid) {
            this.userId = userId;
            this.userName = userName;
            this.roleType = roleType;
            this.valid = valid;
        }
    }
}