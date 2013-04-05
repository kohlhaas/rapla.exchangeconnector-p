package org.rapla.plugin.exchangeconnector.jms;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;

import org.rapla.facade.AllocationChangeEvent;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorUtils;
import org.rapla.plugin.exchangeconnector.datastorage.ExchangeAppointmentStorage;

/**
* Created with IntelliJ IDEA.
* User: kuestermann
* Date: 09.06.12
* Time: 15:13
* To change this template use File | Settings | File Templates.
*/
class MessageServerImpl implements  IMessageServer{
    private Session session;

    MessageServerImpl() throws JMSException {
        init();
    }

    private void init() throws JMSException {
        QueueConnectionFactory connFactory = new com.sun.messaging.QueueConnectionFactory();
        javax.jms.Connection connection = connFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    public Session getSession() {
        return session;
    }

    public void sendObject(Message m) throws JMSException {
        final Queue queue = getSession().createQueue(MessageServerFactory.UPDATE_EXCHANGE);
        final MessageProducer producer = getSession().createProducer(queue);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer.send(m);
    }

    public Message createMessage(AllocationChangeEvent changeEvent) throws JMSException {

        return getSession().createObjectMessage(new MessageContainer(
                ExchangeConnectorUtils.getAppointmentSID(changeEvent.getNewAppointment()),
                changeEvent.getType().toString(),
                ExchangeAppointmentStorage.getInstance().getExchangeId(changeEvent.getNewAppointment())));
    }
}
