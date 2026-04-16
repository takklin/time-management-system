package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("ai_conversation")
public class AiConversation {
    @TableId
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long userId;

    private String role; // "user" or "admin"
    private String conversationId;
    private String content;
    private Date createdAt;
}
