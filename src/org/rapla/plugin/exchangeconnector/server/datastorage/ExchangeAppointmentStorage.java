package org.rapla.plugin.exchangeconnector.server.datastorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.rapla.components.util.TimeInterval;
import org.rapla.entities.User;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.storage.RefEntity;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.Configuration;
import org.rapla.framework.RaplaContext;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.ExchangeConnectorUtils;
import org.rapla.plugin.exchangeconnector.server.SynchronisationManager;
import org.rapla.plugin.exchangeconnector.server.datastorage.SynchronizationTask.SyncStatus;

import edu.emory.mathcs.backport.java.util.Collections;


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
	private String storageFilePath = "";
	private static String DEFAULT_STORAGE_FILE_PATH = "data/exchangeConnector.dat";
	Collection<SynchronizationTask> tasks =  new LinkedHashSet<SynchronizationTask>();
	
	/**
	 * public constructor of the class to read a particular file

	 * @param filePath : {@link String} absolute path to the file which saves both the mapping table and the "to delete" list
	 */
	public ExchangeAppointmentStorage(RaplaContext context, Configuration config) {
		super(context);
		storageFilePath = DEFAULT_STORAGE_FILE_PATH;
		load();
	}
	
	synchronized public SynchronizationTask getTask(Appointment appointment, User user) {
		for (SynchronizationTask task: tasks)
		{
			if (task.matches(appointment, user))
			{
				return task;
			}
		}
		return null;
	}
	
	// FIXME implement syncrange
	synchronized public Collection<SynchronizationTask> getTasks(User user, TimeInterval syncRange) {
		Collection<SynchronizationTask> result = new ArrayList<SynchronizationTask>();
		for (SynchronizationTask task: tasks)
		{
			if (task.matches( user))
			{
				result.add(task);
			}
		}
		return result;
	}
	
	synchronized public void addOrReplace(Collection<SynchronizationTask> tasks) 
	{
		tasks.addAll( tasks);
		save();
	}
	
	synchronized public void remove(SynchronizationTask appointmentTask) {
		boolean remove = tasks.remove( appointmentTask);
		if ( remove )
		{
			save();
		}
	}
	
	synchronized public void changeStatus(SynchronizationTask task,SyncStatus newStatus) {
		task.setStatus( newStatus );
		save();
	}


	
	/**
	 * loads the file into the mapping table
	 * @return {@link Boolean} true if loading the serialized file succeeded
	 */
	@SuppressWarnings("unchecked")
	public boolean load() {
		try {
			File file = new File(storageFilePath );
			FileInputStream fileInputStream = new FileInputStream(file);
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
			Object appointmentsObject = objectInputStream.readObject();
			if(appointmentsObject.getClass().equals(tasks.getClass()))
				tasks = (Collection<SynchronizationTask>) appointmentsObject;
			objectInputStream.close();
			fileInputStream.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * save all data (the mapping table) to a serialized file
	 * @return {@link Boolean} true if saving the serialized file succeeded
	 */
	public boolean save() {
		try {
			File file = new File(storageFilePath );
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
			objectOutputStream.writeObject(tasks);
			objectOutputStream.close();
			fileOutputStream.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	



	

	/**
	 * get all Exchange {Appointment}s which have been created on the Exchange Server
	 * @return {@link HashSet} of {@link Appointment}s which have been created on the Exchange Server
	 */
//	public HashSet<Appointment> getExchangeItems() {
//		HashSet<Appointment> returnAppointments = new HashSet<Appointment>();
//		for (ExchangeAppointmentStorageObject appointmentStorage : appointments.values()) {
//			if(appointmentStorage.isExchangeItem()) {
//				Appointment appointment = ExchangeConnectorUtils.getAppointmentById(appointmentStorage.getAppointmentId(), getClientFacade());
//				if(appointment != null)
//					returnAppointments.add(appointment);
//			}
//		}
//		return returnAppointments;
//	}
//
//
//	public Collection<Integer> getExchangeItemIds() {
//		HashSet<Integer> exchangeAppointmentIds = new HashSet<Integer>();
//		for (Integer key : appointments.keySet()) {
//			if (isExternalAppointment(key)) {
//				exchangeAppointmentIds.add(key);
//			}
//		}
//		return exchangeAppointmentIds;
//	}

	

	
//	public boolean isRaplaItem(String exchangeId) {
//		for (Integer appointmentId : getAppointmentIds(exchangeId)) {
//			ExchangeAppointmentStorageObject appointmentStorageObject = appointments.get(appointmentId);
//			if(appointmentStorageObject != null && !appointmentStorageObject.isExchangeItem())
//				return true;
//		}
//		return false;
//	}


//	public void setAllDeleted() {
//		for (ExchangeAppointmentStorageObject storageObject : this.appointments.values()) {
//			if (!storageObject.isExchangeItem()) {
//				storageObject.setDeleted();
//			}
//		}
//	}
	
//	public void removeExchangeItems() {
//		Collection<Integer> appointmentStorageObjectsKeys = getExchangeItemIds();
//		for (Integer appointmentStorageObjectKey: appointmentStorageObjectsKeys) {
//			appointments.remove(appointmentStorageObjectKey);
//		}
//	}
//

	
//	/**
//	 * check if a given Rapla {@link Appointment} originates from the Exchange Server
//	 * 
//	 * @param appointment : {@link Appointment} the Rapla appointment to be found in the mapping table
//	 * @return {@link Boolean} true if the appointment has been created in Outlook/Exchange, false if it has been created in Rapla
//	 */
//	public boolean isExternalAppointment(Appointment appointment) {
//		return isExternalAppointment(ExchangeConnectorUtils.getAppointmentID(appointment));
//	}
//	/**
//	 * check if a given Rapla {@link Appointment} originates from the Exchange Server
//	 * 
//	 * @param appointmentId : {@link Integer} unique id of the Rapla {@link Appointment} the Rapla appointment to be found in the mapping table
//	 * @return {@link Boolean} true if the appointment has been created in Outlook/Exchange, false if it has been created in Rapla
//	 */
//	public boolean isExternalAppointment(int appointmentId) {
//		ExchangeAppointmentStorageObject appointmentStorage = appointments.get(appointmentId);
//		return (appointmentStorage == null)?false:appointmentStorage.isExchangeItem();
//	}
//	


}