package com.timemanager.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.OperationLog;

/**
 * 操作日志 Mapper（已改为MyBatis Plus）
 * 注意：OperationLog 使用 @TableName 注解，应使用 MyBatis Plus 而非 JPA
 */
public interface OperationLogRepository extends BaseMapper<OperationLog> {
}
