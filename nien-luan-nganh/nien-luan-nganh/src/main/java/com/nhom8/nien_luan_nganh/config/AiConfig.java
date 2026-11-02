package com.nhom8.nien_luan_nganh.config;

import java.beans.BeanProperty;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfig {
    /*
     * Khởi tạo dùng cho cho các hàm gọi AI
     */
    @Bean
    public RestClient aiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10)); // thời gian tối đa thiết lập kết nối
        factory.setReadTimeout(Duration.ofSeconds(20)); // thời gian tối đa để nhận dữ liệu

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

}
