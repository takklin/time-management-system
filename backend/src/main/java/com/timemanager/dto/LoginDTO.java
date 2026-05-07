package com.timemanager.dto;

import lombok.Data;
import java.util.List;

@Data
public class LoginDTO {
    private String username;
    private String email; // either username or email
    private String password;
    // Optional categories to create for new user during registration
    private List<CategoryDTO> categories;
}
