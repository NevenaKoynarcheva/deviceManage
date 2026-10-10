package com.DeviceManage.deviceManage.snmp;

import jakarta.annotation.PreDestroy;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SnmpClient {

    private final Snmp snmp;

    public SnmpClient() throws IOException {
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        transport.listen();
    }

    public String get(String ip, int port, String community, String oid) throws IOException {
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(community));
        target.setAddress(GenericAddress.parse("udp:" + ip + "/" + port));
        target.setVersion(SnmpConstants.version2c);
        target.setRetries(1);
        target.setTimeout(2000);

        PDU pdu = new PDU();
        pdu.setType(PDU.GET);
        pdu.add(new VariableBinding(new OID(oid)));

        ResponseEvent<Address> event = snmp.get(pdu, target);
        PDU response = event.getResponse();

        if (response == null) {
            return null; // няма отговор (timeout)
        }
        if (response.getErrorStatus() != PDU.noError) {
            throw new IOException("SNMP грешка: " + response.getErrorStatusText());
        }
        VariableBinding vb = response.get(0);
        if (vb.isException()) {
            return null; // OID-то не съществува на това устройство
        }
        return vb.getVariable().toString();
    }

    @PreDestroy
    public void close() throws IOException {
        snmp.close();
    }
}
