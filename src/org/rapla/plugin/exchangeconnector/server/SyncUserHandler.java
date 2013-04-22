/**
 * 
 */
package org.rapla.plugin.exchangeconnector.server;

import java.util.Date;

import org.rapla.entities.User;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaContext;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;

/**
 * @author lutz
 *
 */
public class SyncUserHandler extends SynchronisationHandler implements Runnable {
	User raplaUser;
	public SyncUserHandler(RaplaContext context, ClientFacade clientFacade, User raplaUser) {
		super(context, clientFacade);
		this.raplaUser = raplaUser;
	}

	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
        //from/to filter

        final Date from = ExchangeConnectorPlugin.getSynchingPeriodPast(new Date());
        final Date to = ExchangeConnectorPlugin.getSynchingPeriodFuture(new Date());

        try {
            for (Reservation reservation : clientFacade.getReservations(raplaUser, from, to, null)) {

                for (Appointment appointment : reservation.getAppointments()) {
                    if (ExchangeAppointmentStorage.getInstance().appointmentExists(appointment)) {
                        String appointmentOwnerUsernameOnExchange = ExchangeAppointmentStorage
                                .getInstance().getRaplaUsername(appointment);
                        if (appointmentOwnerUsernameOnExchange.isEmpty()) {
                            appointmentOwnerUsernameOnExchange = raplaUser
                                    .getUsername();
                        }
                        DeleteRaplaAppointmentWorker deleteRaplaAppointmentWorker = new DeleteRaplaAppointmentWorker(
                                getContext(),
                                clientFacade,
                                appointmentOwnerUsernameOnExchange,
                                ExchangeConnectorUtils.getAppointmentSID(appointment));
                        deleteRaplaAppointmentWorker.perform();
                    }
                    UploadRaplaAppointmentWorker uploadRaplaAppointmentWorker = new UploadRaplaAppointmentWorker(getContext(), clientFacade, appointment, null);
                    uploadRaplaAppointmentWorker.perform();
                }
            }
        } catch (Exception e) {
			e.printStackTrace();
			SynchronisationManager.logException(e);
		}
	}

}
