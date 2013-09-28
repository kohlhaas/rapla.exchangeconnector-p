package org.rapla.plugin.exchangeconnector.server.worker;

import org.rapla.entities.RaplaObject;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.ExchangeConnectorUtils;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;


public class AppointmentTask extends RaplaComponent {

    public AppointmentTask(RaplaContext context) {
        super(context);
    }


    private synchronized void deleteAppointments(Set<RaplaObject> removed) throws RaplaException {
        for (RaplaObject raplaObject : removed) {
            if (raplaObject instanceof Appointment) {
                Appointment appointment = (Appointment) raplaObject;
                if (check(appointment))
                    deleteAppointment(appointment);
            }

        }

    }

    public synchronized void deleteAppointment(Appointment appointment) throws RaplaException {
        try {
            final SimpleIdentifier appointmentSID = ExchangeConnectorUtils.getAppointmentSID(appointment);
            if (appointment != null) {
                final HashSet<SimpleIdentifier> deleteAppointments = new HashSet<SimpleIdentifier>();
                deleteAppointments.add(appointmentSID);
                deleteAppointments(deleteAppointments, true);

            }
        } catch (Exception e) {
            throw new RaplaException(e);
        }

    }

    /**
     * Method to delete an Exchange {@link microsoft.exchange.webservices.data.Appointment} with a particular exchange id from the Exchange Server
     *
     * @param deleteAppointments : {@link String} the unique id of the {@link microsoft.exchange.webservices.data.Appointment} on the Exchange Server
     * @param addToDeleteList    : {@link Boolean} flag, if this appointment should be remembered in the "to delete" list in case deleting the item from the Exchange Server fails
     * @throws Exception
     * @see org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage
     */
    private void deleteAppointments(final HashSet<SimpleIdentifier> deleteAppointments, boolean addToDeleteList) throws Exception {
        for (SimpleIdentifier simpleIdentifier : deleteAppointments) {
            if (ExchangeAppointmentStorage.getInstance().appointmentExists(simpleIdentifier.getKey())
                    && !ExchangeAppointmentStorage.getInstance().isExternalAppointment(simpleIdentifier.getKey())) {

                if (addToDeleteList) {
                    ExchangeAppointmentStorage.getInstance().setDeleted(simpleIdentifier.getKey());
                    ExchangeAppointmentStorage.getInstance().save();
                }
                final String raplaUsername = ExchangeAppointmentStorage.getInstance().getRaplaUsername(simpleIdentifier.getKey());
                DeleteWorker deleteWorker = new DeleteWorker(getContext(), getClientFacade().getUser(raplaUsername));
                deleteWorker.perform(simpleIdentifier);
            }
        }
    }

    public synchronized void addOrUpdateAppointments(Set<RaplaObject> appointmentSet) throws RaplaException {
        final User[] users = getClientFacade().getUsers();

        for (RaplaObject raplaObject : appointmentSet) {
            if (raplaObject instanceof Appointment) {
                Appointment appointment = (Appointment) raplaObject;
                // check appointment and owner preferences
                //todo: replace owner by rapla user if not available
                //up to new only booked resources are informed
                if (check(appointment))
                {
                    Allocatable[] allocatablesFor = appointment.getReservation().getAllocatablesFor(appointment);
                    for (Allocatable allocatable : allocatablesFor) {
                        final Classification classification = allocatable.getClassification();
                        final Attribute attribute = classification.getAttribute(ExchangeConnectorPlugin.RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL);
                        if (allocatable.isPerson() && attribute != null) {
                            User user = getUserForEmail(users, classification.getValueAsString(attribute, getRaplaLocale().getLocale()));
                            // now check if user has prefs allowing to notify him
                            if (user != null && checkUser(user, appointment)) {
                                addOrUpdateAppointment(appointment);
                            }
                        }
                    }
                }
            }
        }
    }

    private static synchronized User getUserForEmail(User[] users, String email) throws RaplaException {
        User result = null;

        if (email != null && !email.isEmpty()) {
            // todo: do this more efficient

            for (User user : users) {
                if (user.getEmail().equalsIgnoreCase(email)) {
                    result = user;
                    break;
                }
            }
        }
        return result;
    }

