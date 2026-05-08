const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    // 与测试文件中的 appUrl 一致
    baseUrl: 'http://localhost:5173',
    // 使用旧的 integration 目录里的测试用例
    specPattern: 'cypress/integration/**/*.js',
    supportFile: false,
    setupNodeEvents(on, config) {
      // 可用于注册插件或拦截事件，当前不需要
      return config;
    }
  }
});
