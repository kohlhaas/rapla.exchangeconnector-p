package org.rapla.plugin.exchangeconnector;

import java.util.Calendar;
import java.util.Date;

import org.rapla.client.ClientService;
import org.rapla.client.ClientServiceContainer;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.facade.CalendarOptions;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.Configuration;
import org.rapla.framework.Container;
import org.rapla.framework.DefaultConfiguration;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.framework.TypedComponentRole;
import org.rapla.plugin.RaplaClientExtensionPoints;
import org.rapla.plugin.RaplaServerExtensionPoints;
import org.rapla.plugin.exchangeconnector.client.ExchangeConnectorAdminOptions;
import org.rapla.plugin.exchangeconnector.client.ExchangeConnectorUserOptions;
import org.rapla.plugin.exchangeconnector.server.ExchangeConnectorRemote;
import org.rapla.plugin.exchangeconnector.server.ExchangeConnectorRemoteObject;
import org.rapla.plugin.exchangeconnector.server.SynchronisationManager;
import org.rapla.server.ServerService;
import org.rapla.server.ServerServiceContainer;


public class ExchangeConnectorPlugin implements PluginDescriptor<ClientServiceContainer> {

	public final static boolean ENABLE_BY_DEFAULT = true;

    public static final String PLUGIN_CLASS = ExchangeConnectorPlugin.class.getName();
    public static final TypedComponentRole<I18nBundle> RESOURCE_FILE = new TypedComponentRole<I18nBundle>(ExchangeConnectorPlugin.class.getPackage().getName() + ".ExchangeConnectorResources");

    public static final String ENABLED_BY_USER_KEY = "exchange_connector_enabled_by_user";
    public static final boolean DEFAULT_ENABLED_BY_USER = false;

    public static final String SYNC_FROM_EXCHANGE_ENABLED_KEY = "sync_from_exchange";
    public static final boolean DEFAULT_SYNC_FROM_EXCHANGE_ENABLED = false;

    public static final String ENABLED_BY_ADMIN_KEY = "exchange_connector_enabled_by_admin";
    public static boolean ENABLED_BY_ADMIN;
    private static final boolean DEFAULT_ENABLED_BY_ADMIN = false;

    public static final String EXCHANGE_WS_FQDN_KEY = "ews_fqdn";
    public static String EXCHANGE_WS_FQDN;
    private static final String DEFAULT_EXCHANGE_WS_FQDN = "https://myexchange.com";


    public static final String SYNCING_PERIOD_PAST_KEY = "exch-sync-past";
    public static Integer SYNCING_PERIOD_PAST;
    private static final Integer DEFAULT_SYNCING_PERIOD_PAST = 5;

    public static final String SYNCING_PERIOD_FUTURE_KEY = "exch-sync-future";
    public static Integer SYNCING_PERIOD_FUTURE;
    private static final Integer DEFAULT_SYNCING_PERIOD_FUTURE = 5;

    public static final String PULL_FREQUENCY_KEY = "exch-pull-freq";
    public static Integer PULL_FREQUENCY;
    private static final Integer DEFAULT_PULL_FREQUENCY = 180;

    //admin
    public static final String IMPORT_EVENT_TYPE_KEY = "import-event-type-key";
    public static final String EXPORT_EVENT_TYPE_KEY = "export-event-type-key";
    public static String IMPORT_EVENT_TYPE;
    private static final String DEFAULT_IMPORT_EVENT_TYPE = "defaultReservation";
    public static final String DEFAULT_EXPORT_EVENT_TYPE = "//";

    //admin -> ICAL Plugin
    public static final String TIMEZONE_KEY = "timezone1";
    public static String TIMEZONE;
    public static final String DEFAULT_TIMEZONE = "Europe/Berlin"; //DateTools.getTimeZone().getID();

    //admin
    public static final String RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE_KEY = "rapla.attr.title";
    public static final String DEFAULT_RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE = "title";
    public static String RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE = "title";

