package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("risk_record")
public class RiskRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 风险类型，例如 LOGIN_BURST, BATCH_DELETE, PRIVILEGE_ESCALATION
    private String riskType;

    // 风险评分 0-100
    private Integer score;

    // 描述
    private String description;

    // 关联的日志 ID 列表（JSON 或逗号分隔）
    private String relatedLogIds;

    // 关联用户名
    private String relatedUsername;

    // 关联 IP
    private String relatedIp;

    // 状态：0 新建，1 已处理
    private Integer status;

    // 采取的动作或备注
    private String actionTaken;

    private LocalDateTime createdAt;
}
