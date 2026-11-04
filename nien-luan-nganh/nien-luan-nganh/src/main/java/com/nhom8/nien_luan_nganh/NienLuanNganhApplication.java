package com.nhom8.nien_luan_nganh;

import com.nhom8.nien_luan_nganh.config.FileStorageConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class NienLuanNganhApplication {

	public static void main(String[] args) {
		SpringApplication.run(NienLuanNganhApplication.class, args);
	}

}
