package com.timemanager.dto;

import com.timemanager.entity.Schedule;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Data
public class ScheduleDTO {
    private String title;
    private String startTime;
    private String endTime;
    private Long taskId;
    private Integer reminderTime;

    public Schedule toEntity() {
        Schedule s = new Schedule();
        s.setTitle(this.title);
        s.setTaskId(this.taskId);
        s.setRemindBefore(this.reminderTime);
        s.setStartTime(parseToLocalDateTime(this.startTime));
        s.setEndTime(parseToLocalDateTime(this.endTime));
        return s;
    }

    private LocalDateTime parseToLocalDateTime(String str) {
        if (str == null) return null;
        try {
            Instant inst = Instant.parse(str);
            return LocalDateTime.ofInstant(inst, ZoneId.systemDefault());
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(str);
            } catch (DateTimeParseException ex2) {
                return null;
            }
        }
    }
}
