package com.timemanager.service.impl;

import com.timemanager.entity.Schedule;
import com.timemanager.mapper.ScheduleMapper;
import com.timemanager.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Override
    public List<Schedule> list(Long userId, String startDate, String endDate) {
        return scheduleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Schedule>()
                        .eq("user_id", userId)
                        .between("start_time", startDate, endDate)
                        .eq("deleted", 0));
    }

    @Override
    public void create(Schedule schedule) {
        scheduleMapper.insert(schedule);
    }

    @Override
    public void update(Schedule schedule) {
        scheduleMapper.updateById(schedule);
    }

    @Override
    public void delete(Long id) {
        Schedule s = new Schedule();
        s.setId(id);
        s.setDeleted(1);
        scheduleMapper.updateById(s);
    }
}
