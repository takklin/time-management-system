package com.timemanager.service.impl;

import com.timemanager.entity.Task;
import com.timemanager.mapper.TaskMapper;
import com.timemanager.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public List<Task> list(Long userId) {
        return taskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Task>()
                        .eq("user_id", userId)
                        .eq("deleted", 0));
    }

    @Override
    public Task getById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public void create(Task task) {
        taskMapper.insert(task);
    }

    @Override
    public void update(Task task) {
        taskMapper.updateById(task);
    }

    @Override
    public void delete(Long id) {
        Task t = new Task();
        t.setId(id);
        t.setDeleted(1);
        taskMapper.updateById(t);
    }

    @Override
    public void complete(Long id, Integer actualMinutes) {
        Task t = new Task();
        t.setId(id);
        t.setStatus(1);
        t.setActualMinutes(actualMinutes);
        t.setCompletedAt(java.time.LocalDateTime.now());
        taskMapper.updateById(t);
    }
}
