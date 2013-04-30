package org.rapla.plugin.exchangeconnector.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ItemId;
import microsoft.exchange.webservices.data.ServiceResponseException;

import org.rapla.components.util.Assert;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.internal.AppointmentImpl;
import org.rapla.entities.domain.internal.ReservationImpl;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaException;
import org.rapla.storage.impl.AbstractCachableOperator;

public class ExchangeConnectorUtils {

//    static {
//    	
//        final java.util.TimeZone prefereredTimeZone = java.util.TimeZone.getTimeZone(ExchangeConnectorPlugin.TIMEZONE);
//
//        //todo: this is still a problem!
//        //how to handle different timezones?
//        if (prefereredTimeZone != null) {
//            try {
//                final String timezoneId = prefereredTimeZone.getID();
//                TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
//                timezone = registry.getTimeZone(timezoneId);
//            } catch (Exception rc) {
//                final VTimeZone vTimeZone = new VTimeZone();
//                timezone = new net.fortuna.ical4j.model.TimeZone(vTimeZone);
//                final int rawOffset = prefereredTimeZone.getRawOffset();
//                timezone.setRawOffset(rawOffset);
//            }
//        }
//        Assert.notNull(timezone);
//
//        SynchronisationManager.logInfo("Using timezone: " + timezone.getDisplayName());
//    }

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


    public static SimpleIdentifier getReservationID(Reservation newReservation) {
        Object oid = ((ReservationImpl) newReservation).getId();
        SimpleIdentifier sid = (SimpleIdentifier) oid;
        Assert.notNull(sid);
        return sid;
    }

   
    
/*
    public static void updateAppointment(RaplaContext context, ClientFacade clientFacade, Appointment appointment, String exchangeId) throws Exception {
        SynchronisationManager.logInfo("add/updating " + appointment + " with exchangeid  " + exchangeId);

        if (appointment.getStart().after(ExchangeConnectorPlugin.getSynchingPeriodPast(new Date())) && appointment.getStart().before(ExchangeConnectorPlugin.getSynchingPeriodFuture(new Date()))) {
            if (!ExchangeAppointmentStorage.getInstance().isExternalAppointment(appointment)) {
                AddUpdateWorker worker = new AddUpdateWorker(
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
    }*/


/*
    */

    /**
     * returns true if appointment has synchronized attribute
     *
     * @param service
     * @return
     *//*
    public static boolean isSynchronized(Appointment oldAppointment) {
        return false;  //To change body of created methods use File | Settings | File Templates.
    }*/
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

   /* public static void synchronizeAppointmentRequest(RaplaContext context, ClientFacade clientFacade, AllocationChangeEvent.Type changeEvent, SimpleIdentifier raplaIdentifier) throws Exception {
        final Appointment appointment = (Appointment) getEntityBySimpleIdentifier(raplaIdentifier, clientFacade);
        if (appointment != null) {
            final String exchangeId = ExchangeAppointmentStorage.getInstance().getExchangeId(raplaIdentifier.getKey());
            if (exchangeId != null && !"".equals(exchangeId))
                updateAppointment(context, clientFacade, appointment, exchangeId);
        } else {
            deleteAppointment(context, clientFacade, raplaIdentifier);
        }


    }*/

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

    public static Set<Allocatable> getAttachedPersonAllocatables(Appointment raplaAppointment) {
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
