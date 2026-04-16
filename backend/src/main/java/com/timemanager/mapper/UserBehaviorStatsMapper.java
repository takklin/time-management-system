package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.UserBehaviorStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为统计Mapper
 */
@Mapper
public interface UserBehaviorStatsMapper extends BaseMapper<UserBehaviorStats> {
}
