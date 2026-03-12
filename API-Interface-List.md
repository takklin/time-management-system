# API 文档（初版） — /api/v1

统一响应：
```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
认证

    POST /api/v1/auth/register {username, email, password}
    POST /api/v1/auth/login {username/email, password} -> { token, user }
    POST /api/v1/auth/refresh (Header: Authorization)
    PUT /api/v1/auth/password {oldPassword, newPassword} (需认证)
    GET /api/v1/auth/me (需认证)

用户

    PUT /api/v1/users/profile {nickname, avatar, email} (需认证)
    GET /api/v1/users/{userId} (管理员)

分类

    GET /api/v1/categories
    POST /api/v1/categories {name, color, sort}
    PUT /api/v1/categories/{id}
    DELETE /api/v1/categories/{id}

任务

    GET /api/v1/tasks?categoryId=&priority=&status=&startDate=&endDate=&page=&size=
    GET /api/v1/tasks/{id}
    POST /api/v1/tasks {title, categoryId, priority, deadline, estimatedMinutes, description}
    PUT /api/v1/tasks/{id}
    DELETE /api/v1/tasks/{id} (逻辑删除)
    PUT /api/v1/tasks/{id}/complete {actualMinutes}

日程

    GET /api/v1/schedules?startDate=&endDate=
    POST /api/v1/schedules {title, taskId, startTime, endTime, remindBefore}
    PUT /api/v1/schedules/{id}
    DELETE /api/v1/schedules/{id}

时间记录

    GET /api/v1/time-records?taskId=&startDate=&endDate=&page=&size=
    POST /api/v1/time-records {taskId, startTime, endTime, durationMinutes, note, recordDate}
    POST /api/v1/time-records/start -> { recordId, startTime }
    PUT /api/v1/time-records/{id}/stop {taskId, note} -> 停止计时并更新时长
    DELETE /api/v1/time-records/{id}

统计

    GET /api/v1/stats/time-distribution?startDate=&endDate=
    GET /api/v1/stats/completion-trend?startDate=&endDate=&groupBy=day/week
    GET /api/v1/stats/daily-focus?startDate=&endDate=
    GET /api/v1/stats/estimate-vs-actual?startDate=&endDate=

（请在开发过程中使用 Swagger / Knife4j 进一步完善每个接口的请求/响应示例及参数说明）
