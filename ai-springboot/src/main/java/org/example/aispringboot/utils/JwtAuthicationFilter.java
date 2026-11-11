package org.example.aispringboot.utils;

import ch.qos.logback.core.util.StringUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.config.JwtConfig;
import org.example.aispringboot.config.SecurityConfig;
import org.example.aispringboot.enumClass.UserStatus;
import org.example.aispringboot.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtAuthicationFilter extends OncePerRequestFilter {
    @Resource
    private UserService userService;

    // 公开路径的处理逻辑
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        String requestUri = request.getRequestURI();
        return SecurityConfig.isPublicPath(requestUri);
    }

    // 不在公开路径下，就会执行这个方法
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        // 获取请求的uri和方法
        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();
        System.out.println("请求URI: " + requestUri);
        System.out.println("请求方法: " + requestMethod);

        // 1、提取 Jwt token
        String token = JwtTokenUtil.extractTokenFromRequest(request);
        if (StringUtils.hasText(token)){
            // 2、验证token并获取用户信息
            JwtTokenUtil.TokenVerificationResult validationResult = JwtTokenUtil.validateToken(token);
            if (validationResult != null && validationResult.isValid()) {
                // 3、查询用户信息验证用户状态
                UserLoginResponseDTO.UserDetailResponseDTO user = userService.getUserById(validationResult.getUserId());
                System.out.println(JSONUtil.parseObj(user));
                if (user != null && UserStatus.NORMAL.getCode().equals(user.getStatus())){
                    // 4、创建spring security认证对象
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + validationResult.getRoleType())
                    );

                    // 5、创建UsernamePasswordAuthenticationToken对象
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            validationResult.getUserName(),  // 用户名作为主体
                            null,  // 密码为空
                            authorities  // 权限列表
                    );

                    // 6、设置认证信息到spring security上下文
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    // 7、将token存储到请求的属性中
                    request.setAttribute("jwtToken", token);

                } else {
                    clearSecurityContext();
                    ResponseUtil.WriteError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
                }
            } else {
                // 清理上下文
                clearSecurityContext();
                ResponseUtil.WriteError(response, ResultCode.TOKEN_INVALID);
            }

        } else {
            // 清理上下文
            clearSecurityContext();
            ResponseUtil.WriteError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }
        // 继续过滤器链
        chain.doFilter(request, response);
    }
    // 清理Spring Security的上下文
    private void clearSecurityContext(){
        SecurityContextHolder.clearContext();
    }
}
