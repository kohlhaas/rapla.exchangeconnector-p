package org.rapla.plugin.exchangeconnector.server.exchange;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import microsoft.exchange.webservices.data.AppointmentSchema;
import microsoft.exchange.webservices.data.ArgumentException;
import microsoft.exchange.webservices.data.ArgumentOutOfRangeException;
import microsoft.exchange.webservices.data.Attendee;
import microsoft.exchange.webservices.data.BodyType;
import microsoft.exchange.webservices.data.ConflictResolutionMode;
import microsoft.exchange.webservices.data.DayOfTheWeek;
import microsoft.exchange.webservices.data.DayOfTheWeekIndex;
import microsoft.exchange.webservices.data.DefaultExtendedPropertySet;
import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.DeletedOccurrenceInfo;
import microsoft.exchange.webservices.data.DeletedOccurrenceInfoCollection;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ExtendedPropertyDefinition;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.FolderId;
import microsoft.exchange.webservices.data.Item;
import microsoft.exchange.webservices.data.ItemId;
import microsoft.exchange.webservices.data.ItemView;
import microsoft.exchange.webservices.data.LegacyFreeBusyStatus;
import microsoft.exchange.webservices.data.MailboxType;
import microsoft.exchange.webservices.data.MapiPropertyType;
import microsoft.exchange.webservices.data.MessageBody;
import microsoft.exchange.webservices.data.Month;
import microsoft.exchange.webservices.data.PropertySet;
import microsoft.exchange.webservices.data.Recurrence;
import microsoft.exchange.webservices.data.SearchFilter;
import microsoft.exchange.webservices.data.SendCancellationsMode;
import microsoft.exchange.webservices.data.SendInvitationsMode;
import microsoft.exchange.webservices.data.SendInvitationsOrCancellationsMode;
import microsoft.exchange.webservices.data.ServiceLocalException;
import microsoft.exchange.webservices.data.ServiceResponseException;
import microsoft.exchange.webservices.data.TimeZoneDefinition;
import microsoft.exchange.webservices.data.WebCredentials;
import microsoft.exchange.webservices.data.WellKnownFolderName;

import org.rapla.components.util.DateTools;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeAnnotations;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.framework.RaplaException;
import org.rapla.framework.logger.Logger;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig.ConfigReader;
import org.rapla.plugin.exchangeconnector.server.SynchronizationTask;
import org.rapla.plugin.exchangeconnector.server.SynchronizationTask.SyncStatus;
import org.rapla.server.TimeZoneConverter;

/**
 * 
 * synchronizes a rapla appointment with an exchange appointment.
 * contains the interface to the exchange api 
 *
 */
public class AppointmentSynchronizer
{

    public static ExtendedPropertyDefinition raplaAppointmentPropertyDefinition;
    User raplaUser;
    protected ConfigReader config;

    private static final String LINE_BREAK = "\n";
    private static final String BODY_ATTENDEE_LIST_OPENING_LINE = "The following resources participate in the appointment:" + LINE_BREAK;
    private static final ExtendedPropertyDefinition RAPLA_ID_PROPERTY_DEFINITION = new ExtendedPropertyDefinition(1, MapiPropertyType.String);

    TimeZoneConverter timeZoneConverter;
    Appointment raplaAppointment;
    TimeZone systemTimeZone = TimeZone.getDefault();
    SynchronizationTask appointmentTask;
    boolean sendNotificationMail;
    Logger logger;
    EWSConnector ewsConnector;

    public AppointmentSynchronizer(Logger logger, ConfigReader config, TimeZoneConverter converter, SynchronizationTask appointmentTask,
            Appointment appointment, User user, String exchangeUsername, String password, boolean sendNotificationMail) throws RaplaException
    {
        this.sendNotificationMail = sendNotificationMail;
        this.logger = logger;
        this.raplaUser = user;
        WebCredentials credentials = new WebCredentials(exchangeUsername, password);
        timeZoneConverter = converter;
        this.raplaAppointment = appointment;
        this.appointmentTask = appointmentTask;
        this.config = config;

        final String url = config.get(ExchangeConnectorConfig.EXCHANGE_WS_FQDN);
        try
        {
            final Logger ewsLogger = logger.getChildLogger("webservice");
            ewsConnector = new EWSConnector(url, credentials, ewsLogger);
            //            final PropertySet propertySet = new PropertySet(RAPLA_ID_PROPERTY_DEFINITION);
            //            final ExchangeService service = ewsConnector.getService();
            //            final Folder folder = Folder.bind(service, WellKnownFolderName.Calendar, propertySet);
            //            folder.setExtendedProperty(RAPLA_ID_PROPERTY_DEFINITION, true);
            //            folder.update();
        }
        catch (Exception e)
        {
            // property definition should not throw an exception as no web service is called
            throw new RaplaException(e.getMessage(), e);
        }
        if (raplaAppointmentPropertyDefinition == null)
        {
            try
            {
                raplaAppointmentPropertyDefinition = new ExtendedPropertyDefinition(DefaultExtendedPropertySet.Appointment, "isRaplaMeeting",
                        MapiPropertyType.Boolean);
            }
            catch (Exception e)
            {
                // property definition should not throw an exception as no web service is called
                throw new RaplaException(e.getMessage(), e);
            }
        }
    }

