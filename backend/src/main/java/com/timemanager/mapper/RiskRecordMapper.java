package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.RiskRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RiskRecordMapper extends BaseMapper<RiskRecord> {

    @Select("SELECT * FROM risk_record ORDER BY created_at DESC LIMIT #{limit}")
    List<RiskRecord> getRecentRiskRecords(int limit);
}
