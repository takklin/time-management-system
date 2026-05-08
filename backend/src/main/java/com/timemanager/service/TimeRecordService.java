package com.timemanager.service;

import com.timemanager.entity.TimeRecord;
import java.util.List;

public interface TimeRecordService {
    List<TimeRecord> list(Long userId, String startDate, String endDate);
    void create(TimeRecord record);
    Long startTimer(Long userId);
    void stopTimer(Long recordId, Long taskId, String note);
    void delete(Long id);
    TimeRecord getById(Long id);
}
