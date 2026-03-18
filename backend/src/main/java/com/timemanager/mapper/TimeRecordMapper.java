package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.TimeRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TimeRecordMapper extends BaseMapper<TimeRecord> {
}
