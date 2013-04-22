/**
 * 
 */
package org.rapla.plugin.exchangeconnector.server;


import org.rapla.facade.ClientFacade;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAccountInformationStorage;

/**
 * @author lutz
 *
 */
public class ScheduledDownloadHandler extends SynchronisationHandler {


	/**
	 * @param clientFacade
	 */
	public ScheduledDownloadHandler(ClientFacade clientFacade) {
		super(null, clientFacade);
	}

	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		try {
			deleteExchangeItemsFromRapla(clientFacade);
			downloadExchangeAppointments();
		} catch (Exception e) {
			SynchronisationManager.logException(e);
		}
	}

	private void downloadExchangeAppointments() throws Exception {
		for (String raplaUsername: ExchangeAccountInformationStorage.getInstance().getAllRaplaUsernames()) {
			if (ExchangeAccountInformationStorage.getInstance().isDownloadFromExchange(raplaUsername)) {
					DownloadExchangeAppointmentWorker appointmentWorker = new DownloadExchangeAppointmentWorker(
							clientFacade, raplaUsername);
					appointmentWorker.perform();
			}
		}
	}

}
