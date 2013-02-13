package org.rapla.plugin.exchangeconnector.jms;

import javax.jms.JMSException;
import javax.jms.MessageListener;

/**
 * User: kuestermann
 * Date: 09.06.12
 * Time: 15:09
 */
public interface IMessageReceiver {

    void addMessageListener(MessageListener rsm) throws JMSException;
}
