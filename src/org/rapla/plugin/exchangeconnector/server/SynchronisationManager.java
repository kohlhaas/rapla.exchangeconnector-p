package org.rapla.plugin.exchangeconnector.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.rapla.components.util.DateTools;
import org.rapla.components.util.TimeInterval;
import org.rapla.entities.Entity;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.User;
import org.rapla.entities.configuration.CalendarModelConfiguration;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.facade.RaplaComponent;
import org.rapla.facade.internal.CalendarModelImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.SynchronizationTask.SyncStatus;
import org.rapla.plugin.exchangeconnector.server.exchange.AppointmentSynchronizer;
import org.rapla.server.RaplaKeyStorage;
import org.rapla.server.RaplaKeyStorage.LoginInfo;
import org.rapla.storage.StorageOperator;
import org.rapla.storage.UpdateOperation;
import org.rapla.storage.UpdateResult;


public class SynchronisationManager extends RaplaComponent implements ModificationListener {
	ExchangeAppointmentStorage appointmentStorage;
	ExchangeConnectorConfig.ConfigReader config;
	RaplaKeyStorage keyStorage;
	Map<String,List<CalendarModelImpl>> calendarModels = new HashMap<String,List<CalendarModelImpl>>();
	protected ReadWriteLock lock = new ReentrantReadWriteLock();

	public SynchronisationManager(RaplaContext context,Configuration config) throws RaplaException {
		super(context);
		
		final ClientFacade clientFacade =  context.lookup(ClientFacade.class);
		clientFacade.addModificationListener(this);
        keyStorage = context.lookup( RaplaKeyStorage.class);
        this.config  = new ExchangeConnectorConfig.ConfigReader(config);
        this.appointmentStorage = context.lookup( ExchangeAppointmentStorage.class);
        for ( User user : getClientFacade().getUsers())
        {
        	updateCalendarMap( user);
        }
        //final Timer scheduledDownloadTimer = new Timer("ScheduledDownloadThread",true);
        //scheduledDownloadTimer.schedule(new ScheduledDownloadHandler(context, clientFacade, getLogger()), 30000, ExchangeConnectorPlugin.PULL_FREQUENCY*1000);
	}
	
	public synchronized void dataChanged(ModificationEvent evt) throws RaplaException {
		synchronize((UpdateResult) evt);
	}
	
    
    private AppointmentSynchronizer createSyncronizer(SynchronizationTask task) throws RaplaException, EntityNotFoundException
    {
    	String userId = task.getUserId();
    	EntityResolver resolver = getContext().lookup(StorageOperator.class);
		User user = (User) resolver.resolve( userId); 
		LoginInfo secrets = keyStorage.getSecrets( user, "exchange");
    	if ( secrets != null)
    	{
    		String username = secrets.login;
    		String password = secrets.secret;
    		String appointmentId = task.getAppointmentId();
			// we don't resolve the appointment if we delete 
    		Appointment appointment = task.getStatus() != SyncStatus.toDelete  ? (Appointment) resolver.resolve( appointmentId) : null;
			Preferences preferences = getQuery().getPreferences( user);
			boolean notificationMail = preferences.getEntryAsBoolean( ExchangeConnectorConfig.EXCHANGE_SEND_INVITATION_AND_CANCELATION, ExchangeConnectorConfig.DEFAULT_EXCHANGE_SEND_INVITATION_AND_CANCELATION);
			return new AppointmentSynchronizer(getContext(), config,task, appointment,user,username,password, notificationMail);
    	}
    	throw new RaplaException("No exchange username and password set for user " + user.getUsername());
    }

//    public synchronized void synchronizeUser(User user)  {
//    	Collection<SynchronizationTask> tasks = new ArrayList<SynchronizationTask>();
//    	try {
//            TimeInterval syncRange = getSyncRange();
//    		for ( SynchronizationTask task:appointmentStorage.getTasks(user,syncRange))
//    		{
//    			task.setStatus( SyncStatus.toDelete);
//    			tasks.add( task);
//    		}
//    		final Reservation[] reservations = getClientFacade().getReservations(user, syncRange.getStart(), syncRange.getEnd(), null);
//            for (Reservation reservation : reservations) {
//            	if ( predicate.apply( reservation))
//            	{
//            		for (Appointment appointment : reservation.getAppointments()) {
//                    	boolean toReplace = true;
//						SynchronizationTask task = addOrUpdateAppointment(appointment, user, toReplace);
//						tasks.add( task);
//            		}
//            	}
//            }
//            appointmentStorage.addOrReplace( tasks);
//            execute( tasks);
//        } catch (Exception e) {
//            getLogger().error(e.getMessage(), e);
//        }
//    }

