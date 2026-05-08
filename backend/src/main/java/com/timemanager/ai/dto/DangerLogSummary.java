package com.timemanager.ai.dto;

import com.timemanager.entity.OperationLog;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DangerLogSummary {
    private String summary;          // 自然语言描述
    private List<OperationLog> logs; // 原始日志列表（可选）
}
