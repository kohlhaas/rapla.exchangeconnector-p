/**
 * 
 */
package org.rapla.plugin.exchangeconnector;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

import microsoft.exchange.webservices.data.ArgumentException;
import microsoft.exchange.webservices.data.ArgumentOutOfRangeException;
import microsoft.exchange.webservices.data.Attendee;
import microsoft.exchange.webservices.data.BodyType;
import microsoft.exchange.webservices.data.ConflictResolutionMode;
import microsoft.exchange.webservices.data.DayOfTheWeek;
import microsoft.exchange.webservices.data.DayOfTheWeekIndex;
import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.LegacyFreeBusyStatus;
import microsoft.exchange.webservices.data.MailboxType;
import microsoft.exchange.webservices.data.MessageBody;
import microsoft.exchange.webservices.data.Month;
import microsoft.exchange.webservices.data.Recurrence;
import microsoft.exchange.webservices.data.SendCancellationsMode;
import microsoft.exchange.webservices.data.SendInvitationsMode;
import microsoft.exchange.webservices.data.SendInvitationsOrCancellationsMode;
import microsoft.exchange.webservices.data.ServiceLocalException;

import org.rapla.entities.User;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.facade.CalendarOptions;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.internal.CalendarOptionsImpl;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.datastorage.ExchangeAppointmentStorage;

/**
 * The worker-class for uploading a new- or changed appointment from Rapla to
 * the respective Exchange Server.
 * 
 * @author lutz
 * @see {@link EWSWorker}
 */
/**
 * @author lutz
 *
 */
public class UploadRaplaAppointmentWorker extends EWSProxy {

	private static final String LINE_BREAK = "\n";
	private static final String BODY_ATTENDEE_LIST_OPENING_LINE = "The following resources participate in the appointment:"+LINE_BREAK;
	private Appointment raplaAppointment;
	private String bodyAttendeeList;
    private String exchangeId;

    /**
	 * The constructor
	 *  

	 *
     *
     *
     * @param facade : {@link org.rapla.facade.ClientFacade}
     * @param appointment : {@link org.rapla.entities.domain.Appointment}
     * @param exchangeId
     * @throws Exception
	 */
	public UploadRaplaAppointmentWorker(ClientFacade facade, Appointment appointment, String exchangeId) throws Exception{
		super(facade, appointment);
		setRaplaAppointment(appointment);
		bodyAttendeeList = new String();
        this.exchangeId = exchangeId;
	}

	/**
	 * This method holds the core functionality of the worker. It creates a
	 * {@link microsoft.exchange.webservices.data.Appointment} and saves its Exchange-Representation
	 * (Appointment) to the Exchange Server.
	 * @throws Exception 
	 * 
	 */
	public void perform() throws Exception {

		if (getRaplaAppointment() != null && getService() != null) {
			microsoft.exchange.webservices.data.Appointment exchangeAppointment = getEquivalentExchangeAppointment(raplaAppointment);
			
			setExchangeRecurrence(raplaAppointment, exchangeAppointment);
			
			addRequiredAttendees(exchangeAppointment);
			
			setMessageBody(exchangeAppointment);

            addRoomResource(exchangeAppointment);

            saveToExchangeServer(exchangeAppointment);

			saveToStorageManager(exchangeAppointment);
			
			removeRecurrenceExceptions(raplaAppointment, exchangeAppointment);
			
		}
	}

	/**
	 * @param exchangeAppointment
	 * @throws ArgumentOutOfRangeException
	 * @throws ArgumentException
	 * @throws Exception
	 */
	private microsoft.exchange.webservices.data.Appointment setExchangeRecurrence( Appointment raplaAppointment, microsoft.exchange.webservices.data.Appointment exchangeAppointment) throws ArgumentOutOfRangeException, ArgumentException, Exception {
	
		Recurrence recurrence = getExchangeRecurrence(raplaAppointment);
		if (recurrence != null) {
			exchangeAppointment.setRecurrence(recurrence);
		}
		
		return exchangeAppointment;
	}