	protected Collection<SynchronizationTask> updateTasks(Appointment appointment, boolean remove) throws RaplaException  {
		Collection<SynchronizationTask> result = new ArrayList<SynchronizationTask>();
		if ( remove){
     		Collection<SynchronizationTask> taskList = appointmentStorage.getTasks( appointment);
        	for (SynchronizationTask task:taskList)
        	{
        		task.setStatus( SyncStatus.toDelete);
        		result.add( task );
        	}
 		}
 		else
 		{
 			 if ( check( appointment))
 	         {
 				 Collection<String> matchingUserIds = findMatchingUser( appointment);
 				 for( String userId:matchingUserIds)
 				 {
 					 SynchronizationTask task = addOrUpdateAppointment(appointment,userId, false);
 					 result.add( task );
 				 }
 	         }
 		}
		return result;
	}
		
    private Collection<String> findMatchingUser(Appointment appointment) throws RaplaException {
    	Set<String> result = new HashSet<String>();
		Lock lock = readLock();
		try	{
			for (String userId :calendarModels.keySet())
			{
				List<CalendarModelImpl> list = calendarModels.get(userId);
				for ( CalendarModelImpl conf:list)
				{
					if (conf.isMatchingSelectionAndFilter( appointment))
					{
						result.add( userId);
						break;
					}
				}
			}
			
		} finally {
			unlock( lock);
		}
		return result;
	}

	public synchronized void synchronize(UpdateResult evt) throws RaplaException {
        Collection<SynchronizationTask> tasks = new ArrayList<SynchronizationTask>();
        
        for (UpdateOperation operation: evt.getOperations())
		{
			Entity current = operation.getCurrent();
			if ( current.getRaplaType() ==  Reservation.TYPE )
			{
				if ( operation instanceof UpdateResult.Remove)
				{
					Reservation oldReservation = (Reservation) current;
					for ( Appointment app: oldReservation.getAppointments() )
					{
						Collection<SynchronizationTask> result = updateTasks( app, true);
						tasks.addAll(result);
					}
				}
				if ( operation instanceof UpdateResult.Add)
				{
					Reservation newReservation = (Reservation) ((UpdateResult.Add) operation).getNew();
					for ( Appointment app: newReservation.getAppointments() )
					{
						Collection<SynchronizationTask> result =  updateTasks(  app, false);
						tasks.addAll(result);
					}
				}
				if ( operation instanceof UpdateResult.Change)
				{
					Reservation oldReservation = (Reservation) ((UpdateResult.Change) operation).getOld();
					Reservation newReservation =(Reservation) ((UpdateResult.Change) operation).getNew();
					Set<Appointment> oldAppointments =  new HashSet<Appointment>(Arrays.asList(oldReservation.getAppointments()));
					Set<Appointment> newAppointments =  new HashSet<Appointment>(Arrays.asList(newReservation.getAppointments()));
					for ( Appointment oldApp: oldAppointments)
					{
						if ( newAppointments.contains( oldApp))
						{
							continue;
						}
						Collection<SynchronizationTask> result =  updateTasks(  oldApp, true);
						tasks.addAll(result);
					}
					for ( Appointment newApp: newAppointments)
					{
						boolean notChanged =false;
						if ( oldAppointments.contains( newApp))
						{
							for ( Appointment oldApp: oldAppointments)
							{
								if ( oldApp.equals( newApp))
								{
									if ( oldApp.matches( newApp))
									{
										Allocatable[] oldAllocatables = oldReservation.getAllocatablesFor( oldApp);
										Allocatable[] newAllocatables = newReservation.getAllocatablesFor( newApp);
										if (Arrays.equals( oldAllocatables,  newAllocatables))
										{
											notChanged = true;
										}
									}
								}
							}
						}
						if ( notChanged )
						{
							continue;
						}
						Collection<SynchronizationTask> result =  updateTasks(  newApp, false);
						tasks.addAll(result);
					}
				}
			}
			if ( current.getRaplaType() ==  Preferences.TYPE )
			{
				Preferences preferences = (Preferences)operation.getCurrent();
				if ( !(operation instanceof UpdateResult.Remove))
				{
					User owner = preferences.getOwner();
					Collection<SynchronizationTask> result =  updateCalendarMap(owner);
					tasks.addAll(result);
				}
			}
			if ( current.getRaplaType() ==  User.TYPE )
			{
				String userId = current.getId();
				Lock lock = writeLock();
				try	{
					calendarModels.remove( userId);
				} finally {
					unlock( lock);
				}
			}
		}

        evt.getAddObjects();
        if ( tasks.size() > 0)
		{
			appointmentStorage.addOrReplace( tasks);
			execute( tasks);
		}
    }

	protected Lock writeLock() throws RaplaException {
		return RaplaComponent.lock( lock.writeLock(), 60);
	}

	protected Lock readLock() throws RaplaException {
		return RaplaComponent.lock( lock.readLock(), 10);
	}

