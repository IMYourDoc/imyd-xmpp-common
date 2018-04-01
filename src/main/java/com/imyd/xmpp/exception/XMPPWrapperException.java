package com.imyd.xmpp.exception;

/**
 * @author javi.more.garc
 *
 */
public class XMPPWrapperException extends RuntimeException {

    private static final long serialVersionUID = -2970926542186446391L;

    public XMPPWrapperException() {
        // default constructor
    }

    public XMPPWrapperException(String message, Throwable cause, boolean enableSuppression,
                                boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public XMPPWrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public XMPPWrapperException(String message) {
        super(message);
    }

    public XMPPWrapperException(Throwable cause) {
        super(cause);
    }

}
