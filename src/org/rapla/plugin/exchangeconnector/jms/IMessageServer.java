package org.rapla.plugin.exchangeconnector.jms;

import javax.jms.JMSException;
import javax.jms.Message;

import org.rapla.facade.AllocationChangeEvent;

/**
 * Created with IntelliJ IDEA.
 * User: kuestermann
 * Date: 09.06.12
 * Time: 15:06
 * To change this template use File | Settings | File Templates.
 */
public interface IMessageServer {

    public void sendObject(Message s) throws JMSException;

    Message createMessage(AllocationChangeEvent changeEvent) throws JMSException;
}
