package com.timemanager.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String email; // either username or email
    private String password;
}
