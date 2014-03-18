package org.rapla.plugin.exchangeconnector.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rapla.components.util.DateTools;
import org.rapla.components.util.Predicate;
import org.rapla.components.util.TimeInterval;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.Configuration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;
import org.rapla.plugin.exchangeconnector.server.datastorage.SynchronizationTask;
import org.rapla.plugin.exchangeconnector.server.datastorage.SynchronizationTask.SyncStatus;
import org.rapla.plugin.exchangeconnector.server.exchange.AppointmentSynchronizer;
import org.rapla.server.RaplaKeyStorage;
import org.rapla.server.RaplaKeyStorage.LoginInfo;
import org.rapla.server.ServerExtension;
import org.rapla.storage.StorageOperator;
import org.rapla.storage.UpdateOperation;
import org.rapla.storage.UpdateResult;

public class SynchronisationManager extends RaplaComponent implements ModificationListener {
	ExchangeAppointmentStorage appointmentStorage;
	ExchangeConnectorConfig.ConfigReader config;
	RaplaKeyStorage keyStorage;

	public SynchronisationManager(RaplaContext context,Configuration config) throws RaplaException {
		super(context);
		
		final ClientFacade clientFacade =  context.lookup(ClientFacade.class);
		clientFacade.addModificationListener(this);
        keyStorage = context.lookup( RaplaKeyStorage.class);
        this.config  = new ExchangeConnectorConfig.ConfigReader(config);
        this.appointmentStorage = context.lookup( ExchangeAppointmentStorage.class);
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
			return new AppointmentSynchronizer(getContext(), config,task, appointment,user,username,password);
    	}
    	throw new RaplaException("No exchange username and password set for user " + user.getUsername());
    }

    public synchronized void synchronizeUser(User user) throws RaplaException {
    	Predicate<Reservation> predicate = getReservationSynced(user);
    	Collection<SynchronizationTask> tasks = new ArrayList<SynchronizationTask>();
    	try {
            TimeInterval syncRange = getSyncRange();
    		for ( SynchronizationTask task:appointmentStorage.getTasks(user,syncRange))
    		{
    			task.setStatus( SyncStatus.toDelete);
    			tasks.add( task);
    		}
    		final Reservation[] reservations = getClientFacade().getReservations(user, syncRange.getStart(), syncRange.getEnd(), null);
            for (Reservation reservation : reservations) {
            	if ( predicate.apply( reservation))
            	{
            		for (Appointment appointment : reservation.getAppointments()) {
                    	boolean toReplace = true;
						SynchronizationTask task = addOrUpdateAppointment(appointment, user, toReplace);
						tasks.add( task);
            		}
            	}
            }
            appointmentStorage.addOrReplace( tasks);
            execute( tasks);
        } catch (Exception e) {
            getLogger().error(e.getMessage(), e);
        }
    }

	protected Collection<SynchronizationTask> updateTasks(Map<Allocatable, User> users,Map<User,Predicate<Reservation>> reservationAllowedPredicates,Appointment appointment, boolean remove) throws RaplaException  {
		Collection<SynchronizationTask> tasks = new ArrayList<SynchronizationTask>();
		Allocatable[] allocatablesFor = appointment.getReservation().getAllocatablesFor(appointment);
		for (Allocatable allocatable : allocatablesFor) 
		{
			User user = users.get( allocatable);
         	if ( user != null)
         	{
         		if ( remove){
             		SynchronizationTask task = appointmentStorage.getTask( appointment,user);
                	if ( task != null)
                	{
                		task.setStatus( SyncStatus.toDelete);
                		tasks.add( task );
                	}
         		}
         		else
         		{
         			 if ( check( appointment))
         	         {
	             		Predicate<Reservation> p = reservationAllowedPredicates.get( user);
	             		if ( p.apply( appointment.getReservation() ))
	             		{
	             			SynchronizationTask task = addOrUpdateAppointment(appointment,user, false);
	             			tasks.add( task );
	             		}
         	         }
         		}
         	}
		}
		return tasks;
	}
		
    public synchronized void synchronize(UpdateResult evt) throws RaplaException {
        Map<Allocatable, User> users = getExchangeSyncUsers();
        Map<User,Predicate<Reservation>> reservationAllowedPredicates = new LinkedHashMap<User,Predicate<Reservation>>();
        for (User user:users.values())
        {
        	Predicate<Reservation> predicate = getReservationSynced(user);
        	reservationAllowedPredicates.put( user, predicate);
        }
        
        Collection<SynchronizationTask> tasks = new ArrayList<SynchronizationTask>();
        
        for (UpdateOperation operation: evt.getOperations())
		{
			RaplaObject current = operation.getCurrent();
			if ( current.getRaplaType() ==  Reservation.TYPE )
			{
				if ( operation instanceof UpdateResult.Remove)
				{
					Reservation oldReservation = (Reservation) current;
					for ( Appointment app: oldReservation.getAppointments() )
					{
						Collection<SynchronizationTask> result = updateTasks( users,reservationAllowedPredicates, app, true);
						tasks.addAll(result);
					}
				}
				if ( operation instanceof UpdateResult.Add)
				{
					Reservation newReservation = (Reservation) ((UpdateResult.Add) operation).getNew();
					for ( Appointment app: newReservation.getAppointments() )
					{
						Collection<SynchronizationTask> result =  updateTasks( users, reservationAllowedPredicates, app, false);
						tasks.addAll(result);
					}
				}
				if ( operation instanceof UpdateResult.Change)
				{
					Reservation oldReservation = (Reservation) ((UpdateResult.Change) operation).getOld();
					Reservation newReservation =(Reservation) ((UpdateResult.Change) operation).getNew();
					Appointment[] oldAppointments =  oldReservation.getAppointments();
					for ( Appointment oldApp: oldAppointments)
					{
						Collection<SynchronizationTask> result =  updateTasks( users,reservationAllowedPredicates, oldApp, true);
						tasks.addAll(result);
					}
					Appointment[] newAppointments =  newReservation.getAppointments();
					for ( Appointment newApp: newAppointments)
					{
						Collection<SynchronizationTask> result =  updateTasks( users,reservationAllowedPredicates, newApp, false);
						tasks.addAll(result);
					}
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

    public synchronized SynchronizationTask addOrUpdateAppointment(Appointment appointment,User user, boolean toReplace) throws RaplaException {
    	SynchronizationTask task = appointmentStorage.getTask( appointment,user);
    	if ( task == null)
    	{
    		task = appointmentStorage.createTask(appointment, user);
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
    
    private Map<Allocatable,User> getExchangeSyncUsers() throws RaplaException
    {
    	Map<Allocatable,User> result = new LinkedHashMap<Allocatable,User>();
    	final ClientFacade clientFacade = getClientFacade();
        final User[] users = clientFacade.getUsers();
        for (User user:users)
        {
        	Allocatable person = user.getPerson();
        	if ( person != null)
        	{
	        	LoginInfo secrets = keyStorage.getSecrets( user, "exchange");
	        	if ( secrets!= null)
	        	{
	        		result.put(person, user);
	        	}
        	}
        }
    	return result;
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
    
    private synchronized Predicate<Reservation> getReservationSynced(final User user) throws RaplaException {
    	Preferences preferences = getClientFacade().getPreferences(user, false);
        final String exportableTypes = preferences != null ? preferences.getEntryAsString(ExchangeConnectorConfig.EXPORT_EVENT_TYPE_KEY, null) : null;
    	return new Predicate<Reservation>()
    	{
			public boolean apply(Reservation reservation) {
				   final DynamicType reservationType = reservation.getClassification().getType();
			        if (exportableTypes == null) {
			            getLogger().debug("Skipping appointment of type " + reservationType + " because filter is not defined for appointment user " + user.getUsername());
			            return false;
			        } 
			        else 
			        {
			            if (!exportableTypes.contains(reservationType.getElementKey())) {
			                getLogger().debug("Skipping appointment of type " + reservationType + " because filtered out by user " + user.getUsername());
			                return false;
			            } else {
			                return true;
			            }
			        }
			}
    	};
    }


}