    public synchronized void addOrUpdateAppointment(Appointment appointment) throws RaplaException {
        try {
            final SimpleIdentifier appointmentSID = ExchangeConnectorUtils.getAppointmentSID(appointment);
            if (appointment != null) {
                final String exchangeId = ExchangeAppointmentStorage.getInstance().getExchangeId(appointmentSID.getKey());
                final AddUpdateWorker worker = new AddUpdateWorker(
                        getContext(),
                        appointment
                );
                worker.perform(appointment, exchangeId);
            }
        } catch (Exception e) {
            throw new RaplaException(e);
        }
    }

    public synchronized void synchronize(ModificationEvent evt) {
        try {
            addOrUpdateAppointments(evt.getChanged());
        } catch (RaplaException e) {
            getLogger().error(e.getMessage(), e);
        }
        try {
            deleteAppointments(evt.getRemoved());
        } catch (RaplaException e) {
            getLogger().error(e.getMessage(), e);
        }
    }

    private synchronized boolean check(User user, Appointment appointment) throws RaplaException {
        boolean result = false;
        if (appointment.getStart().before(ExchangeConnectorPlugin.getSynchingPeriodPast(new Date())) || appointment.getStart().after(ExchangeConnectorPlugin.getSynchingPeriodFuture(new Date()))) {
            getLogger().info("Skipping update of appointment " + appointment + " because is date of item is out of range");
        } else {
            final ClientFacade clientFacade = getClientFacade();
            final DynamicType importEventType = ExchangeConnectorPlugin.getImportEventType(clientFacade);
            final DynamicType reservationType = appointment.getReservation().getClassification().getType();

            if (importEventType != null && reservationType.getElementKey().equals(importEventType.getElementKey())) {
                getLogger().info("Skipping appointment of type " + reservationType + " because is type of item pulled from exchange");
            } else {
                result = checkUser(user, appointment);
            }
        }
        return result;
    }

    private synchronized boolean checkUser(User user, Appointment appointment) throws RaplaException {
        boolean result = false;
        final ClientFacade clientFacade = getClientFacade();
        final DynamicType reservationType = appointment.getReservation().getClassification().getType();

        if (user != null) {
            Preferences preferences = clientFacade.getPreferences(user);
            if (preferences != null) {
                final String exportableTypes = preferences.getEntryAsString(ExchangeConnectorPlugin.EXPORT_EVENT_TYPE_KEY, null);
                if (exportableTypes == null) {
                    getLogger().info("Skipping appointment of type " + reservationType + " because filter is not defined for appointment user " + user.getUsername());
                } else {
                    if (!exportableTypes.contains(reservationType.getElementKey())) {
                        getLogger().info("Skipping appointment of type " + reservationType + " because filtered out by user " + user.getUsername());
                    } else {
                        result = true;
                    }
                }
            } else {
                getLogger().warn("Skipping appointment of type " + reservationType + " because user " + user.getUsername() + " has no preferences");
            }
        } else {
            getLogger().warn("Skipping appointment of type " + reservationType + " because user is not defined");
        }
        return result;
    }


    private synchronized boolean check(Appointment appointment) throws RaplaException {
        User owner = appointment.getOwner();
        return check(owner, appointment);
    }

    public synchronized void downloadUserAppointments(User user) {
        final Date from = ExchangeConnectorPlugin.getSynchingPeriodPast(new Date());
        final Date to = ExchangeConnectorPlugin.getSynchingPeriodFuture(new Date());

        try {

            for (Reservation reservation : getClientFacade().getReservations(user, from, to, null)) {

                for (Appointment appointment : reservation.getAppointments()) {
                    if (ExchangeAppointmentStorage.getInstance().appointmentExists(appointment)) {
                        deleteAppointment(appointment);
                    }
                    addOrUpdateAppointment(appointment);
                }
            }
        } catch (Exception e) {
            getLogger().error(e.getMessage(), e);
        }

    }
}
