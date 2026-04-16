package com.timemanager.monitor;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiStatMonitorConfig {
    @Bean
    public FilterRegistrationBean<ApiStatMonitorFilter> apiStatMonitorFilterRegistration(ApiStatMonitorFilter filter) {
        FilterRegistrationBean<ApiStatMonitorFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setName("apiStatMonitorFilter");
        registration.setOrder(1);
        return registration;
    }
}
