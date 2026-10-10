package com.DeviceManage.deviceManage.repository;

import com.DeviceManage.deviceManage.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByIpAddress(String ipAddress);
    boolean existsByIpAddress(String ipAddress);
}