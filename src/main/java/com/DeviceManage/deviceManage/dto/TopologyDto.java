package com.DeviceManage.deviceManage.dto;

import java.util.List;

/** Данни за картата на мрежата: възли (устройства) и ребра (връзки). */
public record TopologyDto(List<Node> nodes, List<Edge> edges) {

    public record Node(Long id, String label, String status, String type) {
    }

    public record Edge(Long id, Long from, Long to, String status, String label) {
    }
}
