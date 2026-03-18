package com.timemanager.service;

import com.timemanager.entity.Task;
import java.util.List;

public interface TaskService {
    List<Task> list(Long userId);
    Task getById(Long id);
    void create(Task task);
    void update(Task task);
    void delete(Long id);
    void complete(Long id, Integer actualMinutes);
}
