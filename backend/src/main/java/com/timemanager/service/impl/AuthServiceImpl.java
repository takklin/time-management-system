package com.timemanager.service.impl;

import com.timemanager.dto.LoginDTO;
import com.timemanager.mapper.UserMapper;
import com.timemanager.service.AuthService;
import com.timemanager.vo.LoginVO;
import com.timemanager.entity.User;
import com.timemanager.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = null;
        if (dto.getEmail() != null) {
            user = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                            .eq("email", dto.getEmail()));
        } else if (dto.getUsername() != null) {
            user = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                            .eq("username", dto.getUsername()));
        }
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        String token = JwtUtil.generateToken(user.getId());
        return new LoginVO(token, user);
    }
}
