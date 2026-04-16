package com.timemanager.vo;

import com.timemanager.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private UserVO user;

    public static LoginVO fromUser(String token, User user) {
        return new LoginVO(token, UserVO.fromUser(user));
    }
}
