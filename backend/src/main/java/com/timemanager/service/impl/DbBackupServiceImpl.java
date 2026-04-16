package com.timemanager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timemanager.entity.DbBackup;
import com.timemanager.mapper.DbBackupMapper;
import com.timemanager.service.DbBackupService;
import org.springframework.stereotype.Service;

/**
 * 数据库备份Service实现
 */
@Service
public class DbBackupServiceImpl extends ServiceImpl<DbBackupMapper, DbBackup> implements DbBackupService {
}
