package org.example.aispringboot.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.DTO.command.UserLoginCommandDTO;
import org.example.aispringboot.DTO.command.UserRegisterCommandDTO;
import org.example.aispringboot.DTO.response.UserLoginResponseDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.enumClass.UserType;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.UserMapper;
import org.example.aispringboot.service.convert.UserConvert;
import org.example.aispringboot.utils.JwtTokenUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserLoginResponseDTO login(UserLoginCommandDTO commandDTO){
        //构建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername,commandDTO.getUsername())
                .or()
                .eq(User::getEmail,commandDTO.getUsername());
        // 调用MybatisPlus API查询用户
        User user = userMapper.selectOne(queryWrapper);
        System.out.println(user);

        // 判断用户是否存在
        if(user == null){
            throw new BusinessException("用户不存在");
        }

        // 判断密码是否正确
        String inputPassword = commandDTO.getPassword().trim();
        // 安全加密算法
        if (!passwordEncoder.matches(inputPassword,user.getPassword())){
            throw new BusinessException("密码错误");
        }

        // 检查用户状态是否正常
        if(!user.isActive()) {
            throw new BusinessException("用户已被禁用，请联系管理员。");
        }

        // 生成 JWT token
        String token = JwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        System.out.println(token);
        // 生成的结果返回到控制器中
        UserLoginResponseDTO.UserDetailResponseDTO userInfo = UserConvert.entityToDetailResponse(user);
        return UserConvert.entityToLoginResponse(token, userInfo);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO register(UserRegisterCommandDTO commandDTO){
        System.out.println(JSONUtil.parseObj(commandDTO));
        // 验证密码是否一致
        if (!commandDTO.getPassword().equals(commandDTO.getConfirmPassword())){
            throw new BusinessException("两次输入密码不一致");
        }
        // 验证用户名是否存在
        LambdaQueryWrapper<User> UserNameQuery = new LambdaQueryWrapper<>();
        UserNameQuery.eq(User::getUsername,commandDTO.getUsername());
        if (userMapper.selectCount(UserNameQuery) > 0){
            throw new BusinessException("用户名已存在");
        }
        // 验证邮箱是否存在
        LambdaQueryWrapper<User> EmailQuery = new LambdaQueryWrapper<>();
        EmailQuery.eq(User::getEmail,commandDTO.getEmail());
        if (userMapper.selectCount(EmailQuery) > 0){
            throw new BusinessException("邮箱已存在");
        }

        // 判断用户类型是否为0、1
        if (!UserType.isValidCode(commandDTO.getUserType())){
            throw new BusinessException("无效的用户类型");
        }

        // 创建用户
        String Encodedpassword = passwordEncoder.encode(commandDTO.getPassword().trim());
        User user = UserConvert.registerCommandToEntity(commandDTO,Encodedpassword);

        // 插入数据库
        userMapper.insert(user);

        return UserConvert.entityToDetailResponse(user);
    }

    // 根据用户id查询用户信息
    public UserLoginResponseDTO.UserDetailResponseDTO getUserById(Long userId){
        User user = userMapper.selectById(userId);
        if(user == null){
            throw new BusinessException("用户不存在");
        }
        return UserConvert.entityToDetailResponse(user);
    }
}