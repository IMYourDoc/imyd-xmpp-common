package com.imyd.xmpp.listener;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;

/**
 * @author javi.more.garc
 *
 */
public class DefaultChatManagerListener implements ChatManagerListener {

    private final ChatMessageListener messageListener;

    public DefaultChatManagerListener(ChatMessageListener messageListener) {
        this.messageListener = messageListener;
    }

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        if (!createdLocally) {
            chat.addMessageListener(messageListener);
        }
    }

    public ChatMessageListener getMessageListener() {
        return messageListener;
    }

}