    public static final boolean USE_JMS = false;

    //admin -> editierbare Combobox
    public static final String RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL_KEY = "email.attr.title";
    public static String RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL = "email";
    public static final String DEFAULT_RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL = "email";

    //user, checkbox

    public static final String EXCHANGE_REMINDER_SET_KEY = "exchange.reminder.set";
    public static final boolean  DEFAULT_EXCHANGE_REMINDER_SET = true;
    public static boolean  EXCHANGE_REMINDER_SET = DEFAULT_EXCHANGE_REMINDER_SET;


    /**
     *  user, Textfeld
     */
    public static final String EXCHANGE_APPOINTMENT_CATEGORY_KEY = "exchange.default.category";
    public static String EXCHANGE_APPOINTMENT_CATEGORY;
    private static final String DEFAULT_EXCHANGE_APPOINTMENT_CATEGORY = "DHBW";

    /** user option */
    public static final String EXCHANGE_EXPECT_RESPONSE_KEY = "exchange.response.expected";
    public static final boolean DEFAULT_EXCHANGE_EXPECT_RESPONSE = true;
    public static boolean EXCHANGE_EXPECT_RESPONSE = DEFAULT_EXCHANGE_EXPECT_RESPONSE;

    /** user, combobox mit werten LegacyFreeBusyStatus.values() */
    public static final String EXCHANGE_FREE_AND_BUSY_KEY = "exchange.freeandbusy.mode";
    // CK Removed this line to remove client side dependency from microsoft jars
    //  public static final String DEFAULT_EXCHANGE_FREE_AND_BUSY = Busy.name();
    public static final String DEFAULT_EXCHANGE_FREE_AND_BUSY = "Busy";
    public static String EXCHANGE_FREE_AND_BUSY = DEFAULT_EXCHANGE_FREE_AND_BUSY;

    /**
     * user option, textfeld
     */
    public static final String EXCHANGE_INCOMING_FILTER_CATEGORY_KEY = "exchange.incoming.filter";
    public static final String DEFAULT_EXCHANGE_INCOMING_FILTER_CATEGORY = "IMPORT-RAPLA";
    public static String EXCHANGE_INCOMING_FILTER_CATEGORY = "IMPORT-RAPLA";


    /**
     * user option, textfeld
     */
    public static final String DEFAULT_EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA = "Gebucht";
    public static final String EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA_KEY = "rapla.private.text";
    public static String EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA = "Gebucht";

    /**
     * admin option, int wert
     */
    public static final int DEFAULT_EXCHANGE_FINDITEMS_PAGESIZE = 50;
    public static final String EXCHANGE_FINDITEMS_PAGESIZE_KEY = "exchange.finditems.pagesize";
    public static int EXCHANGE_FINDITEMS_PAGESIZE = DEFAULT_EXCHANGE_FINDITEMS_PAGESIZE;

    /*
    admin option, combobox
     */
    public static final String RAPLA_ROOM_RESOURCE = "roomResource";

    public String toString() {
        return "Exchange-Connector";
    }

