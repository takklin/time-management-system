package com.timemanager.service;

import com.timemanager.dto.LoginDTO;
import com.timemanager.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO dto);
}
