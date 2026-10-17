package com.DeviceManage.deviceManage.service;

import com.DeviceManage.deviceManage.dto.LinkForm;
import com.DeviceManage.deviceManage.dto.LinkView;
import com.DeviceManage.deviceManage.dto.TopologyDto;
import com.DeviceManage.deviceManage.model.Device;
import com.DeviceManage.deviceManage.model.DeviceStatus;
import com.DeviceManage.deviceManage.model.InterfaceSample;
import com.DeviceManage.deviceManage.model.Link;
import com.DeviceManage.deviceManage.repository.LinkRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Връзки между устройствата (топология) и тяхното състояние.
 * Състоянието не се пази, а се изчислява от последните данни за двата края:
 *  - DOWN, ако някое от устройствата или зададените интерфейси е DOWN;
 *  - UNKNOWN, ако някое устройство още не е проверено;
 *  - UP в останалите случаи.
 */
@Service
public class LinkService {

    private final LinkRepository linkRepository;
    private final DeviceService deviceService;

    public LinkService(LinkRepository linkRepository, DeviceService deviceService) {
        this.linkRepository = linkRepository;
        this.deviceService = deviceService;
    }

    public void addLink(LinkForm form) {
        if (form.getDeviceAId().equals(form.getDeviceBId())) {
            throw new IllegalArgumentException("Изберете две различни устройства");
        }
        Link link = new Link();
        link.setDeviceA(deviceService.findById(form.getDeviceAId()));
        link.setDeviceB(deviceService.findById(form.getDeviceBId()));
        link.setIfIndexA(form.getIfIndexA());
        link.setIfIndexB(form.getIfIndexB());
        link.setCapacityMbps(form.getCapacityMbps());
        link.setDescription(form.getDescription() == null || form.getDescription().isBlank()
                ? null : form.getDescription().trim());
        linkRepository.save(link);
    }

    public void deleteLink(Long id) {
        linkRepository.deleteById(id);
    }

    public List<LinkView> findAll() {
        // Последните данни за интерфейсите на всяко устройство (кеш за една заявка)
        Map<Long, Map<Integer, InterfaceSample>> interfaces = new HashMap<>();
        List<LinkView> result = new ArrayList<>();
        for (Link l : linkRepository.findAll()) {
            InterfaceSample ifA = findInterface(interfaces, l.getDeviceA(), l.getIfIndexA());
            InterfaceSample ifB = findInterface(interfaces, l.getDeviceB(), l.getIfIndexB());

            Double trafficMbps = traffic(ifA != null ? ifA : ifB);
            Double load = trafficMbps != null && l.getCapacityMbps() != null
                    ? Math.round(trafficMbps / l.getCapacityMbps() * 1000) / 10.0
                    : null;

            result.add(new LinkView(l.getId(),
                    l.getDeviceA().getId(), l.getDeviceA().getName(), interfaceLabel(l.getIfIndexA(), ifA),
                    l.getDeviceB().getId(), l.getDeviceB().getName(), interfaceLabel(l.getIfIndexB(), ifB),
                    l.getDescription(), status(l, ifA, ifB), trafficMbps, l.getCapacityMbps(), load));
        }
        return result;
    }

    public TopologyDto topology() {
        List<TopologyDto.Node> nodes = deviceService.findAll().stream()
                .map(d -> new TopologyDto.Node(d.getId(), d.getName(), d.getStatus().name(),
                        d.getType() == null ? null : d.getType().name()))
                .toList();
        List<TopologyDto.Edge> edges = findAll().stream()
                .map(v -> new TopologyDto.Edge(v.id(), v.deviceAId(), v.deviceBId(), v.status(),
                        v.loadPercent() != null ? v.loadPercent() + " %" : ""))
                .toList();
        return new TopologyDto(nodes, edges);
    }

    private String status(Link l, InterfaceSample ifA, InterfaceSample ifB) {
        DeviceStatus a = l.getDeviceA().getStatus();
        DeviceStatus b = l.getDeviceB().getStatus();
        if (a == DeviceStatus.DOWN || b == DeviceStatus.DOWN
                || (ifA != null && !ifA.isOperUp()) || (ifB != null && !ifB.isOperUp())) {
            return "DOWN";
        }
        if (a == DeviceStatus.UNKNOWN || b == DeviceStatus.UNKNOWN) {
            return "UNKNOWN";
        }
        return "UP";
    }

    /** По-голямата от входящата и изходящата скорост, в Mbit/s. */
    private static Double traffic(InterfaceSample s) {
        if (s == null || (s.getInBps() == null && s.getOutBps() == null)) {
            return null;
        }
        double max = Math.max(s.getInBps() == null ? 0 : s.getInBps(), s.getOutBps() == null ? 0 : s.getOutBps());
        return Math.round(max / 1_000_000 * 100) / 100.0;
    }

    private static String interfaceLabel(Integer ifIndex, InterfaceSample s) {
        if (ifIndex == null) {
            return null;
        }
        return s != null ? s.getName() + " (#" + ifIndex + ")" : "#" + ifIndex;
    }

    private InterfaceSample findInterface(Map<Long, Map<Integer, InterfaceSample>> cache, Device d, Integer ifIndex) {
        if (ifIndex == null) {
            return null;
        }
        Map<Integer, InterfaceSample> byIndex = cache.computeIfAbsent(d.getId(), id -> {
            Map<Integer, InterfaceSample> map = new HashMap<>();
            deviceService.findLatestInterfaces(id).forEach(s -> map.put(s.getIfIndex(), s));
            return map;
        });
        return byIndex.get(ifIndex);
    }
}
