/**
 *
 */
package org.rapla.plugin.exchangeconnector;

import org.rapla.entities.domain.Appointment;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.AllocationChangeEvent;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaException;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.List;

/**
 * @author lutz
 */
public class ChangeHandler extends SynchronisationHandler {

    private AllocationChangeEvent[] changeEvents;

    /**
     * @param changeEvents
     */
    public ChangeHandler(AllocationChangeEvent[] changeEvents, ClientFacade clientFacade) {
        super(clientFacade);
        this.changeEvents = changeEvents;
    }

    public void run() {
        final DynamicType type = changeEvents[0].getNewReservation().getClassification().getType();

        try {
            final DynamicType importEventType = ExchangeConnectorPlugin.getImportEventType(clientFacade);
            SynchronisationManager.logInfo("Invoked change handler for " + changeEvents.length + " events");
            if (importEventType != null && type.getElementKey().equals(importEventType.getElementKey())) {
                SynchronisationManager.logInfo("Skipping event of type " + type + " because is type of item pulled from exchange");
                return;
            }
        } catch (RaplaException e) {
            SynchronisationManager.logException(e);
            return;
        }

        try {
            final String exportableTypes = clientFacade.getPreferences(
                    changeEvents[0].getNewReservation().getOwner()).getEntryAsString(ExchangeConnectorPlugin.EXPORT_EVENT_TYPE_KEY);
            if (exportableTypes == null) {
                SynchronisationManager.logInfo("Skipping event of type " + type + " because filter is not defined");
                return;
            }
            if (exportableTypes != null && !exportableTypes.contains(type.getElementKey())) {
                SynchronisationManager.logInfo("Skipping event of type " + type + " because filtered out by user");
                return;
            }
        } catch (RaplaException e) {
        }



        if (ExchangeConnectorPlugin.USE_JMS && SynchronisationManager.getInstance().getMessageServer() != null) {
            try {
                runJMS();
            } catch (Exception e) {
                SynchronisationManager.logException(e);
                runLocal();
            }
        } else {
            runLocal();
        }
    }

    private void runJMS() {
        for (AllocationChangeEvent changeEvent : changeEvents) {
            try {
                final Message m = SynchronisationManager.getInstance().getMessageServer().createMessage(changeEvent);
                SynchronisationManager.getInstance().getMessageServer().sendObject(m);
            } catch (JMSException e) {
                SynchronisationManager.logException(e);
            }
        }
    }

    private void runLocal() {
        try {
            for (AllocationChangeEvent changeEvent : changeEvents) {
                final Appointment newAppointment = changeEvent.getNewAppointment();
                ExchangeConnectorUtils.synchronizeAppointmentRequest(
                        clientFacade,
                        changeEvent.getType(),
                        ExchangeConnectorUtils.getAppointmentSID(newAppointment));
            }
        } catch (Exception e) {
            SynchronisationManager.logException(e);
        }
    }

}
