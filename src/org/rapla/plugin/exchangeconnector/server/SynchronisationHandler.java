/**
 *
 */
package org.rapla.plugin.exchangeconnector.server;

import java.util.Date;
import java.util.HashSet;
import java.util.TimerTask;

import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;

/**
 * @author lutz
 */
public abstract class SynchronisationHandler extends TimerTask implements Runnable {
    protected ClientFacade clientFacade;

    /**
     * @param clientFacade
     */
    public SynchronisationHandler(ClientFacade clientFacade) {
        super();
        this.clientFacade = clientFacade;
    }

    protected synchronized void deleteExchangeItemsFromRapla(ClientFacade clientFacade) {
        HashSet<Reservation> reservations = new HashSet<Reservation>();
        for (Appointment appointment : ExchangeAppointmentStorage.getInstance().getExchangeItems()) {
            reservations.add(appointment.getReservation());
        }
        for (Reservation reservation : reservations) {
            try {
                clientFacade.remove(reservation);
            } catch (RaplaException e) {
                //shouldn't occur - but who knows...
            }
        }
        ExchangeAppointmentStorage.getInstance().removeExchangeItems();
        ExchangeAppointmentStorage.getInstance().save();
    }

    /**
     * Method to upload a reservation to the Exchange Server
     *
     * @param newReservation : {@link Reservation} which should be uploaded to the Exchange Server
     * @throws Exception
     */
    protected synchronized  void uploadReservation(ClientFacade clientFacade, Reservation newReservation) throws Exception {
/*
        newReservation.getClassification().setValue("dhbw-plugin-exchange-connector-fk", "uploaded");
        clientFacade.getOperator().storeAndRemove(new Entity[]{newReservation},new Entity[]{}, clientFacade.getUser());
*/
        // iterate over all appointments of this reservation
        for (Appointment tmpAppointment : newReservation.getAppointments()) {
            // upload each appointment to the Exchange Server
            if (tmpAppointment.getStart().after(ExchangeConnectorPlugin.getSynchingPeriodPast(new Date())) && tmpAppointment.getStart().before(ExchangeConnectorPlugin.getSynchingPeriodFuture(new Date()))) {
                uploadAppointment(clientFacade, ExchangeConnectorUtils.getAppointmentID(tmpAppointment));
            }
        }
    }

    /**
     * @param appointmentId
     * @throws Exception
     */
    protected synchronized  void uploadAppointment(ClientFacade clientFacade, int appointmentId) throws Exception {

        Appointment appointment = ExchangeConnectorUtils.getAppointmentById(appointmentId, clientFacade);

        if (appointment != null) {
            uploadAppointment(clientFacade, appointment);
        }
    }

    /**
     * Method to upload an appointment to the exchange server
     *
     * @param appointment : {@link Appointment} to be uploaded to the Exchange Server
     * @throws Exception
     */
    protected synchronized void uploadAppointment(ClientFacade clientFacade, Appointment appointment) throws Exception {
        // exchange items are appointments which have been downloaded from the exchange server to rapla
        // thus they will not be synchronized back to the exchange server and are ignored
        if (!ExchangeAppointmentStorage.getInstance().isExchangeItem(appointment)) {
            UploadRaplaAppointmentWorker worker = new UploadRaplaAppointmentWorker(clientFacade, appointment, null);
            worker.perform();
        }
    }

}
