package org.rapla.plugin.exchangeconnector;

import org.rapla.entities.RaplaObject;
import org.rapla.entities.User;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.ModificationEvent;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.datastorage.ExchangeAppointmentStorage;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: kuestermann
 * Date: 18.04.13
 * Time: 17:11
 * To change this template use File | Settings | File Templates.
 */
class ReservationChangedTask {
    /*   public synchronized void syncReservations(ClientFacade clientFacade, ModificationEvent evt) {
        Set<Reservation> reservationSet = new HashSet<Reservation>();
        addOrUpdateReservations(clientFacade, evt.getChanged(reservationSet));
        reservationSet = evt.getRemoved(reservationSet);
        deleteReservations(clientFacade, reservationSet);
    }

    private synchronized void deleteReservations(ClientFacade clientFacade, Set<Reservation> reservationSet) {
        for (Reservation reservation : reservationSet) {
            deleteReservation(clientFacade, reservation);
        }
    }

    private synchronized void deleteReservation(ClientFacade clientFacade, Reservation reservation) {

    }

    public synchronized void addOrUpdateReservations(ClientFacade clientFacade, Set<Reservation> reservationsSet) {
        for (Reservation reservation : reservationsSet) {
            addOrUpdateReservation(clientFacade, reservation);
        }
    }

    public synchronized void addOrUpdateReservation(ClientFacade clientFacade, Reservation reservation) {

    }*/

    public synchronized void syncAppointments(ClientFacade clientFacade, ModificationEvent evt) throws RaplaException {
        Set<Appointment> appointmentSet = new HashSet<Appointment>();
        addOrUpdateAppointments(clientFacade, evt.getChanged());
        deleteAppointments(clientFacade, evt.getRemoved());
    }

    private synchronized void deleteAppointments(ClientFacade clientFacade, Set<RaplaObject> removed) throws RaplaException {
        for (RaplaObject raplaObject : removed) {
            Appointment appointment = (Appointment) raplaObject;
            if (check(clientFacade, appointment))
                deleteAppointment(clientFacade, appointment);
        }

    }

    private synchronized void deleteAppointment(ClientFacade clientFacade, Appointment appointment) throws RaplaException {
        try {
            final SimpleIdentifier appointmentSID = ExchangeConnectorUtils.getAppointmentSID(appointment);
            if (appointment != null) {
                ExchangeConnectorUtils.deleteAppointment(clientFacade, appointmentSID);
            }
        } catch (Exception e) {
            throw new RaplaException(e);
        }

    }

    public synchronized void addOrUpdateAppointments(ClientFacade clientFacade, Set<RaplaObject> appointmentSet) throws RaplaException {
        for (RaplaObject raplaObject : appointmentSet) {
            if (raplaObject instanceof Appointment) {
                Appointment appointment = (Appointment) raplaObject;
                if (check(clientFacade, appointment))
                    addOrUpdateAppointment(clientFacade, appointment);
            }
        }
    }

    public synchronized void addOrUpdateAppointment(ClientFacade clientFacade, Appointment appointment) throws RaplaException {
        try {
            final SimpleIdentifier appointmentSID = ExchangeConnectorUtils.getAppointmentSID(appointment);
            if (appointment != null) {
                final String exchangeId = ExchangeAppointmentStorage.getInstance().getExchangeId(appointmentSID.getKey());
                final UploadRaplaAppointmentWorker worker = new UploadRaplaAppointmentWorker(
                            clientFacade,
                            appointment,
                            exchangeId);
                worker.perform();
                /*}

                    ExchangeConnectorUtils.updateAppointment(clientFacade, appointment, exchangeId);
                else
                    ExchangeConnectorUtils.addAppointment(clientFacade, appointmentSID);*/
            }
        } catch (Exception e) {
            throw new RaplaException(e);
        }
    }

    public synchronized void synchronize(ClientFacade clientFacade, ModificationEvent evt) throws RaplaException {
        //syncReservations(clientFacade, evt);
        syncAppointments(clientFacade, evt);
    }

    private synchronized boolean check(ClientFacade clientFacade, Appointment appointment) throws RaplaException {
        boolean result = false;
        if (appointment.getStart().before(ExchangeConnectorPlugin.getSynchingPeriodPast(new Date())) || appointment.getStart().after(ExchangeConnectorPlugin.getSynchingPeriodFuture(new Date()))) {
            SynchronisationManager.logInfo("Skipping update of appointment " + appointment+ " because is date of item is out of range");
        } else {
            final DynamicType importEventType = ExchangeConnectorPlugin.getImportEventType(clientFacade);
            final DynamicType reservationType = appointment.getReservation().getClassification().getType();

            if (importEventType != null && reservationType.getElementKey().equals(importEventType.getElementKey())) {
                SynchronisationManager.logInfo("Skipping appointment of type " + reservationType + " because is type of item pulled from exchange");
            } else {
                final String exportableTypes = clientFacade.getPreferences(appointment.getOwner()).getEntryAsString(ExchangeConnectorPlugin.EXPORT_EVENT_TYPE_KEY);
                if (exportableTypes == null) {
                    SynchronisationManager.logInfo("Skipping appointment of type " + reservationType + " because filter is not defined");
                } else {
                    if (!exportableTypes.contains(reservationType.getElementKey())) {
                        SynchronisationManager.logInfo("Skipping appointment of type " + reservationType + " because filtered out by user");
                    } else {
                        result = true;
                    }
                }
            }
        }
        return result;
    }
}
