package com.timemanager.service;

import com.timemanager.entity.Schedule;
import java.util.List;

public interface ScheduleService {
    List<Schedule> list(Long userId, String startDate, String endDate);
    void create(Schedule schedule);
    void update(Schedule schedule);
    void delete(Long id);
}
