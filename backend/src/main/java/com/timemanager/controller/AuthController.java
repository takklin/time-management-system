package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.dto.LoginDTO;
import com.timemanager.service.AuthService;
import com.timemanager.service.OperationLogService;
import com.timemanager.vo.LoginVO;
import com.timemanager.entity.User;
import com.timemanager.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OperationLogService operationLogService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto, HttpServletRequest request) {
        try {
            LoginVO loginVO = authService.login(dto);
            
            // 获取请求 IP 和 User-Agent
            String ip = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            
            // 获取登录用户名
            String username = dto.getUsername() != null ? dto.getUsername() : dto.getEmail();
            
            // 记录登录成功
            operationLogService.recordOperation(
                username,
                "LOGIN",
                "User:" + loginVO.getUser().getId(),
                "SUCCESS"
            );
            
            return Result.success(loginVO);
        } catch (Exception e) {
            // 获取请求 IP 和 User-Agent
            String ip = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String username = dto.getUsername() != null ? dto.getUsername() : (dto.getEmail() != null ? dto.getEmail() : "unknown");
            
            // 记录登录失败
            operationLogService.recordOperation(
                username,
                "LOGIN",
                "User:unknown",
                "FAILURE: " + e.getMessage()
            );
            
            throw e;
        }
    }

    /**
     * 获取客户端真实 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO dto) {
        Long userId = com.timemanager.util.UserUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            return Result.error(400, "旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }

    @PostMapping("/avatar")
    public Result<User> uploadAvatar(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        Long userId = com.timemanager.util.UserUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }

        if (file == null || file.isEmpty()) {
            return Result.error(400, "No file uploaded");
        }

        try {
            String uploadsDir = "uploads"
;            java.nio.file.Path uploadsPath = java.nio.file.Paths.get(uploadsDir);
            if (!java.nio.file.Files.exists(uploadsPath)) {
                java.nio.file.Files.createDirectories(uploadsPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String newFileName = java.util.UUID.randomUUID().toString() + extension;
            java.nio.file.Path filePath = uploadsPath.resolve(newFileName);
            java.nio.file.Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error(404, "User not found");
            }
            // 将 avatar 设为可通过后端接口获取的路径，前端可通过 /api 代理访问
            user.setAvatar("/api/v1/auth/avatar/" + newFileName);
            user.setUpdatedAt(java.time.LocalDateTime.now());
            userMapper.updateById(user);
            user.setPassword(null);
            return Result.success(user);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "Upload failed");
        }
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody UpdateProfileDTO dto) {
        Long userId = com.timemanager.util.UserUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody LoginDTO dto) {
        // Validate input
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return Result.error(400, "用户名不能为空");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            return Result.error(400, "邮箱不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return Result.error(400, "密码不能为空");
        }

        // Check if user already exists
        User existingUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", dto.getUsername())
                        .or()
                        .eq("email", dto.getEmail()));
        if (existingUser != null) {
            return Result.error(400, "用户名或邮箱已存在");
        }

        // Create new user
        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        newUser.setEmail(dto.getEmail());
        // 默认普通用户；如果用户名是 admin 则自动赋 admin 角色（可根据业务改成配置或管理员审批）
        newUser.setRole("user");
        if ("admin".equalsIgnoreCase(dto.getUsername())) {
            newUser.setRole("admin");
        }
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(newUser);

        // Auto login after registration
        return Result.success(authService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    @GetMapping("/user")
    public Result<User> currentUser() {
        Long userId = com.timemanager.util.UserUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @GetMapping("/me")
    public Result<User> me() {
        return currentUser();
    }

    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<?> serveAvatar(@PathVariable String filename) {
        try {
            Path uploadsPath = Paths.get("uploads").toAbsolutePath().normalize();
            Path filePath = uploadsPath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            // 如果文件存在且可读，返回文件
            if (resource.exists() && resource.isReadable()) {
                String contentType = "image/jpeg";
                if (filename.toLowerCase().endsWith("png")) {
                    contentType = "image/png";
                } else if (filename.toLowerCase().endsWith("gif")) {
                    contentType = "image/gif";
                } else if (filename.toLowerCase().endsWith("webp")) {
                    contentType = "image/webp";
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            }
            
            // 文件不存在时，返回生成的占位符图像
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                    200, 200, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = bufferedImage.createGraphics();
            
            // 使用灰色背景
            g2d.setColor(new java.awt.Color(200, 200, 200));
            g2d.fillRect(0, 0, 200, 200);
            
            // 绘制边界
            g2d.setColor(new java.awt.Color(150, 150, 150));
            g2d.setStroke(new java.awt.BasicStroke(2));
            g2d.drawRect(1, 1, 198, 198);
            
            // 绘制文字 "No Image"
            g2d.setColor(new java.awt.Color(100, 100, 100));
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
            java.awt.FontMetrics fm = g2d.getFontMetrics();
            String text = "No Image";
            int textX = (200 - fm.stringWidth(text)) / 2;
            int textY = (200 - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(text, textX, textY);
            
            g2d.dispose();
            
            // 编码为 PNG 格式
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "png", baos);
            byte[] imageData = baos.toByteArray();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .contentLength(imageData.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"placeholder.png\"")
                    .body(imageData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/test")
    public List<User> list() {
        return userMapper.selectList(null);
    }

    public static class ChangePasswordDTO {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public static class AvatarDTO {
        private String avatar;

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
    }

    public static class UpdateProfileDTO {
        private String email;
        private String nickname;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }
}