    public Logger getLogger()
    {
        return logger;
    }

    public Appointment getRaplaAppointment()
    {
        return raplaAppointment;
    }

    public void execute() throws Exception
    {
        SyncStatus status = appointmentTask.getStatus();
        switch (status)
        {
            case deleted:
                return;
            case synched:
                return;
            case toDelete:
                delete();
                appointmentTask.setStatus(SyncStatus.deleted);
                return;
            case toReplace:
                delete();
                appointmentTask.setStatus(SyncStatus.toUpdate);
            case toUpdate:
                addOrUpdate();
                appointmentTask.setStatus(SyncStatus.synched);
                return;
        }
    }

    /** This method holds the core functionality of the worker. It creates a {@link microsoft.exchange.webservices.data.Appointment} and saves its Exchange-Representation
     * (Appointment) to the Exchange Server.
     * @throws Exception
     */
    private void addOrUpdate() throws Exception
    {

        ewsConnector.test();
        long time = System.currentTimeMillis();
        Logger logger = getLogger().getChildLogger("exchangeupdate");
        logger.info("Updating appointment " + raplaAppointment);
        microsoft.exchange.webservices.data.Appointment exchangeAppointment = getEquivalentExchangeAppointment(raplaAppointment);
        if (isDeletedRecurrenceRemoved(exchangeAppointment, calcExceptionDates()))
        {
            delete();
        }
        exchangeAppointment = getEquivalentExchangeAppointment(raplaAppointment);

        //setExchangeRecurrence( );
        Repeating repeating = raplaAppointment.getRepeating();
        if (repeating != null)
        {
            Recurrence recurrence = getExchangeRecurrence(repeating);
            exchangeAppointment.setRecurrence(recurrence);
        }
        String messageBody = getMessageBody();
        exchangeAppointment.setBody(new MessageBody(BodyType.Text, messageBody));

        exchangeAppointment.setExtendedProperty(RAPLA_ID_PROPERTY_DEFINITION, appointmentTask.getAppointmentId());
        addPersonsAndResources(exchangeAppointment);
        saveToExchangeServer(exchangeAppointment, sendNotificationMail);
        // FIXME it an error occurs exceptions may not be serialized correctly
        removeRecurrenceExceptions(exchangeAppointment);
        logger.info("Updated appointment " + raplaAppointment + " took " + (System.currentTimeMillis() - time) + " ms ");
    }

    private synchronized void delete() throws Exception
    {
        String source = "2014-11-21+01:00";
        try
        {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'Z'");
            df.parse(source);
        }
        catch (ParseException ex)
        {
            int offset = 0;
            char offsetChar = '+';
            if (source.length() >= 10)
            {
                offsetChar = source.charAt(10);
                if (offsetChar == '+' || offsetChar == '-')
                {
                    String time = source.substring(11);
                    source = source.substring(0, 10);
                    Date timeString = new SimpleDateFormat("hh:mm").parse(time);
                    Calendar instance = Calendar.getInstance();
                    instance.setTime(timeString);
                    offset = instance.get(Calendar.HOUR_OF_DAY);
                }
            }
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            df.setTimeZone(TimeZone.getTimeZone("UTC" + offsetChar + offset));
            df.parse(source);
        }

        ewsConnector.test();

        String identifier = appointmentTask.getAppointmentId();
        Logger logger = getLogger().getChildLogger("exchangeupdate");
        long time = System.currentTimeMillis();
        logger.info("Deleting appointment with id " + identifier);
        try
        {
            ExchangeService service = ewsConnector.getService();
            microsoft.exchange.webservices.data.Appointment exchangeAppointment = getExchangeAppointmentByRaplaId(service, identifier);
            if (exchangeAppointment != null)
            {
                try
                {
                    getLogger().debug("Deleting  " + exchangeAppointment.getId().getUniqueId() + " " + exchangeAppointment.toString());
                    exchangeAppointment.delete(DeleteMode.HardDelete, SendCancellationsMode.SendToNone);
                }
                catch (ServiceResponseException e)
                {
                    getLogger().error(e.getMessage(), e);
                }
            }
        }
        catch (microsoft.exchange.webservices.data.ServiceResponseException e)
        {
            //can be ignored
        }
        //delete on the Exchange Server side
        //remove it from the "to-be-removed"-list
        logger.info("Delted appointment with id " + identifier + " took " + (System.currentTimeMillis() - time) + " ms ");
    }

