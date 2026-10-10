package com.DeviceManage.deviceManage;

import com.DeviceManage.deviceManage.snmp.SnmpClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DeviceManageApplication {

	@Bean
    CommandLineRunner snmpTest(SnmpClient snmpClient) {
		return args -> {
			String descr = snmpClient.get("127.0.0.1", 161, "nms_read", "1.3.6.1.2.1.1.1.0");
			String uptime = snmpClient.get("127.0.0.1", 161, "nms_read", "1.3.6.1.2.1.1.3.0");
			System.out.println("sysDescr:  " + descr);
			System.out.println("sysUpTime: " + uptime);
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(DeviceManageApplication.class, args);
	}

}
