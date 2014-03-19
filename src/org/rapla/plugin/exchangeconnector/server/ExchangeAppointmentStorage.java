package org.rapla.plugin.exchangeconnector.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.rapla.entities.Entity;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.SynchronizationTask.SyncStatus;
import org.rapla.storage.StorageOperator;



/**
 * This singleton class provides the functionality to save data related to the {@link ExchangeConnectorPlugin}. This includes
 * - the mapping between Rapla {@link Appointment}s and Exchange {@link microsoft.exchange.webservices.data.Appointment}s
 * - the information if the appointment originates from Rapla or from the Exchange Server
 * - a list of all appointments which have been deleted in the Rapla system but for some reason have not been deleted from the Exchange Server (hence they can be deleted later)   
 * 
 * @author Dominik Joder
 * @see {@link ExchangeAppointmentStorageObject}
 * @see {@link SynchronisationManager}
 * @see {@link ExchangeConnectorPlugin}
 */
public class ExchangeAppointmentStorage extends RaplaComponent {
	Map<String,Set<SynchronizationTask>> tasks =  new LinkedHashMap<String,Set<SynchronizationTask>>();
	StorageOperator operator;
	//private static String DEFAULT_STORAGE_FILE_PATH = "data/exchangeConnector.dat";
//	private String storageFilePath = DEFAULT_STORAGE_FILE_PATH;
	protected ReadWriteLock lock = new ReentrantReadWriteLock();
	/**
	 * public constructor of the class to read a particular file

	 * @param filePath : {@link String} absolute path to the file which saves both the mapping table and the "to delete" list
	 */
	public ExchangeAppointmentStorage(RaplaContext context)  throws RaplaException {
		super(context);
		operator = context.lookup(StorageOperator.class);
		DynamicType dynamicType = operator.getDynamicType( StorageOperator.SYNCHRONIZATIONTASK_TYPE);
		Attribute appointmentIdAtt = dynamicType.getAttribute("objectId");
		Attribute exchangeAppointmentIdAtt = dynamicType.getAttribute("externalObjectId");
		Attribute statusAtt = dynamicType.getAttribute("status");
		ClassificationFilter newClassificationFilter = dynamicType.newClassificationFilter();
        Collection<Allocatable> store = operator.getAllocatables( newClassificationFilter.toArray());        
		for ( Allocatable persistant:store)
		{
			User user = persistant.getOwner();
			String appointmentId = persistant.getClassification().getValueAsString(appointmentIdAtt, null);
			String exchangeAppointmentId = persistant.getClassification().getValueAsString(exchangeAppointmentIdAtt, null);
			String status = persistant.getClassification().getValueAsString(statusAtt, null);
			if ( user == null)
			{
				getLogger().error("Synchronization task " + persistant.getId() +  " has no userId. Ignoring.");
				continue;
			}
			SynchronizationTask synchronizationTask = new SynchronizationTask(appointmentId, user.getId());
			if ( exchangeAppointmentId != null)
			{
				synchronizationTask.setExchangeAppointmentId( exchangeAppointmentId);
			}
			if ( status == null)
			{
				getLogger().error("Synchronization task " + persistant.getId() +  " has no status. Ignoring.");
				continue;
			}
			synchronizationTask.setPersistantId( persistant.getId());
			SyncStatus valueOf = SyncStatus.valueOf( status);
			if ( valueOf == null)
			{
				getLogger().error("Synchronization task " + persistant.getId() +  " has unsupported status '" +  status +"'. Ignoring.");
				continue;
			}
			synchronizationTask.setStatus( valueOf);
			Set<SynchronizationTask> taskList = tasks.get( appointmentId);
			if ( taskList == null)
			{
				taskList = new HashSet<SynchronizationTask>();
				tasks.put( appointmentId, taskList);
			}
			taskList.add( synchronizationTask);
		}
	}
	
	protected Lock writeLock() throws RaplaException {
		return RaplaComponent.lock( lock.writeLock(), 60);
	}

	protected Lock readLock() throws RaplaException {
		return RaplaComponent.lock( lock.readLock(), 10);
	}
	
	
	synchronized public SynchronizationTask getTask(Appointment appointment, String userId) throws RaplaException {
		String appointmentId = appointment.getId();
		Lock lock = readLock();
		try
		{
			Set<SynchronizationTask> set = tasks.get(appointmentId);
			if ( set != null)
			{
				for (SynchronizationTask task: set)
				{
					String taskUserId = task.getUserId();
					if (userId == taskUserId || (userId!= null && userId.equals( taskUserId)))
					{
						return task;
					}
				}
			}
		} 
		finally
		{
			unlock( lock);
		}
		return null;
	}
	
	public Collection<SynchronizationTask> getTasks(Appointment appointment) throws RaplaException {
		String appointmentId = appointment.getId();
		Lock lock = readLock();
		try
		{
			Set<SynchronizationTask> set = tasks.get(appointmentId);
			return new ArrayList<SynchronizationTask>( set);
		}
		finally
		{
			unlock( lock);
		}
	}
	
	synchronized public SynchronizationTask createTask(Appointment appointment, String userId) 
	{
		return new SynchronizationTask( appointment.getId(), userId);
	}

	
	
