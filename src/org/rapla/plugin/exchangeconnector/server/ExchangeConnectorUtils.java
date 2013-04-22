package org.rapla.plugin.exchangeconnector.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SimpleTimeZone;

import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ItemId;
import microsoft.exchange.webservices.data.ServiceResponseException;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VTimeZone;

import org.rapla.components.util.Assert;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.internal.AppointmentImpl;
import org.rapla.entities.domain.internal.ReservationImpl;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.facade.AllocationChangeEvent;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;
import org.rapla.storage.impl.AbstractCachableOperator;

public class ExchangeConnectorUtils {
    private static final SimpleTimeZone gmt = new SimpleTimeZone(0, "GMT");
    private static java.util.TimeZone timezone;

    static {
        final java.util.TimeZone prefereredTimeZone = java.util.TimeZone.getTimeZone(ExchangeConnectorPlugin.TIMEZONE);

        //todo: this is still a problem!
        //how to handle different timezones?
        if (prefereredTimeZone != null) {
            try {
                final String timezoneId = prefereredTimeZone.getID();
                TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
                timezone = registry.getTimeZone(timezoneId);
            } catch (Exception rc) {
                final VTimeZone vTimeZone = new VTimeZone();
                timezone = new net.fortuna.ical4j.model.TimeZone(vTimeZone);
                final int rawOffset = prefereredTimeZone.getRawOffset();
                timezone.setRawOffset(rawOffset);
            }
        }
        Assert.notNull(timezone);
        SynchronisationManager.logInfo("Using timezone: " + timezone.getDisplayName());
    }

    public static Appointment getAppointmentById(int id, ClientFacade facade) {
        // ignore not existing facade
        if (facade == null)
            return null;
        AbstractCachableOperator operator = (AbstractCachableOperator) facade.getOperator();
        SimpleIdentifier simpleIdentifier = new SimpleIdentifier(Appointment.TYPE, id);
        RefEntity refEntity = operator.getCache().get(simpleIdentifier);
        return (Appointment) refEntity;
    }

    public static Reservation getReservationById(int id, ClientFacade facade) {
        SimpleIdentifier simpleIdentifier = new SimpleIdentifier(Reservation.TYPE, id);
        return getReservationById(simpleIdentifier, facade);
    }

    public static Reservation getReservationById(SimpleIdentifier id, ClientFacade facade) {
        return (Reservation) getEntityBySimpleIdentifier(id, facade);

    }

    public static RefEntity getEntityBySimpleIdentifier(SimpleIdentifier id, ClientFacade facade) {
        // ignore not existing facade
        if (facade == null)
            return null;
        AbstractCachableOperator operator = (AbstractCachableOperator) facade.getOperator();
        RefEntity refEntity = operator.getCache().get(id);
        return refEntity;
    }


    /**
     * return id of appointment
     *
     * @param appointment
     * @return
     */

    public static int getAppointmentID(Appointment appointment) {
        Object oid = ((AppointmentImpl) appointment).getId();
        SimpleIdentifier sid = (SimpleIdentifier) oid;
        Assert.notNull(sid);
        return sid.getKey();
    }


    public static SimpleIdentifier getAppointmentSID(Appointment appointment) {
        Object oid = ((AppointmentImpl) appointment).getId();
        SimpleIdentifier sid = (SimpleIdentifier) oid;
        Assert.notNull(sid);
        return sid;
    }

    /*public static HashSet<User> getUsers(Appointment appointment
            , ClientFacade clientFacade) throws RaplaException {


        // this user list will be filled with all related users
        HashSet<User> users = new HashSet<User>();

        // iterate over all existing users
        //todo: do we really have to iterate all users in rapla?
        for (User user : clientFacade.getUsers()) {
            // the user is related to this reservation
            //this does not seem to work always
            if (hasUser(appointment, user))
                // add this user to the temporary users list
                users.add(user);
        }
        return users;
    }

    public static boolean hasUser(Appointment appointment, User user) {
        Allocatable allocatableUser = user.getPerson();

        Allocatable[] allocatables = appointment.getReservation().getRestrictedAllocatables(appointment);
        if (allocatables.length > -1)
            allocatables = appointment.getReservation().getAllocatables();

        for (Allocatable tmpAllocatable : allocatables) {
            if (tmpAllocatable.equals(allocatableUser))
                return true;
        }
        return false;

//		if (allocatableUser != null) {
//			for (Appointment appointment : this.getReservation().getAppointmentsFor(allocatableUser)) {
//				if (appointment.equals(this)) {
//					return true;
//				}
//			}
//		}
//		return false;
    }*/