	/**
	 * @param exchangeAppointment
	 * @return 
	 * @throws ServiceLocalException
	 * @throws Exception
	 */
	private microsoft.exchange.webservices.data.Appointment saveToExchangeServer( microsoft.exchange.webservices.data.Appointment exchangeAppointment) throws ServiceLocalException, Exception {
		// save the appointment to the server
		if (exchangeAppointment.isNew()) {
            SynchronisationManager.logInfo("Adding "+exchangeAppointment.getSubject());
			exchangeAppointment.save(SendInvitationsMode.SendOnlyToAll);
		}
		else{
            SynchronisationManager.logInfo("Updating "+exchangeAppointment.getId()+ " "+exchangeAppointment.getSubject()+","+exchangeAppointment.getWhen());
			exchangeAppointment.update(ConflictResolutionMode.AlwaysOverwrite,SendInvitationsOrCancellationsMode.SendOnlyToAll);
		}
		
		return exchangeAppointment;
	}

	/**
	 * @param exchangeAppointment
	 * @throws ServiceLocalException
	 */
	private void saveToStorageManager( microsoft.exchange.webservices.data.Appointment exchangeAppointment) throws ServiceLocalException {
		
		String raplaUsername = getRaplaUser().getUsername();
		
		ExchangeAppointmentStorage.getInstance().addAppointment(
				getRaplaAppointment(),
				exchangeAppointment.getId().getUniqueId(), 
				raplaUsername,
				false);
		ExchangeAppointmentStorage.getInstance().save();
	}
	
	/**
	 * @param raplaAppointment
	 * @return
	 * @throws ArgumentOutOfRangeException
	 * @throws ArgumentException
	 * @throws Exception
	 */
	private microsoft.exchange.webservices.data.Appointment getEquivalentExchangeAppointment(Appointment raplaAppointment) throws ArgumentOutOfRangeException, ArgumentException, Exception {
        microsoft.exchange.webservices.data.Appointment exchangeAppointment = null;
		if (exchangeId != null && !exchangeId.isEmpty())  {
            try {
                exchangeAppointment = ExchangeConnectorUtils.getExchangeAppointmentByID(getService(), exchangeId);
            } catch (Exception e) {
                SynchronisationManager.logException(e);
            }
        }
        if (exchangeAppointment == null) {
            // if we could not find specific appointment, remove it from storage
            if (exchangeId != null &&  !exchangeId.isEmpty())
            {
                ExchangeAppointmentStorage.getInstance().removeAppointment(exchangeId);
                ExchangeAppointmentStorage.getInstance().setDeleted(exchangeId);
            }

            exchangeAppointment = new microsoft.exchange.webservices.data.Appointment( getService());
        }

		final Date startDate = ExchangeConnectorUtils.translateRaplaToExchangeTime(raplaAppointment.getStart());
		final Date endDate = ExchangeConnectorUtils.translateRaplaToExchangeTime(raplaAppointment.getEnd());

		exchangeAppointment.setStart(startDate);
        exchangeAppointment.setEnd(endDate);
        exchangeAppointment.setIsAllDayEvent(ExchangeConnectorPlugin.isAllDayEvent(raplaAppointment, getCalendarOptions(getRaplaUser())));
		exchangeAppointment.setSubject(raplaAppointment.getReservation().getName(Locale.GERMAN));
		exchangeAppointment.setIsResponseRequested(false);
        exchangeAppointment.setIsReminderSet(ExchangeConnectorPlugin.EXCHANGE_REMINDER_SET);
        exchangeAppointment.setLegacyFreeBusyStatus(LegacyFreeBusyStatus.valueOf(ExchangeConnectorPlugin.EXCHANGE_FREE_AND_BUSY));


		if (exchangeAppointment.isNew()) {
            // add category for filtering
            exchangeAppointment.getCategories().add(ExchangeConnectorPlugin.EXCHANGE_APPOINTMENT_CATEGORY);
            // add category for each event type
            exchangeAppointment.getCategories().add(getFacade().getDynamicType(raplaAppointment.getReservation().getClassification().getType().getElementKey()).getName(Locale.getDefault()));
            // add rapla specific property
            exchangeAppointment.setExtendedProperty(raplaAppointmentPropertyDefinition, Boolean.TRUE);
        }
		return exchangeAppointment;
	}

	/**
	 * @param exchangeAppointment
	 * @throws Exception
	 */
	private void setMessageBody( microsoft.exchange.webservices.data.Appointment exchangeAppointment) throws Exception {
		String content = RAPLA_BODY_MESSAGE;
		content += bodyAttendeeList.isEmpty() ? "" : bodyAttendeeList;
		content += RAPLA_NOSYNC_KEYWORD;
		exchangeAppointment.setBody(new MessageBody(BodyType.Text, content));
	}

