package com.imyd.xmpp.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author javi.more.garc
 *
 */
@Component
public class XmppConnectionConfiguration {

    @Value("${application.openfire.server.xmpp.host}")
    private String host;

    @Value("${application.openfire.server.xmpp.port}")
    private int port;

    @Value("${application.openfire.server.xmpp.service.name}")
    private String serviceName;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getServiceName() {
        return serviceName;
    }

}