    /**
     * Method to delete an Exchange {@link microsoft.exchange.webservices.data.Appointment} with a particular exchange id from the Exchange Server
     *
     * @param clientFacade
     * @param deleteAppointments : {@link String} the unique id of the {@link microsoft.exchange.webservices.data.Appointment} on the Exchange Server
     * @param addToDeleteList    : {@link Boolean} flag, if this appointment should be remembered in the "to delete" list in case deleting the item from the Exchange Server fails
     * @throws Exception
     * @see org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage
     */
    public static void deleteAppointments(RaplaContext context, final ClientFacade clientFacade, final HashSet<SimpleIdentifier> deleteAppointments, boolean addToDeleteList) throws Exception {
        for (SimpleIdentifier simpleIdentifier : deleteAppointments) {
            if (ExchangeAppointmentStorage.getInstance().appointmentExists(simpleIdentifier.getKey())
                    && !ExchangeAppointmentStorage.getInstance().isExchangeItem(simpleIdentifier.getKey())) {

                if (addToDeleteList) {
                    ExchangeAppointmentStorage.getInstance().setDeleted(simpleIdentifier.getKey());
                    ExchangeAppointmentStorage.getInstance().save();
                }
                final String raplaUsername = ExchangeAppointmentStorage.getInstance().getRaplaUsername(simpleIdentifier.getKey());
                DeleteRaplaAppointmentWorker deleteRaplaAppointmentWorker = new DeleteRaplaAppointmentWorker(context, clientFacade, raplaUsername, simpleIdentifier);
                deleteRaplaAppointmentWorker.perform();
            }
        }
    }

    public static SimpleIdentifier getReservationID(Reservation newReservation) {
        Object oid = ((ReservationImpl) newReservation).getId();
        SimpleIdentifier sid = (SimpleIdentifier) oid;
        Assert.notNull(sid);
        return sid;
    }

    /**
     * Translates a date coming from rapla into an exchange-conform instance
     *
     * @param date
     * @return
     */
    public static Date translateRaplaToExchangeTime(Date date, java.util.TimeZone timeZone) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(timeZone);
        int offset = cal.getTimeZone().getOffset(date.getTime());
        cal.add(Calendar.MILLISECOND, -offset);

