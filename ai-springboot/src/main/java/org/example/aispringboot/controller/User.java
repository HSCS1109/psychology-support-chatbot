package org.example.aispringboot.controller;


import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispringboot.DTO.command.UserLoginCommandDTO;
import org.example.aispringboot.DTO.command.UserRegisterCommandDTO;
import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.service.UserService;
import org.example.aispringboot.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class User {
    @Resource
    private UserService userService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // 用户登录接口
    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO commandDTO){
        System.out.println(commandDTO.getUsername());
        System.out.println(commandDTO.getPassword());

        // 调用service层登录方法
        UserLoginResponseDTO result =  userService.login(commandDTO);
        return Result.ok(result);
    }

    // 用户注册接口
    @PostMapping("/add")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> register(@Valid @RequestBody UserRegisterCommandDTO commandDTO){
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.register(commandDTO);
        return Result.ok(result);
    }

    // 获取当前用户接口
    @GetMapping("/current")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> getCurrentUser(){
        // 如何从token中解析用户id
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        // 调用service层获取用户详情
        UserLoginResponseDTO.UserDetailResponseDTO user =  userService.getUserById(userId);
        return Result.ok(user);
    }
}
