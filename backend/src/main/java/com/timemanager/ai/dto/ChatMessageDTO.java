package com.timemanager.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 聊天消息 DTO
 * 用于保存对话历史
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    /** 消息角色 (user / assistant) */
    private String role;
    
    /** 消息内容 */
    private String content;
    
    /** 时间戳 */
    private LocalDateTime timestamp;
    
    /** 意图（如果是 assistant 消息，记录识别到的意图） */
    private String intent;
    
    public ChatMessageDTO(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
}
