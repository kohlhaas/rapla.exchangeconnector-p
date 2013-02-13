package org.rapla.plugin.exchangeconnector.jms;

import javax.jms.*;

/**
* Created with IntelliJ IDEA.
* User: kuestermann
* Date: 09.06.12
* Time: 15:13
* To change this template use File | Settings | File Templates.
*/
class MessageReceiverImpl implements IMessageReceiver{
    private Session session;

    MessageReceiverImpl() throws JMSException {
        init();
    }

    private void init() throws JMSException {
        QueueConnectionFactory connFactory = new com.sun.messaging.QueueConnectionFactory();
        javax.jms.Connection connection = connFactory.createConnection();

        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();
    }

    public Session getSession() {
        return session;
    }

    public void addMessageListener(MessageListener messageListener) throws JMSException {
        final Queue queue = getSession().createQueue(MessageServerFactory.UPDATE_EXCHANGE);

        final MessageConsumer consumer = getSession().createConsumer(queue);


        consumer.setMessageListener(messageListener);
    }
}