    /* (non-Javadoc)
      * @see org.rapla.framework.PluginDescriptor#provideServices(org.rapla.framework.Container, org.apache.avalon.framework.configuration.Configuration)
      */
    public void provideServices(ClientServiceContainer container, Configuration config) throws RaplaContextException {
        container.addContainerProvidedComponent(RESOURCE_FILE, I18nBundleImpl.class, I18nBundleImpl.createConfig(RESOURCE_FILE.getId()));
        container.addContainerProvidedComponent(RaplaClientExtensionPoints.PLUGIN_OPTION_PANEL_EXTENSION, ExchangeConnectorAdminOptions.class);
        if (config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT)) {
            loadConfigParameters(config);
            container.addContainerProvidedComponent(RaplaClientExtensionPoints.USER_OPTION_PANEL_EXTENSION, ExchangeConnectorUserOptions.class);
        }
    }

    public static void loadConfigParameters(Configuration config) {
        ENABLED_BY_ADMIN = config.getChild(ENABLED_BY_ADMIN_KEY).getValueAsBoolean(DEFAULT_ENABLED_BY_ADMIN);
        EXCHANGE_WS_FQDN = config.getChild(EXCHANGE_WS_FQDN_KEY).getValue(DEFAULT_EXCHANGE_WS_FQDN);
        SYNCING_PERIOD_PAST = config.getChild(SYNCING_PERIOD_PAST_KEY).getValueAsInteger(DEFAULT_SYNCING_PERIOD_PAST);
        SYNCING_PERIOD_FUTURE = config.getChild(SYNCING_PERIOD_FUTURE_KEY).getValueAsInteger(DEFAULT_SYNCING_PERIOD_FUTURE);
        PULL_FREQUENCY = config.getChild(PULL_FREQUENCY_KEY).getValueAsInteger(DEFAULT_PULL_FREQUENCY);
        IMPORT_EVENT_TYPE = config.getChild(IMPORT_EVENT_TYPE_KEY).getValue(DEFAULT_IMPORT_EVENT_TYPE);
        TIMEZONE = config.getChild(TIMEZONE_KEY).getValue(DEFAULT_TIMEZONE);

        RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL = config.getChild(RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL_KEY).getValue(DEFAULT_RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL);
        RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE= config.getChild(RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE_KEY).getValue(DEFAULT_RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE);

        EXCHANGE_APPOINTMENT_CATEGORY = config.getChild(EXCHANGE_APPOINTMENT_CATEGORY_KEY).getValue(DEFAULT_EXCHANGE_APPOINTMENT_CATEGORY);
        EXCHANGE_FREE_AND_BUSY = config.getChild(EXCHANGE_FREE_AND_BUSY_KEY).getValue(DEFAULT_EXCHANGE_FREE_AND_BUSY);
        EXCHANGE_INCOMING_FILTER_CATEGORY = config.getChild(EXCHANGE_INCOMING_FILTER_CATEGORY_KEY).getValue(DEFAULT_EXCHANGE_INCOMING_FILTER_CATEGORY);
        EXCHANGE_EXPECT_RESPONSE = config.getChild(EXCHANGE_EXPECT_RESPONSE_KEY).getValueAsBoolean(DEFAULT_EXCHANGE_EXPECT_RESPONSE);
        EXCHANGE_REMINDER_SET = config.getChild(EXCHANGE_REMINDER_SET_KEY).getValueAsBoolean(DEFAULT_EXCHANGE_REMINDER_SET);
        EXCHANGE_FINDITEMS_PAGESIZE = config.getChild(EXCHANGE_FINDITEMS_PAGESIZE_KEY).getValueAsInteger(DEFAULT_EXCHANGE_FINDITEMS_PAGESIZE);
        EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA= config.getChild(EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA_KEY).getValue(DEFAULT_EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA);
    }

    public static void storeParametersToConfig(DefaultConfiguration newConfig)  {
        newConfig.getMutableChild(ExchangeConnectorPlugin.ENABLED_BY_ADMIN_KEY, true).setValue(ENABLED_BY_ADMIN);
        newConfig.getMutableChild(ExchangeConnectorPlugin.EXCHANGE_WS_FQDN_KEY, true).setValue(EXCHANGE_WS_FQDN);
        newConfig.getMutableChild(ExchangeConnectorPlugin.SYNCING_PERIOD_PAST_KEY, true).setValue(SYNCING_PERIOD_PAST);
        newConfig.getMutableChild(ExchangeConnectorPlugin.SYNCING_PERIOD_FUTURE_KEY, true).setValue(SYNCING_PERIOD_FUTURE);
        newConfig.getMutableChild(ExchangeConnectorPlugin.PULL_FREQUENCY_KEY, true).setValue(PULL_FREQUENCY);
        newConfig.getMutableChild(ExchangeConnectorPlugin.IMPORT_EVENT_TYPE_KEY, true).setValue(IMPORT_EVENT_TYPE);
        newConfig.getMutableChild(ExchangeConnectorPlugin.TIMEZONE_KEY, true).setValue(TIMEZONE);
        newConfig.getMutableChild(ExchangeConnectorPlugin.EXCHANGE_INCOMING_FILTER_CATEGORY_KEY, true).setValue(EXCHANGE_INCOMING_FILTER_CATEGORY);

        newConfig.getMutableChild(ExchangeConnectorPlugin.RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL_KEY, true).setValue(RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL);
        newConfig.getMutableChild(ExchangeConnectorPlugin.RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE_KEY, true).setValue(RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE);

        newConfig.getMutableChild(ExchangeConnectorPlugin.EXCHANGE_APPOINTMENT_CATEGORY_KEY, true).setValue(EXCHANGE_APPOINTMENT_CATEGORY);
        newConfig.getMutableChild(ExchangeConnectorPlugin.EXCHANGE_EXPECT_RESPONSE_KEY, true).setValue(EXCHANGE_EXPECT_RESPONSE);
        newConfig.getMutableChild(ExchangeConnectorPlugin.EXCHANGE_REMINDER_SET_KEY, true).setValue(EXCHANGE_REMINDER_SET);
        newConfig.getMutableChild(ExchangeConnectorPlugin.EXCHANGE_FREE_AND_BUSY_KEY, true).setValue(EXCHANGE_FREE_AND_BUSY);
        newConfig.getMutableChild(ExchangeConnectorPlugin.EXCHANGE_FINDITEMS_PAGESIZE_KEY, true).setValue(EXCHANGE_FINDITEMS_PAGESIZE);
        newConfig.getMutableChild(ExchangeConnectorPlugin.EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA_KEY, true).setValue(EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA);

    }

    public static DynamicType getImportEventType(ClientFacade currentClientFacade) throws RaplaException {
        final DynamicType[] dynamicTypes = currentClientFacade.getDynamicTypes(DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION);
        for (DynamicType dynamicType : dynamicTypes) {
            if (dynamicType.getElementKey().equals(ExchangeConnectorPlugin.IMPORT_EVENT_TYPE)) {
                return dynamicType;

            }
        }

        return null;
    }




    /**
     * returns synching period with respect to given date
     *
     * @param date given, @not null
     * @return returns date reduced by SYNCHING_PERIOD_PAST
     */
    public static Date getSynchingPeriodPast(Date date) {
        final Calendar from = Calendar.getInstance();
        from.setTime(date);
        from.set(Calendar.HOUR, 0);
        from.set(Calendar.MINUTE, 0);
        from.set(Calendar.SECOND, 0);
        from.add(Calendar.DAY_OF_YEAR, -ExchangeConnectorPlugin.SYNCING_PERIOD_PAST);
        return from.getTime();
    }

    public static Date getSynchingPeriodFuture(Date date) {
        final Calendar to = Calendar.getInstance();
        to.setTime(date);
        to.set(Calendar.HOUR, 23);
        to.set(Calendar.MINUTE, 59);
        to.set(Calendar.SECOND, 59);

        to.add(Calendar.DAY_OF_YEAR, ExchangeConnectorPlugin.SYNCING_PERIOD_FUTURE);

        return to.getTime();
    }

    public static boolean isAllDayEvent(Appointment appointment, CalendarOptions calendarOptions) {
        if (appointment.isWholeDaysSet())
            return true;

        //interpret that going from start of workingTime to end of working is the same.
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(appointment.getStart());
        int startHour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        calendar.setTime(appointment.getEnd());
        int endHour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        boolean startsOnWorkTimeStart = startHour == calendarOptions.getWorktimeStart();
        boolean endsOnWorkTimeEnd = endHour == calendarOptions.getWorktimeEnd();

        return startsOnWorkTimeStart && endsOnWorkTimeEnd;
    }


}
