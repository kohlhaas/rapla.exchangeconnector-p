/**
 *
 */
package org.rapla.plugin.exchangeconnector.server;


import org.rapla.facade.AllocationChangeEvent;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.ModificationEvent;

/**
 * @author lutz
 */
public class ChangeHandler extends SynchronisationHandler {

    /*/private AllocationChangeEvent[] changeEvents;
   // private Reservation changedReservation;
    private ModificationEvent modificationEvent;

    /**
     * @param changeEvents
     */


    public ChangeHandler(AllocationChangeEvent[] changeEvents, ClientFacade clientFacade) {
        super(clientFacade);
     //   this.changeEvents = changeEvents;
       // changedReservation = changeEvents[0].getNewReservation();

    }

    public ChangeHandler(ModificationEvent evt, ClientFacade clientFacade) {
        super(clientFacade);

        /*this.modificationEvent  = evt;
        for (RaplaObject raplaObject : evt.getChanged()) {
            if (raplaObject instanceof Reservation)
            {
                changedReservation = (Reservation) raplaObject;
                break;
            }
        }
*/
    }

    public void run() {

   /*     final DynamicType type = changedReservation.getClassification().getType();

        try {
            final DynamicType importEventType = ExchangeConnectorPlugin.getImportEventType(clientFacade);
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
                    changedReservation.getOwner()).getEntryAsString(ExchangeConnectorPlugin.EXPORT_EVENT_TYPE_KEY);
            if (exportableTypes == null) {
                SynchronisationManager.logInfo("Skipping event of type " + type + " because filter is not defined");
                return;
            }
            if (exportableTypes != null && !exportableTypes.contains(type.getElementKey())) {
                SynchronisationManager.logInfo("Skipping event of type " + type + " because filtered out by user");
                return;
            }
        } catch (RaplaException e) {
        }*/



       /* if (ExchangeConnectorPlugin.USE_JMS && SynchronisationManager.getInstance().getMessageServer() != null) {
            try {
                runJMS();
            } catch (Exception e) {
                SynchronisationManager.logException(e);
                runLocal();
            }
        } else {
            runLocal();
        }*/
        runLocal();
    }

/*
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
*/

    private void runLocal() {
        /*try {
            if (modificationEvent != null)
            {
                for (RaplaObject raplaObject : modificationEvent.getChanged()) {
                    if (raplaObject instanceof Appointment) {
                        final Appointment newAppointment = (Appointment) raplaObject;
                        ExchangeConnectorUtils.synchronizeAppointmentRequest(
                                clientFacade,
                                null,
                                ExchangeConnectorUtils.getAppointmentSID(newAppointment));
                    }
                }
            }else if (changeEvents != null)
                for (AllocationChangeEvent changeEvent : changeEvents) {
                    final Appointment newAppointment = changeEvent.getNewAppointment();
                    ExchangeConnectorUtils.synchronizeAppointmentRequest(
                            clientFacade,
                            changeEvent.getType(),
                            ExchangeConnectorUtils.getAppointmentSID(newAppointment));
                }
        } catch (Exception e) {
            SynchronisationManager.logException(e);
        }*/
    }

}
