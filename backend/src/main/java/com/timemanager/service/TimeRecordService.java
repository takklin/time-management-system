package com.timemanager.service;

import com.timemanager.entity.TimeRecord;
import java.util.List;

public interface TimeRecordService {
    List<TimeRecord> list(Long userId, String startDate, String endDate);
    void create(TimeRecord record);
    void startTimer(Long userId); // returns id in real impl
    void stopTimer(Long recordId, Long taskId, String note);
    void delete(Long id);
}
