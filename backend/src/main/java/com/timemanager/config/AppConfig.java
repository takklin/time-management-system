package com.timemanager.config;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;

/**
 * 应用配置
 */
@Configuration
public class AppConfig {
    
    /**
     * RestTemplate Bean 用于 HTTP 调用
     * 配置支持 HTTPS、连接池、超时等
     */
    @Bean
    public RestTemplate restTemplate() {
        try {
            // 创建信任所有证书的SSLContext（用于外部API调用）
            SSLContext sslContext = SSLContexts.createDefault();
            
            // 创建HttpClient，支持HTTPS
            HttpClient httpClient = HttpClientBuilder.create()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setMaxConnTotal(200)        // 最大连接数
                    .setMaxConnPerRoute(100)     // 每个路由的最大连接数
                    .build();
            
            // 使用HttpComponentsClientHttpRequestFactory
            HttpComponentsClientHttpRequestFactory factory = 
                    new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(30000);  // 连接超时 30秒
            factory.setReadTimeout(30000);     // 读取超时 30秒
            
            return new RestTemplate(factory);
        } catch (Exception e) {
            // 如果配置失败，返回简单的RestTemplate
            return new RestTemplate();
        }
    }
}
