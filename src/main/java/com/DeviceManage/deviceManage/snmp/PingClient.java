package com.DeviceManage.deviceManage.snmp;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PingClient {

    // Хваща "time=12ms", "time<1ms", "време=12ms" и т.н. независимо от езика на Windows
    private static final Pattern TIME = Pattern.compile("[=<]\\s*(\\d+)\\s*ms", Pattern.CASE_INSENSITIVE);
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public record PingResult(int sent, int received, Double avgLatencyMs) {
        public double packetLossPercent() {
            return 100.0 * (sent - received) / sent;
        }
    }

    public PingResult ping(String ip, int count, int timeoutMs) {
        int received = 0;
        long totalMs = 0;
        for (int i = 0; i < count; i++) {
            Long ms = pingOnce(ip, timeoutMs);
            if (ms != null) {
                received++;
                totalMs += ms;
            }
        }
        Double avg = received > 0 ? (double) totalMs / received : null;
        return new PingResult(count, received, avg);
    }

    private Long pingOnce(String ip, int timeoutMs) {
        ProcessBuilder pb = WINDOWS
                ? new ProcessBuilder("ping", "-n", "1", "-w", String.valueOf(timeoutMs), ip)
                : new ProcessBuilder("ping", "-c", "1", "-W", String.valueOf(Math.max(1, timeoutMs / 1000)), ip);
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) out.append(line).append('\n');
            }
            p.waitFor();
            String text = out.toString();
            // "TTL=" присъства само при истински отговор, на всеки език
            if (!text.toUpperCase().contains("TTL=")) return null;
            Matcher m = TIME.matcher(text);
            return m.find() ? Long.parseLong(m.group(1)) : 0L;
        } catch (Exception e) {
            return null;
        }
    }
}