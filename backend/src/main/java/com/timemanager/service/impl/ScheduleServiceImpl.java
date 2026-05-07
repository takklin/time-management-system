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
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Schedule> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
            wrapper.between("start_time", startDate, endDate);
        }
        wrapper.eq("deleted", 0);
        return scheduleMapper.selectList(wrapper);
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

    @Override
    public Schedule getById(Long id) {
        return scheduleMapper.selectById(id);
    }
}
