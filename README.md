# 个人时间管理系统 (time-management-system)

项目简介
- 毕设题目：个人时间管理系统的设计与实现（Spring Boot + Vue）
- 目标：帮助个人用户规划时间、追踪任务、记录专注时长并可视化展示时间分配与完成率。

主要技术栈
- 后端：Spring Boot 2.7.x, MyBatis-Plus, Spring Security + JWT, MySQL 8.0
- 前端：Vue 3.x, Element Plus, Pinia, Vue Router, Axios, ECharts
- 构建/工具：Maven (后端), Vite (前端), Git, Postman

目录结构（建议���/ (repo root) ├─ backend/ # 后端 Spring Boot 项目 ├─ frontend/ # 前端 Vue 项目 ├─ database-schema.sql ├─ API-Interface-List.md ├─ README.md ├─ LICENSE └─ .gitignore

快速开始（本地开发）
1. 克隆仓库（或在本地新建项目文件夹并复制本仓库文件）
2. 后端
   - 进入 backend/，用 Maven 导入项目依赖
   - 配置 application.yml（数据库、JWT 秘钥等）
   - 创建 MySQL 数据库并执行 database-schema.sql
   - 启动 Spring Boot 应用
3. 前端
   - 进入 frontend/，运行 `npm install` 或 `pnpm install`
   - 修改前端配置（API base URL）
   - 运行 `npm run dev`
4. 联调与测试
   - 使用 Postman 或前端页面测试 API 与功能

后续工作
- 完善后端 Controller/Service/Mapper、单元测试
- 完善前端页面原型与交互
- 集成 Swagger / Knife4j 自动生成 API 文档

