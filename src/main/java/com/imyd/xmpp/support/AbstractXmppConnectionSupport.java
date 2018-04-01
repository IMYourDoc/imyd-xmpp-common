package com.imyd.xmpp.support;

import com.google.common.cache.CacheBuilder;
import com.imyd.common.base.exception.NotFoundException;
import com.imyd.xmpp.custom.MessageEventProvider;
import com.imyd.xmpp.exception.XMPPWrapperException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.net.ssl.SSLSocketFactory;
import java.util.Map;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.jivesoftware.smack.ConnectionConfiguration.SecurityMode.ifpossible;

/**
 * @author javi.more.garc
 * @author sorrus.development@gmail.com
 */
public abstract class AbstractXmppConnectionSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractXmppConnectionSupport.class);

    protected static final String NO_XMPP_CONNECTION_FOUND = "No XMPP connection found";

    @Inject
    protected XmppConnectionConfiguration configuration;

    protected Map<String, XMPPTCPConnection> mapSessionIdAndConnection;

    protected XMPPTCPConnection testConnection;

    protected final MessageEventProvider messageEventProvider = new MessageEventProvider();

    @PostConstruct
    protected void postConstruct() {

        //
        // the entries of this map are removed when the session times out. This
        // is done by a HttpSessionListener invoking the method
        // disconnectAndDeleteFromMap.

        //
        // we're using a cache so that the entries are auto removed in the
        // unlikely event that there is an error when the listener makes the
        // clean up.

        this.mapSessionIdAndConnection = CacheBuilder.newBuilder() //
                .expireAfterAccess(7, DAYS) //
                .<String, XMPPTCPConnection>build().asMap();

        try {
            // create connection
            testConnection = create();

        } catch (Exception e) {
            throw new XMPPWrapperException("Unable to generate a xmpp test connection", e);
        }

    }

    /**
     * This methods returns the test connection for xmpp
     *
     * @return XMPPTCPConnection
     */
    public XMPPTCPConnection getTestConnection() {
        return testConnection;
    }

    /**
     * This method creates a new XMPP connection.
     * <p>
     * As a result of this method a new XMPP connection will be introduced in
     * the map of session ids and connections.
     * <p>
     * There is no need to synchronize this method in the sense that is called
     * once and the session id is unique.
     *
     * @param sessionId
     * @param username
     * @param password
     */
    public abstract void create(String sessionId, String username, String password);

    /**
     * Disconnect the existing connection associated to the session id.
     *
     * @param sessionId
     */
    public void disconnect(String sessionId) {

        XMPPTCPConnection connection = mapSessionIdAndConnection.get(sessionId);

        // it may happen that disconnect is called after the connection has been
        // cleaned from the map so we need to make sure it still exists
        if (connection == null) {
            return;
        }

        synchronized (connection) {

            try {

                connection.disconnect();
            } catch (Exception e) {
                throw new XMPPWrapperException(e);
            }
        }
    }

    /**
     * Disconnect the existing connection associated to the session id and
     * removes it from the map of connection.
     *
     * @param sessionId
     */
    public void disconnectAndDeleteFromMap(String sessionId) {

        XMPPTCPConnection connection = mapSessionIdAndConnection.get(sessionId);

        if (connection == null) {
            return;
        }

        synchronized (connection) {

            try {
                connection.disconnect();
            } catch (Exception e) {
                LOGGER.trace("Error disconnecting", e);
            }

            mapSessionIdAndConnection.remove(sessionId);
        }

    }

    /**
     * Get the connection associated to the session id
     *
     * @param sessionId
     * @return
     */
    public XMPPConnection getConnection(String sessionId) {

        XMPPTCPConnection connection = mapSessionIdAndConnection.get(sessionId);

        if (connection == null) {
            throw new NotFoundException(NO_XMPP_CONNECTION_FOUND);
        }

        // this should not be needed most of the time since smack reconnects
        // automatically but sometimes it may take some time

        if (!connection.isConnected()) {
            try {
                connection.connect();
            } catch (Exception e) {
                throw new XMPPWrapperException(e);
            }
        }

//        try {
//            if (!connection.isAuthenticated()) {
//                connection.loginAnonymously();
//            }
//        } catch (Exception e) {
//            throw new XMPPWrapperException(e);
//        }

        return connection;
    }

    //
    // private methods

    protected abstract XMPPTCPConnection create();

    protected abstract XMPPTCPConnection create(String username, String password);

    protected XMPPTCPConnection baseCreate(String username, String password, String resName) {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder() //
                .setHost(configuration.getHost()) //
                .setPort(configuration.getPort()) //
                .setResource(resName) //
                .setServiceName(configuration.getServiceName()) //
                .setSecurityMode(ifpossible)//
                .setSendPresence(true) //
                .setUsernameAndPassword(username, password) //
                .setSocketFactory(SSLSocketFactory.getDefault()) //
                .setDebuggerEnabled(true) //
                .build();

        ProviderManager.addExtensionProvider("x", "jabber:x:event", messageEventProvider);

        return new XMPPTCPConnection(config);
    }

    /**
     * Reconnect the existing connection associated to the session id. It
     * includes sending an available presence and rejoining all rooms that the
     * user is affiliated/ member to/ of
     *
     * @param sessionId
     */
    public abstract void reconnect(String sessionId);

}
