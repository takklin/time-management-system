Time Manager Backend（时间管理系统后端）

这是一个 基于 Spring Boot 的个人时间管理系统后端项目。

该项目提供 RESTful API，用于支持时间管理系统的核心功能，例如用户认证、分类管理，以及后续扩展的任务管理、日程管理、时间记录和统计分析等模块。

项目结构

项目采用典型的 Spring Boot + 分层架构：

src/main/java/...
├── controller   // REST 接口控制层
├── service      // 业务逻辑层
├── mapper       // MyBatis-Plus 数据访问层
├── entity       // 数据库实体类
├── dto          // 请求数据对象（Data Transfer Object）
├── vo           // 响应数据对象（View Object）
├── config       // Spring / Security 配置
├── common       // 通用工具类和统一返回结果封装
└── util         // 工具类（例如 JWT 工具）

各层职责说明：

controller：接收 HTTP 请求并返回响应

service：处理业务逻辑

mapper：负责数据库操作（MyBatis-Plus）

entity：数据库表对应的 Java 对象

dto：前端请求参数封装

vo：返回给前端的数据结构

config：Spring 配置和安全配置

common：统一返回结构、通用工具

util：辅助工具类（例如 JWT 处理）

快速启动
1 配置数据库

修改配置文件：

src/main/resources/application.properties

填写你的 MySQL 用户名和密码。

示例：

spring.datasource.url=jdbc:mysql://localhost:3306/time_manager
spring.datasource.username=root
spring.datasource.password=123456
2 启动项目

方式一：使用 Maven 构建

mvn clean package

方式二：直接运行 Spring Boot 主类

TimeManagerApplication
3 API 地址

服务启动后，接口地址为：

http://localhost:8080/api/v1/
已包含的模块

当前项目包含基础模块：

用户认证模块（Auth）

分类管理模块（Category）

后续可以继续扩展以下模块：

任务管理（Task）

日程管理（Schedule）

时间记录（Time Record）

统计分析（Statistics）

这些模块可以按照当前项目结构继续开发。

数据库初始化
1 启动 MySQL

创建数据库：

CREATE DATABASE time_manager;
USE time_manager;
2 初始化表结构

执行项目根目录中的 SQL 文件：

database-schema.sql

该文件会创建项目所需的数据库表。

3 修改数据库配置

修改文件：

src/main/resources/application.properties

配置数据库连接信息。

构建与运行

进入后端目录：

cd backend

编译项目：

mvn clean package

运行 jar 包：

java -jar target/time-manager-backend-0.0.1-SNAPSHOT.jar

项目启动后访问：

http://localhost:8080
API 测试

可以使用以下工具测试接口：

Postman

curl

Apifox

Insomnia

1 获取 Token

先调用登录或注册接口：

/api/v1/auth/*

成功后会返回 JWT Token。

2 在请求头添加 Token

后续请求需要在 Header 中加入：

Authorization: Bearer <token>
3 curl 示例

创建分类：

curl -X POST http://localhost:8080/api/v1/categories \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"name":"学习"}'
数据库调试

如果接口数据异常，可以：

打开 MySQL

查看数据库

time_manager

确认数据是否正确写入。

测试

可以在以下目录添加 JUnit 测试类：

src/test/java

用于编写 接口测试或集成测试。