    private void saveToExchangeServer(microsoft.exchange.webservices.data.Appointment exchangeAppointment, boolean notify) throws Exception
    {
        // save the appointment to the server
        if (exchangeAppointment.isNew())
        {
            getLogger().info("Adding " + exchangeAppointment.getSubject() + " to exchange");
            SendInvitationsMode sendMode = notify ? SendInvitationsMode.SendOnlyToAll : SendInvitationsMode.SendToNone;
            exchangeAppointment.save(sendMode);
        }
        else
        {
            getLogger().info("Updating " + exchangeAppointment.getId() + " " + exchangeAppointment.getSubject() + "," + exchangeAppointment.getWhen());
            SendInvitationsOrCancellationsMode sendMode = notify ? SendInvitationsOrCancellationsMode.SendOnlyToAll
                    : SendInvitationsOrCancellationsMode.SendToNone;
            exchangeAppointment.update(ConflictResolutionMode.AlwaysOverwrite, sendMode);
        }
    }

    private static microsoft.exchange.webservices.data.Appointment getExchangeAppointmentByRaplaId(ExchangeService service, String raplaId) throws Exception
    {
        try
        {
            final ItemView view = new ItemView(1);
            final SearchFilter searchFilter = new SearchFilter.ContainsSubstring(RAPLA_ID_PROPERTY_DEFINITION, raplaId);
            final FindItemsResults<Item> items = service.findItems(new FolderId(WellKnownFolderName.Calendar), searchFilter, view);
            if (items != null && items.isMoreAvailable())
            {
                final Item item = items.iterator().next();
                if (item instanceof microsoft.exchange.webservices.data.Appointment)
                {
                    return microsoft.exchange.webservices.data.Appointment.class.cast(item);
                }
                final ItemId id = item.getId();
                return microsoft.exchange.webservices.data.Appointment.bind(service, id);
            }
        }
        catch (ServiceResponseException e)
        {
        }
        return null;
    }

    private microsoft.exchange.webservices.data.Appointment getEquivalentExchangeAppointment(Appointment raplaAppointment) throws ArgumentOutOfRangeException,
            ArgumentException, Exception
    {
        ExchangeService service = ewsConnector.getService();
        microsoft.exchange.webservices.data.Appointment exchangeAppointment = null;
        if (exchangeAppointment == null)
        {
            final String raplaId = raplaAppointment.getId();
            exchangeAppointment = getExchangeAppointmentByRaplaId(service, raplaId);
        }
        if (exchangeAppointment == null)
        {
            exchangeAppointment = new microsoft.exchange.webservices.data.Appointment(service);
        }
        //exchangeAppointment.setICalUid( raplaAppointment.getId());

        Date start = raplaAppointment.getStart();
        Date end = raplaAppointment.getEnd();
        Date startDate = rapla2exchange(start);
        Date endDate = rapla2exchange(end);
        exchangeAppointment.setStart(startDate);
        //String[] availableIDs = TimeZone.getAvailableIDs();
        //        TimeZone timeZone = TimeZone.getTimeZone("Etc/UTC");//timeZoneConverter.getImportExportTimeZone();
        //        Collection<TimeZoneDefinition> serverTimeZones = service.getServerTimeZones();
        //        
        //        ArrayList list = new ArrayList();
        //        TimeZoneDefinition tDef = null;
        //        for ( TimeZoneDefinition def: serverTimeZones)
        //            
        //        {
        //            if ( def.getId().indexOf("Berlin")>=0 )
        //            {
        //                tDef = def;
        //                continue;
        //            }
        //        }
        String id = config.get(ExchangeConnectorConfig.EXCHANGE_TIMEZONE);
        String name = id;
        TimeZoneDefinition tDef = new MyTimeZoneDefinition(id, name);
        exchangeAppointment.setStartTimeZone(tDef);
        exchangeAppointment.setEnd(endDate);
        exchangeAppointment.setEndTimeZone(tDef);
        exchangeAppointment.setIsAllDayEvent(raplaAppointment.isWholeDaysSet());
        exchangeAppointment.setSubject(getName(raplaAppointment.getReservation(), Locale.GERMAN));
        exchangeAppointment.setIsResponseRequested(false);
        exchangeAppointment.setIsReminderSet(ExchangeConnectorConfig.DEFAULT_EXCHANGE_REMINDER_SET);
        exchangeAppointment.setLegacyFreeBusyStatus(LegacyFreeBusyStatus.valueOf(ExchangeConnectorConfig.DEFAULT_EXCHANGE_FREE_AND_BUSY));

        if (exchangeAppointment.isNew())
        {
            // add category for filtering
            exchangeAppointment.getCategories().add(config.get(ExchangeConnectorConfig.EXCHANGE_APPOINTMENT_CATEGORY));
            // add category for each event type
            exchangeAppointment.getCategories().add(raplaAppointment.getReservation().getClassification().getType().getName(Locale.getDefault()));
            // add rapla specific property
            exchangeAppointment.setExtendedProperty(raplaAppointmentPropertyDefinition, Boolean.TRUE);
        }
        return exchangeAppointment;
    }

