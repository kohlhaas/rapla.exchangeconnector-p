package org.rapla.plugin.exchangeconnector.jms;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.facade.AllocationChangeEvent;
import org.rapla.facade.ClientFacade;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorUtils;
import org.rapla.plugin.exchangeconnector.SynchronisationManager;

/**
 * User: kuestermann
 * Date: 09.06.12
 * Time: 14:06
 */
public class ReservationMessageListener implements MessageListener {
    private ClientFacade clientFacade;

    public ReservationMessageListener(ClientFacade clientFacade) {
        this.clientFacade = clientFacade;
    }

    public void onMessage(Message message) {
        if (message instanceof ObjectMessage) {
            ObjectMessage messageObject = (ObjectMessage) message;
            try {
                final Serializable object = messageObject.getObject();

                if (object instanceof MessageContainer) {
                    final MessageContainer messageContainer = (MessageContainer) object;
                    final SimpleIdentifier identifier = (SimpleIdentifier) messageContainer.getObject();
                    final AllocationChangeEvent.Type changeEvent = messageContainer.getTypeForString();

                    ExchangeConnectorUtils.synchronizeAppointmentRequest(
                            clientFacade,
                            changeEvent,
                            identifier
                    );


                }
            } catch (JMSException e) {
                SynchronisationManager.logException(e);
            } catch (Exception e) {
                SynchronisationManager.logException(e);
            }

        }
    }
}