	// FIXME implement sync range
//	synchronized public Collection<SynchronizationTask> getTasks(User user, TimeInterval syncRange) {
//		Collection<SynchronizationTask> result = new ArrayList<SynchronizationTask>();
//		for (SynchronizationTask task: tasks)
//		{
//			if (task.matches( user))
//			{
//				result.add(task);
//			}
//		}
//		return result;
//	}
	
	synchronized public void addOrReplace(Collection<SynchronizationTask> toStore) throws RaplaException 
	{
		Lock lock = writeLock();
		try
		{
			for (SynchronizationTask task: toStore)
			{
				String appointmentId = task.getAppointmentId();
				Set<SynchronizationTask> set = tasks.get(appointmentId);
				if ( set != null)
				{
					set.remove( task);
				}
				else
				{
					set = new HashSet<SynchronizationTask>();
					tasks.put( appointmentId, set);
				}
				set.add( task);
			}
		} 
		finally
		{
			unlock( lock);
		}
		Collection<SynchronizationTask> toRemove = Collections.emptyList();
		storeAndRemove( toStore, toRemove);
	}
	
	synchronized public void remove(SynchronizationTask appointmentTask) throws RaplaException {
		String appointmentId = appointmentTask.getAppointmentId();
		boolean remove = false;
		Lock lock = writeLock();
		try
		{
			Set<SynchronizationTask> set = tasks.get(appointmentId);
			if ( set != null)
			{
				remove = set.remove( appointmentTask);
			}
		} 
		finally
		{
			unlock( lock);
		}
		if ( remove)
		{
			Collection<SynchronizationTask> toStore = Collections.emptyList();
			Collection<SynchronizationTask> toRemove = Collections.singletonList(appointmentTask);
			storeAndRemove( toStore,toRemove);
		}
	}
	
	synchronized public void changeStatus(SynchronizationTask task,SyncStatus newStatus) throws RaplaException {
		task.setStatus( newStatus );
		List<SynchronizationTask> toStore = Collections.singletonList(task);
		List<SynchronizationTask> toRemove = Collections.emptyList();
		storeAndRemove( toStore,toRemove);
	}
	
	private void storeAndRemove(Collection<SynchronizationTask> toStore, Collection<SynchronizationTask> toRemove) throws RaplaException
	{
		Collection<Entity> storeObjects = new HashSet<Entity>();
		Collection<Entity> removeObjects = new HashSet<Entity>();
		for ( SynchronizationTask task:toRemove)
		{
			String persistantId = task.getPersistantId();
			if ( persistantId != null)
			{
				Entity persistant = operator.tryResolve( persistantId);
				if ( persistant != null)
				{
					removeObjects.add( persistant);
				}
			}
		}
		for ( SynchronizationTask task:toStore)
		{
			String persistantId = task.getPersistantId();
			Classification newClassification = null;
			if ( persistantId != null)
			{
				Entity persistant = operator.tryResolve( persistantId);
				if ( persistant != null)
				{
					Set<Entity> singleton = Collections.singleton( persistant);
					Collection<Entity> editObjects = operator.editObjects( singleton, null);
					Allocatable editable = (Allocatable) editObjects.iterator().next();
					newClassification = editable.getClassification();
					storeObjects.add( editable);
				}
			}
			else
			{
				DynamicType dynamicType = operator.getDynamicType( StorageOperator.SYNCHRONIZATIONTASK_TYPE);
				newClassification = dynamicType.newClassification(); 
				Allocatable newObject = getModification().newAllocatable(newClassification, null );
				String userId = task.getUserId();
				task.setPersistantId( newObject.getId());
				User owner = (User) operator.tryResolve(userId);
				if ( owner == null)
				{
					getLogger().error("User for id " + userId + " not found. Ignoring appointmentTask for appointment " + task.getAppointmentId());
					continue;
				}
				else
				{	
					newObject.setOwner(owner);
					storeObjects.add( newObject);
				}
			}
			if ( newClassification != null)
			{
				newClassification.setValue("objectId", task.getAppointmentId());
				newClassification.setValue("externalObjectId", task.getExchangeAppointmentId());
				newClassification.setValue("status", task.getStatus().name());
			}

		}
		User user = null;
		operator.storeAndRemove(storeObjects, removeObjects, user);
	}

	
	
	
//	/**
//	 * loads the file into the mapping table
//	 * @return {@link Boolean} true if loading the serialized file succeeded
//	 */
//	@SuppressWarnings("unchecked")
//	public boolean load() {
//		try {
//			File file = new File(storageFilePath );
//			FileInputStream fileInputStream = new FileInputStream(file);
//			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
//			Object appointmentsObject = objectInputStream.readObject();
//			if(appointmentsObject.getClass().equals(tasks.getClass()))
//				tasks = (Collection<SynchronizationTask>) appointmentsObject;
//			objectInputStream.close();
//			fileInputStream.close();
//			return true;
//		} catch (Exception e) {
//			return false;
//		}
//	}
	
	/**
	 * save all data (the mapping table) to a serialized file
	 * @return {@link Boolean} true if saving the serialized file succeeded
	 */
//	public boolean save() {
//		try {
//			File file = new File(storageFilePath );
//			FileOutputStream fileOutputStream = new FileOutputStream(file);
//			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
//			objectOutputStream.writeObject(tasks);
//			objectOutputStream.close();
//			fileOutputStream.close();
//			return true;
//		} catch (IOException e) {
//			return false;
//		}
//	}
}