    static class MyTimeZoneDefinition extends TimeZoneDefinition
    {
        public MyTimeZoneDefinition(String id, String name)
        {
            super();
            this.id = id;
            this.name = name;
        }

        @Override
        public void validate() throws ServiceLocalException
        {

        }
    }

    private Object getName(Reservation reservation, Locale locale)
    {
        String annotationName = reservation.getClassification().getType().getAnnotation(DynamicTypeAnnotations.KEY_NAME_FORMAT_EXPORT) != null ? DynamicTypeAnnotations.KEY_NAME_FORMAT_EXPORT
                : DynamicTypeAnnotations.KEY_NAME_FORMAT;
        String eventDescription = reservation.format(locale, annotationName);
        return eventDescription;
    }

    private Date rapla2exchange(Date date)
    {
        //        return new Date( date.getTime() - DateTools.MILLISECONDS_PER_HOUR);
        TimeZone timeZone = timeZoneConverter.getImportExportTimeZone();
        long time = date.getTime();
        int offset = 0;//TimeZoneConverterImpl.getOffset(timeZone, systemTimeZone, time);
        Date offsetToSystemTime = new Date(time + offset);
        Date exportDate = timeZoneConverter.fromRaplaTime(timeZone, offsetToSystemTime);
        getLogger().debug("Rapladate " + date + " converted to exchange " + exportDate);
        return exportDate;
        //Date exchangeDate = timeZoneConverter.fromRaplaTime(systemTimeZone, exportDate);
        //return exchangeDate;

        //		return exchangeDate ;
    }

    private Date exchange2rapla(Date date)
    {
        Date importDate = timeZoneConverter.toRaplaTime(systemTimeZone, date);
        TimeZone timeZone = timeZoneConverter.getImportExportTimeZone();
        Date raplaDate = timeZoneConverter.toRaplaTime(timeZone, importDate);
        return raplaDate;
    }

    protected static final String RAPLA_NOSYNC_KEYWORD = "<==8NO_SYNC8==>";
    protected static final String RAPLA_BODY_MESSAGE = "Please do not change this item, to prevent inconsistencies!\n\n\n";

    private String getMessageBody() throws Exception
    {
        String bodyAttendeeList = BODY_ATTENDEE_LIST_OPENING_LINE + getStringForRessources(raplaAppointment) + LINE_BREAK;
        String content = RAPLA_BODY_MESSAGE;
        content += bodyAttendeeList.isEmpty() ? "" : bodyAttendeeList;
        content += RAPLA_NOSYNC_KEYWORD;
        return content;
    }

    private String getStringForRessources(Appointment raplaAppointment)
    {
        StringBuilder result = new StringBuilder();
        // get all restricted resources
        Allocatable[] resources = raplaAppointment.getReservation().getAllocatablesFor(raplaAppointment);
        // join and check for mail address, if so, add to reservation
        for (Allocatable restrictedAllocatable : resources)
        {
            if (!restrictedAllocatable.isPerson())
            {
                final String name = restrictedAllocatable.getName(Locale.getDefault());
                result.append(name).append(LINE_BREAK);
            }
        }
        return result.toString();
    }

