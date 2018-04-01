package com.imyd.xmpp.base.util;

/**
 * @author sorrus.development@gmail.com
 */
public final class Constants {

    //
    // date settings

    public static final String TIMESTAMP_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z";
    public static final String TIMEZONE = "UTC";

    public static final String FULL_AUTH_REQUIRED = "Full authentication is required to access this resource: you can connect by sending a POST request to /login with username and password parameters";

    public static final String WEB_SOCKET_REPLY_DESTINATION = "/reply";

    //
    // elements and namespaces for extensions

    public static final String NAMESPACE_CHAT_STATES = "http://jabber.org/protocol/chatstates";

    public static final String ELEMENET_RECEIVED_CHAT_RECEIPTS = "received";

    public static final String NAMESPACE_CHAT_RECEIPTS = "urn:xmpp:receipts";

    public static final String NAMESPACE_INVITATIONS = "jabber:x:conference";

    public static final String PROP_MESSAGE_VERSION = "message_version";
    public static final String PROP_MESSAGE_VERSION_VALUE = "2.0";

    //
    // default pageable settings

    public static final Integer DEFAULT_PAGE_NUMBER = 0;

    public static final Integer DEFAULT_PAGE_SIZE = 10;

    //
    // android push

    public static final int NUM_ANDROID_PUSH_RETRIES = 5;

    private Constants() {

    }

}
