package com.imyd.xmpp.service.impl;

import com.google.common.cache.CacheBuilder;
import com.imyd.common.base.utils.DateUtils;
import com.imyd.common.base.utils.JsonUtils;
import com.imyd.common.webchat.converter.IdConverter;
import com.imyd.openfire.model.message.content.OfMessageBodyContent;
import com.imyd.xmpp.exception.XMPPWrapperException;
import com.imyd.xmpp.listener.DefaultChatManagerListener;
import com.imyd.xmpp.support.AbstractXmppConnectionSupport;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.imyd.xmpp.base.util.Constants.PROP_MESSAGE_VERSION;
import static com.imyd.xmpp.base.util.Constants.PROP_MESSAGE_VERSION_VALUE;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author javi.more.garc
 * @author sorrus.development@gmail.com
 */
public abstract class AbstractChatServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractChatServiceImpl.class);

    @Inject
    protected AbstractXmppConnectionSupport xmppConnectionSupport;

    @Inject
    protected IdConverter idConverter;

    //
    // we're using a cache so that we don't need to create a new chat for every
    // message between two users

    protected final Map<String, Chat> mapChats = CacheBuilder.newBuilder() //
            .expireAfterAccess(15, MINUTES) //
            .<String, Chat>build().asMap();

    //
    // protected methods

    protected static OfMessageBodyContent newMessageBodyContent(String usernameBareJid, String toUsernameBareJid) {

        OfMessageBodyContent bodyContent = new OfMessageBodyContent();

        bodyContent.setMessageId(UUID.randomUUID().toString().toUpperCase());
        bodyContent.setTimestamp(DateUtils.convertDateToISOString(new java.util.Date()));

        bodyContent.setFrom(usernameBareJid);
        bodyContent.setTo(toUsernameBareJid);

        return bodyContent;
    }

    protected String sendMessageInternal(String sessionId, String fromUsername, String password, String toUsername, boolean oneToOne, OfMessageBodyContent content) {

        Message smackMessage = new Message();

        smackMessage.setType(oneToOne ? Type.chat : Type.groupchat);
        smackMessage.setStanzaId(content.getMessageId());

        smackMessage.setSubject(content.getSubject());
        smackMessage.setBody(JsonUtils.write(content));

        smackMessage.setFrom(content.getFrom());
        smackMessage.setTo(content.getTo());

        // add the version so that consumers understand JSON
        JivePropertiesManager.addProperty(smackMessage, PROP_MESSAGE_VERSION, PROP_MESSAGE_VERSION_VALUE);

        //Prepare XMPP connection for a given sessionId
        xmppConnectionSupport.create(sessionId, fromUsername, password);

        // get chat instance
        Object chat = oneToOne ? getChat(sessionId, fromUsername, toUsername) : getMultiUserChat(sessionId, fromUsername, toUsername);

        try {

            MethodUtils.invokeExactMethod(chat, "sendMessage", smackMessage);

        } catch (Exception e) {
            throw new XMPPWrapperException(e);
        } finally {
            //TODO:
//            if (oneToOne) {
//                ((Chat)chat).close();
//            } else {
//                try {
//                    ((MultiUserChat)chat).leave();
//                } catch (SmackException.NotConnectedException e) {
//                }
//            }
            xmppConnectionSupport.disconnectAndDeleteFromMap(sessionId);
        }

        LOGGER.debug("User with username '{}' sent the message with packet id '{}'", fromUsername,
                smackMessage.getStanzaId());

        return smackMessage.getStanzaId();
    }


    private Chat getChat(String sessionId, String fromUsername, String toUsername) {

        XMPPConnection connection = xmppConnectionSupport.getConnection(sessionId);

        ChatManager chatManager = ChatManager.getInstanceFor(connection);

        synchronized (chatManager) {

            String key = String.format("chat_%s_%s", fromUsername, toUsername);

            Chat chat = mapChats.get(key);

            // we check the thread id because we may have cached a chat from a
            // previous user session whose thread wouldn't work with the current
            // session
            if (chat != null && chatManager.getThreadChat(chat.getThreadID()) != null) {

                return chat;
            }

            // search for the chat manager listener we set up in the login
            Optional<ChatManagerListener> opChatManList = chatManager.getChatListeners().stream() //
                    .filter(chatList -> chatList instanceof DefaultChatManagerListener) //
                    .findFirst();

            if (opChatManList == null) {
                throw new IllegalArgumentException(
                        String.format("No message listener found for username '%s'", fromUsername));

            }

            DefaultChatManagerListener chatManagerListener = (DefaultChatManagerListener) opChatManList.get();

            // transform the recipient's username to jid
            String toJid = idConverter.fromUsernameToBareJid(toUsername);

            // create a new chat and leave as message listener the one we
            // introduced
            // when creating the chat manager listener after the from user
            // signed in
            chat = chatManager.createChat(toJid, chatManagerListener.getMessageListener());

            mapChats.put(key, chat);

            return chat;
        }

    }

    private MultiUserChat getMultiUserChat(String sessionId, String fromUsername, String toRoomName) {

        XMPPConnection connection = xmppConnectionSupport.getConnection(sessionId);

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(connection);

        return multiUserChatManager.getMultiUserChat(idConverter.fromRoomNameToBareJid(toRoomName));

    }

}
