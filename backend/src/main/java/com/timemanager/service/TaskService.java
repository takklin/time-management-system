package com.timemanager.service;

import com.timemanager.entity.Task;
import java.util.List;

public interface TaskService {
    List<Task> list(Long userId);
    List<Task> listWithCategory(Long userId, Long categoryId);
    Task getById(Long id);
    void create(Task task);
    Task update(Task task);
    boolean delete(Long id);
    void complete(Long id, Integer actualMinutes);
    List<com.timemanager.entity.Task> listWithFilters(Long userId, Long categoryId, String priority, java.time.LocalDateTime start, java.time.LocalDateTime end, Boolean completed);
}
