package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.dto.LoginDTO;
import com.timemanager.service.AuthService;
import com.timemanager.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }
}