        return cal.getTime();
    }


    public static Date translateRaplaToExchangeTime(Date date) {
        //return date;
        return translateRaplaToExchangeTime(date, timezone);
    }

    /**
     * Translates a date coming from exchange into a rapla-conform instance
     * <p/>
     * Exchange expects to be in local time (why ever) so it adds 2 hours to given time.
     * hence we have to subtract 2 hour from given utc to be correct again
     * <p/>
     * Rapla stores all dates in GMT, hence transfering it to Exchange which has Time
     *
     * @param date
     * @return
     */

    public static Date translateExchangeToRaplaTime(Date date, java.util.TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setTimeZone(timeZone);
        int offset = calendar.getTimeZone().getOffset(date.getTime());
        calendar.add(Calendar.MILLISECOND, +offset);
        return calendar.getTime();
    }

    public static Date translateExchangeToRaplaTime(Date start) {
        return translateExchangeToRaplaTime(start, timezone);
    }

    public static void updateAppointment(RaplaContext context, ClientFacade clientFacade, Appointment appointment, String exchangeId) throws Exception {
        SynchronisationManager.logInfo("add/updating " + appointment + " with exchangeid  " + exchangeId);

        if (appointment.getStart().after(ExchangeConnectorPlugin.getSynchingPeriodPast(new Date())) && appointment.getStart().before(ExchangeConnectorPlugin.getSynchingPeriodFuture(new Date()))) {
            if (!ExchangeAppointmentStorage.getInstance().isExchangeItem(appointment)) {
                UploadRaplaAppointmentWorker worker = new UploadRaplaAppointmentWorker(
                        context, clientFacade,
                        appointment,
                        exchangeId);
                worker.perform();
            }
        }
    }

    public static void addAppointment(ClientFacade clientFacade, SimpleIdentifier appointment) throws Exception {
        //we have to chack for update since an add event is even invoked when adding a second (!) resource
//        updateAppointment(clientFacade, appointment, ExchangeAppointmentStorage.getInstance().getExchangeId(appointment.getKey()));
    }

    public static void deleteAppointment(RaplaContext context, ClientFacade clientFacade, SimpleIdentifier appointment) throws Exception {
        final HashSet<SimpleIdentifier> deleteAppointments = new HashSet<SimpleIdentifier>();
        deleteAppointments.add(appointment);
        deleteAppointments(context, clientFacade, deleteAppointments, true);
    }

    /**
     * returns true if appointment has synchronized attribute
     *
     * @param oldAppointment
     * @return
     */
    public static boolean isSynchronized(Appointment oldAppointment) {
        return false;  //To change body of created methods use File | Settings | File Templates.
    }

    public static microsoft.exchange.webservices.data.Appointment getExchangeAppointmentByID(ExchangeService service, String exchangeId) throws Exception {
        try {
            return microsoft.exchange.webservices.data.Appointment.bind(service, new ItemId(exchangeId));
        } catch (ServiceResponseException e) {
            // item not found error is to be expected sometimes so do not raise!
            if (e.getErrorCode().ordinal() == 225) {
                return null;
            }
            throw e;
        }

    }

    public static void synchronizeAppointmentRequest(RaplaContext context, ClientFacade clientFacade, AllocationChangeEvent.Type changeEvent, SimpleIdentifier raplaIdentifier) throws Exception {
        final Appointment appointment = (Appointment) getEntityBySimpleIdentifier(raplaIdentifier, clientFacade);
        if (appointment != null) {
            final String exchangeId = ExchangeAppointmentStorage.getInstance().getExchangeId(raplaIdentifier.getKey());
            if (exchangeId != null && !"".equals(exchangeId))
                updateAppointment(context, clientFacade, appointment, exchangeId);
        } else {
            deleteAppointment(context, clientFacade, raplaIdentifier);
        }


    }

    public static List<User> getAppointmentUsers(Appointment raplaAppointment, ClientFacade clientFacade) throws RaplaException {
        // get all restricted resources
        final Allocatable[] restrictedAllocatables = raplaAppointment.getReservation().getRestrictedAllocatables(raplaAppointment);
        // get all non restricted resources
        final Allocatable[] persons = raplaAppointment.getReservation().getPersons();
        final List<Allocatable> allocatablePersons = new ArrayList<Allocatable>();
        allocatablePersons.addAll(Arrays.asList(restrictedAllocatables));
        allocatablePersons.addAll(Arrays.asList(persons));
        final List<User> results = new ArrayList<User>();

        // todo: here we are really missing a feature!
        // todo: mapping of person -> users is very inefficient
        final User[] users = clientFacade.getUsers();
        for (User user : users) {
            if (user.getPerson() != null && allocatablePersons.contains(user.getPerson())) {

                results.add(user);
            }
        }

        return results;
    }

    static Set<Allocatable> getAttachedPersonAllocatables(Appointment raplaAppointment) {
        final Allocatable[] restrictedAllocatables = raplaAppointment.getReservation().getRestrictedAllocatables(raplaAppointment);
        // get all non restricted resources
        final Allocatable[] persons = raplaAppointment.getReservation().getPersons();
        final Set<Allocatable> allocatablePersons = new HashSet<Allocatable>();
        for (Allocatable restrictedAllocatable : restrictedAllocatables) {
            if (restrictedAllocatable.isPerson())
                allocatablePersons.add(restrictedAllocatable);
        }
        allocatablePersons.addAll(Arrays.asList(persons));
        return allocatablePersons;
    }
}