	/**
	 * @param exchangeAppointment
	 * @throws ServiceLocalException
	 * @throws Exception
	 */
	private void addRequiredAttendees( microsoft.exchange.webservices.data.Appointment exchangeAppointment) throws ServiceLocalException, Exception {
        // get all restricted resources
        final Set<Allocatable> allocatablePersons = ExchangeConnectorUtils.getAttachedPersonAllocatables(raplaAppointment);
        // join and check for mail address, if so, add to reservation
        exchangeAppointment.getRequiredAttendees().clear();
        for (Allocatable restrictedAllocatable : allocatablePersons) {
            if (restrictedAllocatable.isPerson()) {
                final String name = restrictedAllocatable.getName(Locale.getDefault());
                final Object email = restrictedAllocatable.getClassification().getValue(ExchangeConnectorPlugin.RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL);
                if (email != null && !email.toString().isEmpty() && !email.toString().equalsIgnoreCase(getRaplaUser().getEmail())) {
                    exchangeAppointment.getRequiredAttendees().add(name, email.toString());
                    if (ExchangeConnectorPlugin.EXCHANGE_EXPECT_RESPONSE)
                        exchangeAppointment.setIsResponseRequested(true);
                }

            }
        }

	}

    /**
     * @param exchangeAppointment
     * @throws ServiceLocalException
     * @throws Exception
     */
    private void addRoomResource( microsoft.exchange.webservices.data.Appointment exchangeAppointment) throws ServiceLocalException, Exception {
        //todo: generify with option attributes
        //download from exchange as well!

        // get all restricted resources
        Allocatable[] resources = raplaAppointment.getReservation().getRestrictedAllocatables(raplaAppointment);
        if (resources.length == 0)
            resources = raplaAppointment.getReservation().getResources();
        // join and check for mail address, if so, add to reservation
        final List<String> buffer = new ArrayList<String>();
        exchangeAppointment.getResources().clear();
        for (Allocatable restrictedAllocatable : resources) {
            if (!restrictedAllocatable.isPerson()) {
                final String name = restrictedAllocatable.getName(Locale.getDefault());
                try {
                    final Classification classification = restrictedAllocatable.getClassification();
                    //todo: define room resource type in plugin and check for rooms here
                    //if (classification.getType().equals(ExchangeConnectorPlugin.XXXX)
                    final Object email = classification.getValue(ExchangeConnectorPlugin.RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL);
                    if (email != null && !email.toString().isEmpty()) {
                        final Attendee attendee = new Attendee(email.toString());
                        attendee.setMailboxType(MailboxType.Mailbox);
                        attendee.setRoutingType("SMTP");
                        attendee.setName(name);
                        exchangeAppointment.getResources().add(attendee);
                    }
                } catch (NoSuchElementException e) {
                     // ok, some resources do not have attribute email, just add it to location attribute
                } finally {
                    buffer.add(name);
                }
            }
        }
        if (buffer.size() > 0) {
            StringBuilder location = new StringBuilder();
            for (String name : buffer) {
                location.append(name);
                if (buffer.size() > 1) {
                    location.append(", ");
                }
            }
            exchangeAppointment.setLocation(location.toString());
        }

    }


    private void appendAttendeeToBodyMessage(String currentUsersName) {
		if(bodyAttendeeList.isEmpty()){
			bodyAttendeeList = BODY_ATTENDEE_LIST_OPENING_LINE;
		}
		bodyAttendeeList += currentUsersName+LINE_BREAK;
	}

