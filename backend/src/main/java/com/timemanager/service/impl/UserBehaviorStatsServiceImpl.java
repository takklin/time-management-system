package com.timemanager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timemanager.entity.UserBehaviorStats;
import com.timemanager.mapper.UserBehaviorStatsMapper;
import com.timemanager.service.UserBehaviorStatsService;
import org.springframework.stereotype.Service;

/**
 * 用户行为统计Service实现
 */
@Service
public class UserBehaviorStatsServiceImpl extends ServiceImpl<UserBehaviorStatsMapper, UserBehaviorStats> implements UserBehaviorStatsService {
}
