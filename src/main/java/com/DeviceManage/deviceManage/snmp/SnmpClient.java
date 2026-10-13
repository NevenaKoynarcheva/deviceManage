package com.DeviceManage.deviceManage.snmp;

import jakarta.annotation.PreDestroy;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SnmpClient {

    private final Snmp snmp;

    public SnmpClient() throws IOException {
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        transport.listen();
    }

    private CommunityTarget<Address> target(String ip, int port, String community) {
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(community));
        target.setAddress(GenericAddress.parse("udp:" + ip + "/" + port));
        target.setVersion(SnmpConstants.version2c);
        target.setRetries(1);
        target.setTimeout(2000);
        return target;
    }

    /** GetRequest за един OID. Връща null при timeout или ако OID-то не съществува. */
    public Variable getVariable(String ip, int port, String community, String oid) throws IOException {
        PDU pdu = new PDU();
        pdu.setType(PDU.GET);
        pdu.add(new VariableBinding(new OID(oid)));

        ResponseEvent<Address> event = snmp.get(pdu, target(ip, port, community));
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
        return vb.getVariable();
    }

    public String get(String ip, int port, String community, String oid) throws IOException {
        Variable v = getVariable(ip, port, community, oid);
        return v == null ? null : v.toString();
    }

    /**
     * Обхожда цял клон от OID дървото (SNMP walk с GetBulk).
     * Ключът в резултата е индексът след началния OID, например "1" или "10.1",
     * а стойността е прочетената променлива. Празен резултат = клонът липсва.
     */
    public Map<String, Variable> walk(String ip, int port, String community, String rootOid) throws IOException {
        OID root = new OID(rootOid);
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory(PDU.GETBULK));
        List<TreeEvent> events = treeUtils.getSubtree(target(ip, port, community), root);

        Map<String, Variable> result = new LinkedHashMap<>();
        for (TreeEvent event : events) {
            if (event == null) {
                continue;
            }
            if (event.isError()) {
                if (result.isEmpty()) {
                    throw new IOException("SNMP walk грешка: " + event.getErrorMessage());
                }
                break; // част от клона е прочетена - връщаме каквото имаме
            }
            VariableBinding[] vbs = event.getVariableBindings();
            if (vbs == null) {
                continue;
            }
            for (VariableBinding vb : vbs) {
                if (vb != null && vb.getOid().startsWith(root) && !vb.isException()) {
                    result.put(vb.getOid().getSuffix(root).toString(), vb.getVariable());
                }
            }
        }
        return result;
    }

    /**
     * SetRequest - записва нова стойност в устройството.
     * Хвърля IOException, ако няма отговор или устройството откаже промяната.
     */
    public void set(String ip, int port, String writeCommunity, String oid, Variable value) throws IOException {
        PDU pdu = new PDU();
        pdu.setType(PDU.SET);
        pdu.add(new VariableBinding(new OID(oid), value));

        ResponseEvent<Address> event = snmp.set(pdu, target(ip, port, writeCommunity));
        PDU response = event.getResponse();
        if (response == null) {
            throw new IOException("няма отговор (грешна write community или устройството не е достъпно)");
        }
        if (response.getErrorStatus() != PDU.noError) {
            throw new IOException("устройството отказа промяната: " + response.getErrorStatusText());
        }
    }

    @PreDestroy
    public void close() throws IOException {
        snmp.close();
    }
}
