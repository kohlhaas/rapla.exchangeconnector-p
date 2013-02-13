package org.rapla.plugin.exchangeconnector.jms;

import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.facade.AllocationChangeEvent;

import javax.jms.JMSException;
import javax.jms.Message;
import java.io.Serializable;

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
