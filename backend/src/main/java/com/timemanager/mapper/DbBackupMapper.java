package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.DbBackup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据库备份Mapper
 */
@Mapper
public interface DbBackupMapper extends BaseMapper<DbBackup> {
}