	/**
	 * @param raplaAppointment
	 * @return 
	 * @throws Exception
	 */
	private String getLocationString(Appointment raplaAppointment) throws Exception {
		String locations = new String();
		for (Allocatable allocatable : raplaAppointment.getReservation().getAllocatables()) {
			
			if (!allocatable.isPerson()) {
				locations += (locations.isEmpty() ? "" : ", ") + allocatable.getName(Locale.GERMAN);
			}
		}
		return locations;
	}
	/**
	 * @param raplaAppointment
	 * @return
	 * @throws ArgumentOutOfRangeException
	 * @throws ArgumentException
	 */
	private Recurrence getExchangeRecurrence(Appointment raplaAppointment) throws ArgumentOutOfRangeException, ArgumentException {

		Recurrence returnVal = null;
		Repeating repeating = raplaAppointment.getRepeating();

		if (repeating != null) {
			
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(raplaAppointment.getStart());
			int dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK) - 1;
			int dayOfMonthInt = calendar.get(Calendar.DAY_OF_MONTH);
			int weekOfMonthInt = calendar.get(Calendar.WEEK_OF_MONTH)-1;
			int monthInt = calendar.get(Calendar.MONTH);
			DayOfTheWeek[] daysOfWeek = DayOfTheWeek.values();
			DayOfTheWeekIndex[] dayOfTheWeekIndizes = DayOfTheWeekIndex.values();
			Month[] months = Month.values();
			
			RaplaRepeatingType raplaRepeatingType = RaplaRepeatingType.getRaplaRepeatingType(repeating.getType());
			
			switch (raplaRepeatingType) {
				case DAILY:
					returnVal = new Recurrence.DailyPattern(
							raplaAppointment.getStart(), repeating.getInterval());
					break;
				case WEEKLY:
					returnVal = new Recurrence.WeeklyPattern(
							raplaAppointment.getStart(), repeating.getInterval(),
							daysOfWeek[dayOfWeekInt]);
					break;
				case MONTHLY:
					returnVal = new Recurrence.RelativeMonthlyPattern(raplaAppointment.getStart(),repeating.getInterval(),daysOfWeek[dayOfWeekInt], dayOfTheWeekIndizes [weekOfMonthInt]);
					break;
				default:
					returnVal = new Recurrence.YearlyPattern(
							raplaAppointment.getStart(), months[monthInt],
							dayOfMonthInt);
					break;
			}
			if (repeating.isFixedNumber()) {
				returnVal.setNumberOfOccurrences(repeating.getNumber());
			} 
			else {
				
				if (repeating.getEnd() != null) {
					returnVal.setEndDate(repeating.getEnd());
				} 
				else {
					returnVal.neverEnds();
				}
			}
		}
		return returnVal;
	}
	/**
	 * @param raplaAppointment
	 * @param exchangeAppointment
	 * @return 
	 * @throws ServiceLocalException
	 * @throws Exception
	 */
	private microsoft.exchange.webservices.data.Appointment removeRecurrenceExceptions(Appointment raplaAppointment, microsoft.exchange.webservices.data.Appointment exchangeAppointment) throws ServiceLocalException, Exception {
		
		if (raplaAppointment.isRepeatingEnabled()) {
			Set<String> exceptionDates = new HashSet<String>();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			
			for (Date exceptionDate : raplaAppointment.getRepeating().getExceptions()) {
				exceptionDates.add(dateFormat.format(exceptionDate));
			}

			try {
                microsoft.exchange.webservices.data.Appointment occurrence;
				for (int occurrenceIndex = 1; (occurrence = microsoft.exchange.webservices.data.Appointment
						.bindToOccurrence(getService(),
								exchangeAppointment.getId(), occurrenceIndex)) != null; occurrenceIndex++) {

					String occurrenceDateString = dateFormat.format(occurrence.getStart());

					if (exceptionDates.contains(occurrenceDateString)) {
                        SynchronisationManager.logInfo("Removing exception for "+occurrence.getId().getUniqueId()+ " "+occurrence.toString());
						occurrence.delete(DeleteMode.HardDelete, SendCancellationsMode.SendOnlyToAll);
					}
				}
			} catch (Exception e) {
				//Intended Exception
			}
        }
		return exchangeAppointment;
	}
	/**
	 * @return the raplaAppointment
	 */
	private Appointment getRaplaAppointment() {
		return raplaAppointment;
	}

	/**
	 * @param raplaAppointment : {@link Appointment}
	 *            the raplaAppointment to set
	 */
	private void setRaplaAppointment(Appointment raplaAppointment) {
		this.raplaAppointment = raplaAppointment;
	}

    protected CalendarOptions getCalendarOptions(User user) {
        RaplaConfiguration conf = null;
        try {
            if ( user != null)
            {
                conf = (RaplaConfiguration) getFacade().getPreferences( user ).getEntry(CalendarOptionsImpl.CALENDAR_OPTIONS);
            }
            if ( conf == null)
            {
                conf = (RaplaConfiguration)getFacade().getPreferences( null ).getEntry(CalendarOptionsImpl.CALENDAR_OPTIONS);
            }
            if ( conf != null)
            {
                return new CalendarOptionsImpl( conf.getConfig());
            }
        } catch (RaplaException ex) {

        }
        return null;
    }


}
