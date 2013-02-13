package org.rapla.plugin.exchangeconnector.jms;

import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.facade.AllocationChangeEvent;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorUtils;
import org.rapla.plugin.exchangeconnector.datastorage.ExchangeAppointmentStorage;

import javax.jms.*;

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

    @Override
    public void sendObject(Message m) throws JMSException {
        final Queue queue = getSession().createQueue(MessageServerFactory.UPDATE_EXCHANGE);
        final MessageProducer producer = getSession().createProducer(queue);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer.send(m);
    }

    @Override
    public Message createMessage(AllocationChangeEvent changeEvent) throws JMSException {

        return getSession().createObjectMessage(new MessageContainer(
                ExchangeConnectorUtils.getAppointmentSID(changeEvent.getNewAppointment()),
                changeEvent.getType().toString(),
                ExchangeAppointmentStorage.getInstance().getExchangeId(changeEvent.getNewAppointment())));
    }
}
