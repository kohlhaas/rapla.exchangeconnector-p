/**
 * 
 */
package org.rapla.plugin.exchangeconnector.server;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.ItemId;
import microsoft.exchange.webservices.data.SendCancellationsMode;
import microsoft.exchange.webservices.data.ServiceResponseException;

import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaContext;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;

/**
 * This worker is employed if an appointment needs to be deleted.
 * 
 * @author lutz
 * @see {@link EWSProxy}
 */
public class DeleteRaplaAppointmentWorker extends EWSProxy {


    private SimpleIdentifier identifier;


    /**
	 *
     * @param clientFacade
     * @param raplaUsername
     * @param raplaAppointmentIdentifier
     * @throws Exception
	 */
	public DeleteRaplaAppointmentWorker(RaplaContext context, ClientFacade clientFacade, String raplaUsername, SimpleIdentifier raplaAppointmentIdentifier) throws Exception {
		super(context, clientFacade, raplaUsername);
		this.identifier = raplaAppointmentIdentifier;
	}

	/**
	 * This method holds the core functionality of the worker.
	 * @throws Exception 
	 * 
	 */
	public void perform() throws Exception{
			if (!ExchangeAppointmentStorage.getInstance().isExchangeItem(identifier.getKey())) {

				String exchangeId = ExchangeAppointmentStorage.getInstance().getExchangeId(identifier.getKey());
                if (exchangeId != null && !"".equals(exchangeId))
                    return;
				Appointment exchangeAppointment;
				try {
					try {
						exchangeAppointment = Appointment.bindToRecurringMaster( getService(), new ItemId(exchangeId));
					} catch (Exception e) {
						exchangeAppointment = Appointment.bind(getService(), new ItemId(exchangeId));
					}
                    try {
                        SynchronisationManager.logInfo("Deleting  "+exchangeAppointment.getId().getUniqueId()+ " "+exchangeAppointment.toString());
                        exchangeAppointment.delete(DeleteMode.HardDelete,SendCancellationsMode.SendOnlyToAll);
                    } catch (ServiceResponseException e) {
                        SynchronisationManager.logException(e);
                    }
                } catch (microsoft.exchange.webservices.data.ServiceResponseException e) {
					//can be ignored
				}
				//delete on the Exchange Server side
				//remove it from the "to-be-removed"-list
				ExchangeAppointmentStorage.getInstance().removeAppointment(identifier.getKey());
				ExchangeAppointmentStorage.getInstance().save();
			}
	}

}
