package com.timemanager.service.impl;

import com.timemanager.entity.TimeRecord;
import com.timemanager.mapper.TimeRecordMapper;
import com.timemanager.service.TimeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimeRecordServiceImpl implements TimeRecordService {

    @Autowired
    private TimeRecordMapper timeRecordMapper;

    @Override
    public List<TimeRecord> list(Long userId, String startDate, String endDate) {
        return timeRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TimeRecord>()
                        .eq("user_id", userId)
                        .between("record_date", startDate, endDate)
                        .eq("deleted", 0));
    }

    @Override
    public void create(TimeRecord record) {
        timeRecordMapper.insert(record);
    }

    @Override
    public void startTimer(Long userId) {
        TimeRecord rec = new TimeRecord();
        rec.setUserId(userId);
        rec.setStartTime(LocalDateTime.now());
        rec.setRecordDate(LocalDate.now());
        timeRecordMapper.insert(rec);
    }

    @Override
    public void stopTimer(Long recordId, Long taskId, String note) {
        TimeRecord rec = new TimeRecord();
        rec.setId(recordId);
        rec.setTaskId(taskId);
        rec.setNote(note);
        LocalDateTime now = LocalDateTime.now();
        rec.setEndTime(now);
        // calculate duration
        TimeRecord existing = timeRecordMapper.selectById(recordId);
        if (existing != null && existing.getStartTime() != null) {
            long minutes = java.time.Duration.between(existing.getStartTime(), now).toMinutes();
            rec.setDurationMinutes((int) minutes);
        }
        timeRecordMapper.updateById(rec);
    }

    @Override
    public void delete(Long id) {
        TimeRecord t = new TimeRecord();
        t.setId(id);
        t.setDeleted(1);
        timeRecordMapper.updateById(t);
    }
}
