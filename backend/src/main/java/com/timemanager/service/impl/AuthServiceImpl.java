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
        boolean passwordMatch;
        String storedPassword = user.getPassword();
        if (storedPassword != null) {
            try {
                passwordMatch = BCrypt.checkpw(dto.getPassword(), storedPassword);
            } catch (Exception ex) {
                // 如果不是标准 bcrypt 哈希，则回退到明文比较以兼容历史数据
                passwordMatch = storedPassword.equals(dto.getPassword());
                if (passwordMatch) {
                    String encoded = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(dto.getPassword());
                    user.setPassword(encoded);
                    userMapper.updateById(user);
                }
            }
        } else {
            passwordMatch = false;
        }
        if (!passwordMatch) {
            throw new RuntimeException("密码错误");
        }
        String token = JwtUtil.generateToken(user.getId());
        return LoginVO.fromUser(token, user);
    }
}
