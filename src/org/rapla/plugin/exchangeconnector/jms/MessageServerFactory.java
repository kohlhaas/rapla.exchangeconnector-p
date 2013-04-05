package org.rapla.plugin.exchangeconnector.jms;

import javax.jms.JMSException;

public class MessageServerFactory {
    static final String UPDATE_EXCHANGE = "rapla.exchangeconnector.queue.update_exchange";

    private static MessageServerImpl messageServerInstance;


    public static IMessageServer getMessageServer() throws JMSException {
        if (messageServerInstance == null) {
            messageServerInstance = new MessageServerImpl();
        }

        return messageServerInstance;
    }


    private static MessageReceiverImpl messageReceiverInstance;

    public static IMessageReceiver getMessageReveiver() throws JMSException
    {
        if (messageReceiverInstance == null) {
            messageReceiverInstance = new MessageReceiverImpl();


        }
        return messageReceiverInstance;

    }

}
