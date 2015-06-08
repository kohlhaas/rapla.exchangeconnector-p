package org.rapla.plugin.exchangeconnector;

import java.util.HashMap;
import java.util.Map;

import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.framework.Configuration;
import org.rapla.framework.TypedComponentRole;

public interface ExchangeConnectorConfig 
{
    public static final TypedComponentRole<RaplaConfiguration> EXCHANGESERVER_CONFIG = new TypedComponentRole<RaplaConfiguration>("org.rapla.plugin.exchangeconnector.server.Config");
    
	public static final TypedComponentRole<I18nBundle> RESOURCE_FILE = new TypedComponentRole<I18nBundle>(ExchangeConnectorPlugin.class.getPackage().getName() + ".ExchangeConnectorResources");
	
	public static final TypedComponentRole<String> EXCHANGE_WS_FQDN = new TypedComponentRole<String>("ews_fqdn");
	public static final String DEFAULT_EXCHANGE_WS_FQDN = "https://myexchange.com";
	
	public static final TypedComponentRole<Integer> SYNCING_PERIOD_PAST = new TypedComponentRole<Integer>("exch-sync-past");
	public static final Integer DEFAULT_SYNCING_PERIOD_PAST = 30;
	
//	public static final TypedComponentRole<Integer> SYNCING_PERIOD_FUTURE = new TypedComponentRole<Integer>("exch-sync-future");
//	public static final Integer DEFAULT_SYNCING_PERIOD_FUTURE = 300;

	public static final TypedComponentRole<String> EXCHANGE_APPOINTMENT_CATEGORY  = new TypedComponentRole<String>( "exchange.default.category");
	public static final String DEFAULT_EXCHANGE_APPOINTMENT_CATEGORY = "RAPLA";
	
	public static final TypedComponentRole<String> EXCHANGE_TIMEZONE  = new TypedComponentRole<String>( "exchange.timezone");
	public static final String DEFAULT_EXCHANGE_TIMEZONE = "W. Europe Standard Time";

	public static final TypedComponentRole<Boolean> EXCHANGE_SEND_INVITATION_AND_CANCELATION  = new TypedComponentRole<Boolean>( "exchange.sendInvitationAndCancelation");
	public static final boolean DEFAULT_EXCHANGE_SEND_INVITATION_AND_CANCELATION = false;

	public static final boolean DEFAULT_EXCHANGE_REMINDER_SET = true;
	public static final String DEFAULT_EXCHANGE_FREE_AND_BUSY = "Busy";

//  public static final TypedComponentRole<String> EXPORT_EVENT_TYPE_KEY = new TypedComponentRole<String>("export-event-type-key");
//	public static final String DEFAULT_EXPORT_EVENT_TYPE = "//";
//	public static final TypedComponentRole<Boolean> ENABLED_BY_USER_KEY = new TypedComponentRole<Boolean>("exchangeconnector.userenabled");
//	public static final boolean DEFAULT_ENABLED_BY_USER = false;
//	public static final TypedComponentRole<String> USERNAME = new TypedComponentRole<String>("exchangeconnector.username");
//	public static final TypedComponentRole<String> PASSWORD = new TypedComponentRole<String>("exchangeconnector.password");
//	public static final TypedComponentRole<Boolean> SYNC_FROM_EXCHANGE_ENABLED_KEY = new TypedComponentRole<Boolean>("sync_from_exchange");
//	public static final boolean DEFAULT_SYNC_FROM_EXCHANGE_ENABLED = false;
//	public static final TypedComponentRole<Boolean> ENABLED_BY_ADMIN = new TypedComponentRole<Boolean>("exchange_connector_enabled_by_admin");
//	public static final boolean DEFAULT_ENABLED_BY_ADMIN = false;
//	public static final String PULL_FREQUENCY_KEY = "exch-pull-freq";
//	public static final Integer DEFAULT_PULL_FREQUENCY = 180;
//	public static final TypedComponentRole<String> IMPORT_EVENT_TYPE_KEY = new TypedComponentRole<String>("import-event-type-key");
//	public static final String RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE_KEY = "rapla.attr.title";
//	public static final String DEFAULT_RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE = "title";
//	public static final TypedComponentRole<String> RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL = new TypedComponentRole<String>("email.attr.title");
//	public static final String DEFAULT_RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL = "email";
//	public static final String EXCHANGE_ALWAYS_PRIVATE_KEY = "exchange.import.alwaysprivate";
//	public static final boolean DEFAULT_EXCHANGE_ALWAYS_PRIVATE = true;
//	public static final String EXCHANGE_REMINDER_SET_KEY = "exchange.reminder.set";
//	public static final String EXCHANGE_EXPECT_RESPONSE_KEY = "exchange.response.expected";
//	public static final boolean DEFAULT_EXCHANGE_EXPECT_RESPONSE = false;
//	public static final String EXCHANGE_FREE_AND_BUSY_KEY = "exchange.freeandbusy.mode";
//	public static final String EXCHANGE_INCOMING_FILTER_CATEGORY_KEY = "exchange.incoming.filter";
//	public static final String DEFAULT_EXCHANGE_INCOMING_FILTER_CATEGORY = "IMPORT-RAPLA";
//	public static final String DEFAULT_EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA = "Gebucht";
//	public static final String EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA_KEY = "rapla.private.text";
//	public static final int DEFAULT_EXCHANGE_FINDITEMS_PAGESIZE = 50;
//	public static final String EXCHANGE_FINDITEMS_PAGESIZE_KEY = "exchange.finditems.pagesize";
//	public static final  TypedComponentRole<String> ROOM_TYPE = new  TypedComponentRole<String>("rapla.room.type");
//	public static final String DEFAULT_ROOM_TYPE = "room";

