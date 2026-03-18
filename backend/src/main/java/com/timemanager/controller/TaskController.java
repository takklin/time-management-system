package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.entity.Task;
import com.timemanager.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    private Long currentUserId() { return com.timemanager.util.UserUtil.getCurrentUserId(); }

    @GetMapping
    public Result<List<Task>> list() {
        return Result.success(taskService.list(currentUserId()));
    }

    @GetMapping("/{id}")
    public Result<Task> get(@PathVariable Long id) {
        return Result.success(taskService.getById(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody Task task) {
        task.setUserId(currentUserId());
        taskService.create(task);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id);
        taskService.update(task);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        taskService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    public Result<?> complete(@PathVariable Long id, @RequestParam(required=false) Integer actualMinutes) {
        taskService.complete(id, actualMinutes);
        return Result.success();
    }
}