    private void addPersonsAndResources(microsoft.exchange.webservices.data.Appointment exchangeAppointment) throws ServiceLocalException, Exception
    {
        //final DynamicType roomType = getClientFacade().getDynamicTypes();
        // get all restricted resources
        final Allocatable[] allocatables = raplaAppointment.getReservation().getAllocatablesFor(raplaAppointment);
        // join and check for mail address, if so, add to reservation
        exchangeAppointment.getRequiredAttendees().clear();
        exchangeAppointment.getResources().clear();

        final List<String> locationList = new ArrayList<String>();

        for (Allocatable restrictedAllocatable : allocatables)
        {
            //String emailAttribute = config.get(ExchangeConnectorConfig.RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL);
            final Classification classification = restrictedAllocatable.getClassification();
            final String email = getEmail(classification);
            final String name = restrictedAllocatable.getName(Locale.getDefault());
            if (restrictedAllocatable.isPerson())
            {
                if (email != null && !email.equalsIgnoreCase(raplaUser.getEmail()))
                {
                    exchangeAppointment.getRequiredAttendees().add(name, email);
                    //                    if (ExchangeConnectorConfig.DEFAULT_EXCHANGE_EXPECT_RESPONSE)
                    //                        exchangeAppointment.setIsResponseRequested(true);
                }
            }
            else if (classification.getType().getAnnotation(DynamicTypeAnnotations.KEY_LOCATION, "false").equals("true"))
            {
                if (email != null)
                {
                    final Attendee attendee = new Attendee(email.toString());
                    attendee.setMailboxType(MailboxType.Mailbox);
                    attendee.setRoutingType("SMTP");
                    attendee.setName(name);
                    exchangeAppointment.getResources().add(attendee);
                }
                locationList.add(name);
            }
        }

        if (locationList.size() > 0)
        {
            StringBuilder location = new StringBuilder();
            for (String name : locationList)
            {
                location.append(name);
                if (locationList.size() > 1)
                {
                    location.append(", ");
                }
            }
            exchangeAppointment.setLocation(location.toString());
        }
    }

    private String getEmail(final Classification classification)
    {
        Attribute emailAttribute = getEmailAttribute(classification);
        final String email = emailAttribute != null ? classification.getValueAsString(emailAttribute, null) : null;
        if (email != null && email.isEmpty())
        {
            return null;
        }
        return email;
    }

    private Attribute getEmailAttribute(final Classification classification)
    {
        Attribute[] attributes = classification.getType().getAttributes();
        for (Attribute att : attributes)
        {
            String isEmail = att.getAnnotation(AttributeAnnotations.KEY_EMAIL);
            if (isEmail != null)
            {
                if (isEmail.equals("true"))
                {
                    return att;
                }
            }
            else if (att.getKey().equalsIgnoreCase("email"))
            {
                return att;
            }
        }
        return null;
    }

    private Recurrence getExchangeRecurrence(Repeating repeating) throws ArgumentOutOfRangeException, ArgumentException
    {
        final Recurrence returnVal;
        Calendar calendar = new GregorianCalendar();
        Date start = raplaAppointment.getStart();
        calendar.setTime(start);
        int dayOfMonthInt = calendar.get(Calendar.DAY_OF_MONTH);

        Month month = Month.values()[calendar.get(Calendar.MONTH)];
        RepeatingType type = repeating.getType();
        int interval = repeating.getInterval();
        if (type.is(RepeatingType.DAILY))
        {
            returnVal = new Recurrence.DailyPattern(start, interval);
        }
        else if (type.is(RepeatingType.WEEKLY))
        {
            DayOfTheWeek dayOfWeek = getDayOfWeek(calendar);
            returnVal = new Recurrence.WeeklyPattern(start, interval, dayOfWeek);
        }
        else if (type.is(RepeatingType.MONTHLY))
        {
            DayOfTheWeekIndex weekOfMonth = getWeekOfMonth(calendar);
            DayOfTheWeek dayOfWeek = getDayOfWeek(calendar);
            returnVal = new Recurrence.RelativeMonthlyPattern(start, interval, dayOfWeek, weekOfMonth);
        }
        else
        {
            returnVal = new Recurrence.YearlyPattern(start, month, dayOfMonthInt);
        }
        if (repeating.isFixedNumber())
        {
            returnVal.setNumberOfOccurrences(repeating.getNumber());
        }
        else
        {

            Date end = repeating.getEnd();
            if (end != null)
            {
                returnVal.setEndDate(rapla2exchange(DateTools.subDay(end)));
            }
            else
            {
                returnVal.neverEnds();
            }
        }
        return returnVal;
    }

