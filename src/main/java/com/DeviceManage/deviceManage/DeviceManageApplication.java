package com.DeviceManage.deviceManage;

import com.DeviceManage.deviceManage.model.Device;
import com.DeviceManage.deviceManage.model.DeviceType;
import com.DeviceManage.deviceManage.repository.DeviceRepository;
import com.DeviceManage.deviceManage.snmp.SnmpClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DeviceManageApplication {

	@Bean
	CommandLineRunner initTestDevice(DeviceRepository deviceRepository) {
		return args -> {
			if (!deviceRepository.existsByIpAddress("127.0.0.1")) {
				Device laptop = new Device();
				laptop.setName("Лаптоп (тестово устройство)");
				laptop.setIpAddress("127.0.0.1");
				laptop.setType(DeviceType.OTHER);
				laptop.setReadCommunity("nms_read");
				deviceRepository.save(laptop);
			}
		};
	}
	public static void main(String[] args) {
		SpringApplication.run(DeviceManageApplication.class, args);
	}

}
