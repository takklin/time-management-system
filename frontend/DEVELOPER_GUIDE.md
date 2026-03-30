# 项目项目从前端设置开始配置

## 什么是 Vite?
Vite 是一个现代化的前端构建工具，提供极速的冷启动和高效的热模块更新。

## 什么是 Pinia?
Pinia 是 Vue 的轻量级状态管理库，比 Vuex 更简单直观。

## 什么是 Element Plus?
Element Plus 是基于 Vue 3 的企业级 UI 组件库，提供丰富的组件和主题定制功能。

## 开发流程
1. 修改代码
2. 保存文件，自动热更新
3. 打开浏览器 http://localhost:5173 查看效果
4. 完成后运行 npm run build 构建生产版本

## 关键文件说明

### vite.config.ts
Vite 的配置文件，定义了开发和生产环境的构建选项。

### tsconfig.json
TypeScript 的配置文件，定义了类型检查和编译选项。

### src/main.ts
项目的入口文件，初始化 Vue 应用、路由、状态管理等。

### src/router/index.ts
Vue Router 的路由配置，定义了所有的页面路由和权限控制。

### src/store/
Pinia store 的定义，管理应用的全局状态。

## 开发小贴士
- 使用 Ctrl+Shift+P (macOS 为 Cmd+Shift+P) 打开 VS Code 命令面板
- 安装 Vetur 或 Volar 插件获得更好的 Vue 支持
- 使用浏览器的 Vue DevTools 扩展程序进行调试