    private DayOfTheWeek getDayOfWeek(Calendar calendar)
    {
        DayOfTheWeek dayOfWeek;
        {
            DayOfTheWeek[] values = DayOfTheWeek.values();
            int i = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            if (i < 0 || i >= values.length)
            {
                getLogger().error("Illegal exchange values for repeating in day of week " + values + " does not have index " + i);
                dayOfWeek = DayOfTheWeek.Monday;
            }
            else
            {
                dayOfWeek = values[i];
            }
        }
        return dayOfWeek;
    }

    private DayOfTheWeekIndex getWeekOfMonth(Calendar calendar)
    {
        DayOfTheWeekIndex weekOfMonth;
        {
            DayOfTheWeekIndex[] values = DayOfTheWeekIndex.values();
            int i = calendar.get(Calendar.WEEK_OF_MONTH) - 1;
            if (i < 0 || i >= values.length)
            {
                getLogger().error("Illegal exchange values for repeating in week of month " + values + " does not have index " + i);
                weekOfMonth = DayOfTheWeekIndex.First;
            }
            else
            {
                weekOfMonth = values[i];
            }
        }
        return weekOfMonth;
    }

    private boolean isDeletedRecurrenceRemoved(microsoft.exchange.webservices.data.Appointment exchangeAppointment, Set<Date> exceptionDates)
    {
        boolean result = false;
        try
        {
            exchangeAppointment.load(new PropertySet(AppointmentSchema.DeletedOccurrences));
            final DeletedOccurrenceInfoCollection deletedOccurrences = exchangeAppointment.getDeletedOccurrences();
            if(deletedOccurrences != null)
            {
                final Iterator<DeletedOccurrenceInfo> iterator = deletedOccurrences.iterator();
                while (iterator.hasNext())
                {
                    final DeletedOccurrenceInfo next = iterator.next();
                    final Date originalStart = DateTools.cutDate(exchange2rapla(next.getOriginalStart()));
                    if (!exceptionDates.contains(originalStart))
                    {
                        result = true;
                        break;
                    }
                }
            }
        }
        catch (Exception e)
        {
        }
        try
        {
            exchangeAppointment.load();
        }
        catch (Exception e)
        {
        }
        return result;
    }

    private void removeRecurrenceExceptions(microsoft.exchange.webservices.data.Appointment exchangeAppointment)
    {
        SortedSet<Date> exceptionDates = calcExceptionDates();
        if (exceptionDates.size() == 0)
        {
            return;
        }
        Date lastException = exceptionDates.last();
        try
        {
            ItemId id = exchangeAppointment.getId();
            int occurrenceIndex = 1;
            while (true)
            {
                ExchangeService service = ewsConnector.getService();
                microsoft.exchange.webservices.data.Appointment occurrence = microsoft.exchange.webservices.data.Appointment.bindToOccurrence(service, id,
                        occurrenceIndex);
                if (occurrence == null)
                {
                    break;
                }
                else
                {
                    occurrenceIndex++;
                }
                Date exchangeException = DateTools.cutDate(exchange2rapla(occurrence.getStart()));
                if (exchangeException.after(lastException))
                {
                    break;
                }
                //FIXME occurences wont get reinserted when exception is deleted in rapla
                if (exceptionDates.contains(exchangeException))
                {
                    getLogger().info("Removing exception for " + occurrence.getId().getUniqueId() + " " + occurrence.toString());
                    occurrence.delete(DeleteMode.MoveToDeletedItems, SendCancellationsMode.SendOnlyToAll);
                }
            }
        }
        catch (Exception e)
        {
            //Intended Exception
        }
    }

    private SortedSet<Date> calcExceptionDates()
    {
        SortedSet<Date> exceptionDates = new TreeSet<Date>();
        if (raplaAppointment.isRepeatingEnabled())
        {
            Date[] exceptions = raplaAppointment.getRepeating().getExceptions();
            for (Date exceptionDate : exceptions)
            {
                exceptionDates.add(DateTools.cutDate(exceptionDate));
            }
        }
        return exceptionDates;
    }

}
