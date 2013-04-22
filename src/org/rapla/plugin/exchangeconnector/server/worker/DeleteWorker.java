/**
 *
 */
package org.rapla.plugin.exchangeconnector.server.worker;

import microsoft.exchange.webservices.data.*;
import org.rapla.entities.User;
import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.framework.RaplaContext;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;


/**
 * This worker is employed if an appointment needs to be deleted.
 *
 * @author lutz
 * @see {@link org.rapla.plugin.exchangeconnector.server.worker.EWSWorker}
 */
 class DeleteWorker extends EWSWorker {


    /**
     * @param user
     * @throws Exception
     */
    public DeleteWorker(RaplaContext context, User user) throws Exception {
        super(context, user);
    }

    /**
     * This method holds the core functionality of the worker.
     *
     * @throws Exception
     */
    public synchronized void perform(SimpleIdentifier identifier) throws Exception {
        if (!ExchangeAppointmentStorage.getInstance().isExternalAppointment(identifier.getKey())) {

            String exchangeId = ExchangeAppointmentStorage.getInstance().getExchangeId(identifier.getKey());
            if (exchangeId == null || "".equals(exchangeId))
                return;
            Appointment exchangeAppointment;
            try {
                try {
                    exchangeAppointment = Appointment.bindToRecurringMaster(getService(), new ItemId(exchangeId));
                } catch (Exception e) {
                    exchangeAppointment = Appointment.bind(getService(), new ItemId(exchangeId));
                }
                try {
                    getLogger().debug("Deleting  " + exchangeAppointment.getId().getUniqueId() + " " + exchangeAppointment.toString());
                    exchangeAppointment.delete(DeleteMode.HardDelete, SendCancellationsMode.SendOnlyToAll);
                } catch (ServiceResponseException e) {
                    getLogger().error(e.getMessage(), e);
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
