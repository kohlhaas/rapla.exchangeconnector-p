/**
 * 
 */
package org.rapla.plugin.exchangeconnector.server;


import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAccountInformationStorage;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;
import org.rapla.plugin.exchangeconnector.server.worker.DownloadWorker;

import java.util.HashSet;
import java.util.TimerTask;

/**
 * @author lutz
 *
 */
public class ScheduledDownloadHandler extends TimerTask {

    private final RaplaContext context;
    private final ClientFacade clientFacade;
	/**
	 * @param clientFacade
	 */
	public ScheduledDownloadHandler(RaplaContext context,ClientFacade clientFacade) {
		super();
        this.context = context;
        this.clientFacade = clientFacade;

	}

	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		try {
			deleteExchangeItemsFromRapla();
			downloadExchangeAppointments();
		} catch (Exception e) {

		}
	}

    private synchronized void deleteExchangeItemsFromRapla() {
        HashSet<Reservation> reservations = new HashSet<Reservation>();
        for (Appointment appointment : ExchangeAppointmentStorage.getInstance().getExchangeItems()) {
            reservations.add(appointment.getReservation());
        }
        for (Reservation reservation : reservations) {
            try {
                clientFacade.remove(reservation);
            } catch (RaplaException e) {

            }
        }
        ExchangeAppointmentStorage.getInstance().removeExchangeItems();
        ExchangeAppointmentStorage.getInstance().save();
    }


    private void downloadExchangeAppointments() throws Exception {
		for (String raplaUsername: ExchangeAccountInformationStorage.getInstance().getAllRaplaUsernames()) {
			if (ExchangeAccountInformationStorage.getInstance().isDownloadFromExchange(raplaUsername)) {
					DownloadWorker appointmentWorker = new DownloadWorker(context, clientFacade.getUser(raplaUsername));
					appointmentWorker.perform();
			}
		}
	}

}
