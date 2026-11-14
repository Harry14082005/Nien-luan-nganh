package com.nhom8.nien_luan_nganh;

import com.nhom8.nien_luan_nganh.config.FileStorageConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Kích hoạt chạy tác vụ ngầm định kỳ (cho ReminderScheduler quét lịch hẹn hết
					// hạn)
public class NienLuanNganhApplication {

	public static void main(String[] args) {
		SpringApplication.run(NienLuanNganhApplication.class, args);
	}

}