    private Collection<SynchronizationTask> updateCalendarMap(User user) throws RaplaException 
    {
    	Collection<SynchronizationTask> result = new ArrayList<SynchronizationTask>();
    	boolean createIfNotNull = false;
    	String userId = user.getId();
		Preferences preferences = getQuery().getPreferences(user, createIfNotNull);
		if ( preferences == null)
		{
			Lock lock = writeLock();
			try	{
				this.calendarModels.remove( userId);
			} finally {
				unlock( lock);
			}
			return result;
		}
		CalendarModelConfiguration modelConfig = preferences.getEntry(CalendarModelConfiguration.CONFIG_ENTRY);
        Map<String,CalendarModelConfiguration> exportMap= preferences.getEntry(CalendarModelConfiguration.EXPORT_ENTRY);
        if ( modelConfig == null && exportMap == null)
        {
        	Lock lock = writeLock();
			try	{
				this.calendarModels.remove( userId);
			} finally {
				unlock( lock);
			}
        	return result;
        }
        List<CalendarModelImpl> configList = new ArrayList<CalendarModelImpl>();
        if ( modelConfig!= null)
        {
        	if ( hasExchangeExport( modelConfig))
        	{
        		Collection<SynchronizationTask> updateTasks = updateTasks(user, modelConfig, configList);
				result.addAll(updateTasks);
        	}
        }
        
        if ( exportMap != null)
        {
        	for ( String key:exportMap.keySet())
        	{
        		CalendarModelConfiguration calendarModelConfiguration = exportMap.get( key);
        		if ( hasExchangeExport( modelConfig))
            	{
        			Collection<SynchronizationTask> updateTasks = updateTasks(user, calendarModelConfiguration, configList);
            		result.addAll(updateTasks);
            	}
        	}
        }
        Lock lock = writeLock();
		try	{
    		if ( configList.size() > 0)
            {
    			this.calendarModels.put( userId, configList);
            }
    		else
    		{
    			this.calendarModels.remove( userId);
    		}
		} finally {
			unlock( lock);
		}
		return result;
	}

	protected Collection<SynchronizationTask> updateTasks(User user,CalendarModelConfiguration modelConfig,List<CalendarModelImpl> configList) throws RaplaException {
		String userId = user.getId();
		
		List<SynchronizationTask> result = new ArrayList<SynchronizationTask>();
		CalendarModelImpl calendarModelImpl = new CalendarModelImpl(getContext(), user, getClientFacade());
		Map<String, String> alternativOptions = null;
		calendarModelImpl.setConfiguration( modelConfig, alternativOptions);
		configList.add( calendarModelImpl);
		TimeInterval syncRange = getSyncRange();
		Collection<Appointment> appointments = calendarModelImpl.getAppointments(syncRange);
		for ( Appointment app:appointments)
		{
			SynchronizationTask task = appointmentStorage.getTask(app, userId);
			// add new appointments to the appointment store, we don't need to check for updates here as this, will be triggered by a reservation change
			if ( task == null)
			{
				task = appointmentStorage.createTask(app, userId);
				result.add( task);
			}
		}
		return result;
	}

	private boolean hasExchangeExport(CalendarModelConfiguration modelConfig) {
		String option = modelConfig.getOptionMap().get(ExchangeConnectorPlugin.EXCHANGE_EXPORT);
		if ( option != null && option.equals("true"))
		{
			return true;
		}
		return false;
	}

	public synchronized SynchronizationTask addOrUpdateAppointment(Appointment appointment,String userId, boolean toReplace) throws RaplaException {
    	SynchronizationTask task = appointmentStorage.getTask( appointment,userId);
    	if ( task == null)
    	{
    		task = appointmentStorage.createTask(appointment, userId);
    	}
    	task.setStatus( toReplace ? SyncStatus.toReplace : SyncStatus.toUpdate);
    	return task;
    }
    
	public void execute(Collection<SynchronizationTask> tasks) throws RaplaException {
		for ( SynchronizationTask task:tasks)
		{
			 final AppointmentSynchronizer worker; 
			 try
			 {
				 worker = createSyncronizer(task);
			 } catch (EntityNotFoundException e) {
				 getLogger().warn( "Removing synchronize " + task + " due to " + e.getMessage() );
				 appointmentStorage.remove( task);
				 continue;
			 }
			try
	        {
	            worker.execute();
	        } catch (Exception e) {
	        	getLogger().warn( "Can't synchronize " + task , e );
	        }
		}
	}
        
    private TimeInterval getSyncRange()
    {
    	final ClientFacade clientFacade = getClientFacade();
    	Date today = clientFacade.today();
    	Date start = DateTools.addDays(today, -config.get(ExchangeConnectorConfig.SYNCING_PERIOD_PAST));
    	Date end = DateTools.addDays(today, config.get(ExchangeConnectorConfig.SYNCING_PERIOD_FUTURE));
    	return new TimeInterval(start, end);
    }

    private synchronized boolean check( Appointment appointment)  {
    	Date start = appointment.getStart();
		TimeInterval appointmentRange = new TimeInterval(start, appointment.getMaxEnd());
		TimeInterval syncRange = getSyncRange();
		if ( !syncRange.overlaps( appointmentRange))
		{
		    getLogger().debug("Skipping update of appointment " + appointment + " because is date of item is out of range");
	        return false;
		}
		else 
		{
			return true;
		}
    }

    

}
