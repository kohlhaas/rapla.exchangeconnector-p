/**
 *
 */
package org.rapla.plugin.exchangeconnector;

import java.util.Date;

import org.rapla.entities.User;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.datastorage.ExchangeAppointmentStorage;

/**
 * @author lutz
 */
public class CompleteReconciliationHandler extends SynchronisationHandler {


    /**
     * @param clientFacade
     */
    public CompleteReconciliationHandler(ClientFacade clientFacade) {
        super(clientFacade);
    }

    public void run() {
        deleteExchangeItemsFromRapla();
        ExchangeAppointmentStorage.getInstance().setAllDeleted();
        try {
            deleteAll();
            uploadAll();
            downloadAll();
        } catch (Exception e) {
            SynchronisationManager.logException(e);
        }
    }

    /**
     * @throws InterruptedException
     */
    private void downloadAll() throws InterruptedException {
        Thread thread = new Thread(new ScheduledDownloadHandler(clientFacade));
        thread.start();
        while (thread.isAlive()) {
            this.wait(100);
        }
    }

    /**
     * @throws RaplaException
     * @throws Exception
     */
    private void uploadAll() throws RaplaException, Exception {

        DynamicType importEventType = ExchangeConnectorPlugin.getImportEventType(clientFacade);
        final Date from = ExchangeConnectorPlugin.getSynchingPeriodPast(new Date());
        final Date to = ExchangeConnectorPlugin.getSynchingPeriodFuture(new Date());

        for (User user : clientFacade.getUsers()) {
//			User user= clientFacade.getUser(raplaUsername);


            Reservation[] reservations = clientFacade.getReservations(user,
                    from, to, null);
            for (Reservation reservation : reservations) {
                // only upload those which weren't added by Exchange
                if (importEventType != null && !reservation.getClassification().getType().getElementKey().equals(importEventType.getElementKey())) {
                    for (Appointment appointment : reservation.getAppointments()) {
                        UploadRaplaAppointmentWorker worker = new UploadRaplaAppointmentWorker(clientFacade, appointment, null);
                        worker.perform();
                    }
                }
            }
        }
    }

    /**
     * @throws Exception
     */
    private void deleteAll() throws Exception {
        for (Appointment appointment : ExchangeAppointmentStorage.getInstance().getDeletedItems()) {
            String raplaUsername = ExchangeAppointmentStorage.getInstance().getRaplaUsername(appointment);
            DeleteRaplaAppointmentWorker worker = new DeleteRaplaAppointmentWorker(clientFacade, raplaUsername,
                    ExchangeConnectorUtils.getAppointmentSID(appointment));
            worker.perform();
        }
        ExchangeAppointmentStorage.getInstance().clearStorage();
    }
}
