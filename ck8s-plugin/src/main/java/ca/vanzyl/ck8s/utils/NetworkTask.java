package ca.vanzyl.ck8s.utils;

import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.stream.Collectors;

@Named("network")
@DryRunReady
public class NetworkTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(NetworkTask.class);

    public void dumpInterfaceInfo() throws Exception {
        var interfaces = NetworkInterface.getNetworkInterfaces();
        var buf = new StringBuilder();
        interfaces.asIterator().forEachRemaining(iface -> {
            var name = iface.getName();
            var displayName = iface.getDisplayName();
            var inetAddresses = iface.inetAddresses().map(InetAddress::getHostAddress).collect(Collectors.joining(", "));
            buf.append("""
                    \nInterface: %s
                        Display Name: %s
                        Addresses: %s
                    """.formatted(name, displayName, inetAddresses));
        });
        log.info(buf.toString());
    }

    public static void main(String[] args) throws Exception {
        new NetworkTask().dumpInterfaceInfo();
    }
}