	 public static class ConfigReader implements ExchangeConnectorConfig
	    {
	    	Configuration config;
		    Map<TypedComponentRole<?>,Object> map = new HashMap<TypedComponentRole<?>,Object>();
	    	public ConfigReader(Configuration config)
	    	{

		        load(config,EXCHANGE_WS_FQDN,DEFAULT_EXCHANGE_WS_FQDN);
		        loadInt(config,SYNCING_PERIOD_PAST,DEFAULT_SYNCING_PERIOD_PAST);
		        //loadInt(config,SYNCING_PERIOD_FUTURE,DEFAULT_SYNCING_PERIOD_FUTURE);
		        load(config,EXCHANGE_APPOINTMENT_CATEGORY,DEFAULT_EXCHANGE_APPOINTMENT_CATEGORY);
		        load(config,EXCHANGE_TIMEZONE,DEFAULT_EXCHANGE_TIMEZONE);
		        //loadBoolean(config,ENABLED_BY_ADMIN, DEFAULT_ENABLED_BY_ADMIN);
		        //loadInt(config,PULL_FREQUENCY_KEY,DEFAULT_PULL_FREQUENCY);
		        //load(config,IMPORT_EVENT_TYPE_KEY,DEFAULT_IMPORT_EVENT_TYPE);
		        //load(config,RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL,DEFAULT_RAPLA_EVENT_TYPE_ATTRIBUTE_EMAIL);
		        //load(config,RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE_KEY,DEFAULT_RAPLA_EVENT_TYPE_ATTRIBUTE_TITLE);
		        //load(config,EXCHANGE_FREE_AND_BUSY_KEY,DEFAULT_EXCHANGE_FREE_AND_BUSY);
		        //load(config,EXCHANGE_INCOMING_FILTER_CATEGORY_KEY,DEFAULT_EXCHANGE_INCOMING_FILTER_CATEGORY);
		        //loadBoolean(config,EXCHANGE_EXPECT_RESPONSE_KEY,DEFAULT_EXCHANGE_EXPECT_RESPONSE);
		        //loadBoolean(config,EXCHANGE_REMINDER_SET_KEY,DEFAULT_EXCHANGE_REMINDER_SET);
		        //loadInt(config,EXCHANGE_FINDITEMS_PAGESIZE_KEY,DEFAULT_EXCHANGE_FINDITEMS_PAGESIZE);
		        //load(config,EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA_KEY,DEFAULT_EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA);
		        //loadBoolean(config,EXCHANGE_APPOINTMENT_PRIVATE_NAME_IN_RAPLA_KEY,DEFAULT_EXCHANGE_ALWAYS_PRIVATE);
		    }

//	    	private void loadBoolean(Configuration config,TypedComponentRole<Boolean> key,
//	    			boolean defaultValue) {
//	    	    	boolean value = config.getChild(key.getId()).getValueAsBoolean(defaultValue);
//	    	    	map.put( key,value);
//	    	}
	    	    
	    	private void loadInt(Configuration config,TypedComponentRole<Integer> key,
	    			int defaultValue) {
	    		int value = config.getChild(key.getId()).getValueAsInteger(defaultValue);
	    	    map.put( key,value);
	    	}
	    	    
	    	private void load(Configuration config,TypedComponentRole<String> key,
	    			String defaultValue) {
	    		String id = key.getId();
				Configuration child = config.getChild(id);
				String value = child.getValue(defaultValue);
	    		map.put( key,value);
	    	}
	    	
	    	@SuppressWarnings("unchecked")
			public <T> T get( TypedComponentRole<T> key)
	    	{
	    		return (T) map.get(key);
	    	}
	    	
	    }
}
