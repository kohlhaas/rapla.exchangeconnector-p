package org.rapla.plugin.exchangeconnector.server.datastorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.rapla.entities.domain.Appointment;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.ExchangeConnectorUtils;
import org.rapla.plugin.exchangeconnector.server.SynchronisationManager;

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
public class ExchangeAppointmentStorage {
	private String storageFilePath = "";
	private static String DEFAULT_STORAGE_FILE_PATH = "data/exchangeConnector.dat";
	
	private HashMap<Integer, ExchangeAppointmentStorageObject> appointments = new HashMap<Integer, ExchangeAppointmentStorageObject>();
	
	private static ExchangeAppointmentStorage SINGLETON_INSTANCE;
	
	/**
	 * @return the singleton of {@link ExchangeAppointmentStorage}
	 */
	public static ExchangeAppointmentStorage getInstance(){
		if(SINGLETON_INSTANCE == null)
			SINGLETON_INSTANCE = new ExchangeAppointmentStorage();
		
		return SINGLETON_INSTANCE;
	}
	
	
	/**
	 * private constructor of the class to read the storage file
	 */
	private ExchangeAppointmentStorage() {
		this(DEFAULT_STORAGE_FILE_PATH);
	}
	
	/**
	 * public constructor of the class to read a particular file

	 * @param filePath : {@link String} absolute path to the file which saves both the mapping table and the "to delete" list
	 */
	public ExchangeAppointmentStorage(String filePath) {
		storageFilePath = filePath;
		load();
		
		// remember this reference (forget old reference)
		SINGLETON_INSTANCE = this;
	}
	
	/**
	 * add an appointment with its unique Rapla {@link Appointment}, its unique Exchange {@link microsoft.exchange.webservices.data.Appointment} id and the flag if it originated from the Exchange Server in the mapping table.
	 * @param appointment : {@link Appointment} the Rapla appointment
	 * @param exchangeId : {@link String} the Exchange appointment id
	 * @param raplaUsername : {@link String} the name of the Rapla user that owns the appointment
	 * @param isExchangeItem : {@link Boolean} true if the appointment has been created in Outlook/Exchange, false if it has been created in Rapla
	 */
	public void addAppointment(Appointment appointment, String exchangeId, String raplaUsername, boolean isExchangeItem) {

        addAppointment(ExchangeConnectorUtils.getAppointmentID(appointment), exchangeId, raplaUsername, isExchangeItem);
	}

    /**
	 * add an appointment with its unique Rapla {@link Appointment} id, its unique Exchange {@link microsoft.exchange.webservices.data.Appointment} id and the flag if it originated from the Exchange Server in the mapping table.
	 * 
	 * @param appointmentId : {@link Integer} the unique id of the Rapla {@link Appointment}
	 * @param exchangeId : {@link String} the Exchange appointment id
	 * @param raplaUsername : {@link String} the name of the Rapla user that owns the appointment
	 * @param isExchangeItem : {@link Boolean} true if the appointment has been created in Outlook/Exchange, false if it has been created in Rapla
	 */
	public void addAppointment(int appointmentId, String exchangeId, String raplaUsername, boolean isExchangeItem) {
		ExchangeAppointmentStorageObject appointmentStorage = new ExchangeAppointmentStorageObject(appointmentId, exchangeId, raplaUsername, isExchangeItem, false);
		appointments.put(appointmentId, appointmentStorage);
	}
	
	/**
	 * removes an appointment from the mapping table
	 * 
	 * @param exchangeId : {@link String} the Exchange {@link microsoft.exchange.webservices.data.Appointment} id
	 */
	public void removeAppointment(String exchangeId) {
		// get all appointments relied to this Exchange id and remove them
		for (int appointmentId : getAppointmentIds(exchangeId)) {
			removeAppointment(appointmentId);
		}
	}
	
	/**
	 * removes an appointment from the mapping table
	 * 
	 * @param appointment : {@link Appointment} the appointment to be removed from the mapping table
	 */
	public void removeAppointment(Appointment appointment) {
		removeAppointment(ExchangeConnectorUtils.getAppointmentID(appointment));
	}
	
	/**
	 * removes an appointment from the mapping table
	 * 
	 * @param appointmentId : {@link Integer} the appointment to be removed from the mapping table
	 */
	public void removeAppointment(int appointmentId) {
		appointments.remove(appointmentId);
	}
	
	/**
	 * convert the id of an Exchange {@link microsoft.exchange.webservices.data.Appointment} to Rapla {@link Appointment} ids
	 * 
	 * @param exchangeId : {@link String} the Exchange {@link microsoft.exchange.webservices.data.Appointment} id
	 * @return {@link HashSet} of {@link Integer} the Rapla {@link Appointment} ids matching the given Exchange {@link microsoft.exchange.webservices.data.Appointment} id
	 */
	public HashSet<Integer> getAppointmentIds(String exchangeId) {
		HashSet<Integer> appointmentIds = new HashSet<Integer>();
		// iterate over all stored appointments and look for given exchange id
		for (ExchangeAppointmentStorageObject appointmentStorage : appointments.values()) {
			if(appointmentStorage.getExchangeId().equals(exchangeId))
				appointmentIds.add(appointmentStorage.getAppointmentId());
		}
		// return the set of all found appointment ids
		return appointmentIds;
	}
	
	/**
	 * convert the id of an Exchange {@link microsoft.exchange.webservices.data.Appointment} to a Rapla {@link Appointment}
	 * 
	 * @param exchangeId : {@link String} the Exchange {@link microsoft.exchange.webservices.data.Appointment} id
	 * @return {@link HashSet} of {@link Appointment} the Rapla {@link Appointment}s matching the given Exchange {@link microsoft.exchange.webservices.data.Appointment} id
	 */
	public HashSet<Appointment> getAppointments(String exchangeId) {
		HashSet<Appointment> appointments = new HashSet<Appointment>();
		// iterate over all appointment ids matching a particular exchange id
		for (int appointmentId : getAppointmentIds(exchangeId)) {
			// generate appointment objects to these ids
			appointments.add(ExchangeConnectorUtils.getAppointmentById(appointmentId, SynchronisationManager.getSyncManagerClientFacade()));
		}
		return appointments;
	}

    /**
	 * convert the unique id of a Rapla {@link Appointment} to an Exchange {@link microsoft.exchange.webservices.data.Appointment} id 
	 * 
	 * @param appointment : {@link Appointment} the Rapla appointment to be found in the mapping table
	 * @return {@link String} the unique id of the respective {@link microsoft.exchange.webservices.data.Appointment} on the Exchange Server
	 */
	public String getExchangeId(Appointment appointment) {
		return getExchangeId(ExchangeConnectorUtils.getAppointmentID(appointment));
	}
	/**
	 * convert the unique id of a Rapla {@link Appointment} to an Exchange {@link microsoft.exchange.webservices.data.Appointment} id
	 * 
	 * @param appointmentId : {@link Integer} the id of the Rapla appointment to be found in the mapping table
	 * @return {@link String} the unique id of the respective {@link microsoft.exchange.webservices.data.Appointment} on the Exchange Server
	 */
	public String getExchangeId(int appointmentId) {
		ExchangeAppointmentStorageObject appointmentStorage = appointments.get(appointmentId);
		return (appointmentStorage == null)?null :appointmentStorage.getExchangeId();
	}

	/**
	 * @param appointment : {@link Appointment} the Rapla appointment to be found in the mapping table
	 * @return {@link String} the name of the user in the Rapla System
	 */
	public String getRaplaUsername(Appointment appointment) {
		return getRaplaUsername(ExchangeConnectorUtils.getAppointmentID(appointment));
	}
	
	/**
	 * @param appointmentId : {@link Integer} the id of the Rapla appointment to be found in the mapping table
	 * @return {@link String} the name of the user in the Rapla System
	 */
	public String getRaplaUsername(int appointmentId) {
		ExchangeAppointmentStorageObject appointmentStorage = appointments.get(appointmentId);
		return (appointmentStorage == null)?"":appointmentStorage.getRaplaUsername();
	}
	
	/**
	 * @param exchangeId: {@link String} the id of the Exchange {@link microsoft.exchange.webservices.data.Appointment}
	 * @return : true if exchange id is found in the mapping table
	 */
	public boolean exchangeIdExists(String exchangeId) {
		return (getAppointmentIds(exchangeId).size() > 0);
	}
	
	/**
	 * check if a given Rapla {@link Appointment} originates from the Exchange Server
	 * 
	 * @param appointment : {@link Appointment} the Rapla appointment to be found in the mapping table
	 * @return {@link Boolean} true if the appointment has been created in Outlook/Exchange, false if it has been created in Rapla
	 */
	public boolean isExternalAppointment(Appointment appointment) {
		return isExternalAppointment(ExchangeConnectorUtils.getAppointmentID(appointment));
	}
	/**
	 * check if a given Rapla {@link Appointment} originates from the Exchange Server
	 * 
	 * @param appointmentId : {@link Integer} unique id of the Rapla {@link Appointment} the Rapla appointment to be found in the mapping table
	 * @return {@link Boolean} true if the appointment has been created in Outlook/Exchange, false if it has been created in Rapla
	 */
	public boolean isExternalAppointment(int appointmentId) {
		ExchangeAppointmentStorageObject appointmentStorage = appointments.get(appointmentId);
		return (appointmentStorage == null)?false:appointmentStorage.isExchangeItem();
	}
	
	/**
	 * remove all appointments from the mapping table
	 */
	public void clearStorage() {
		appointments.clear();
	}
	
	/**
	 * remember a particular appointment to be deleted from the Exchange Server
	 * 
	 * @param exchangeId : {@link String} the unique id of the Exchange {@link microsoft.exchange.webservices.data.Appointment}
	 */
	public void setDeleted(String exchangeId) {
		for (int appointmentId : getAppointmentIds(exchangeId)) {
			setDeleted(appointmentId);
		}
	}
	
	/**
	 * remember a particular appointment to be deleted from the Exchange Server
	 * items are only added to the list when they have been created in the Rapla System
	 * 
	 * @param appointment : {@link Appointment} the Rapla appointment to be deleted from the Exchange Server
	 */
	public void setDeleted(Appointment appointment) {
		if (appointment != null) {
			setDeleted(ExchangeConnectorUtils.getAppointmentID(appointment));
		}
	}
	
	/**
	 * remember a particular appointment to be deleted from the Exchange Server
	 * 
	 * @param appointmentId : {@link Integer} the id of the Rapla {@link Appointment}
	 */
	public void setDeleted(int appointmentId) {
		ExchangeAppointmentStorageObject appointmentStorage = appointments.get(appointmentId);
		if(appointmentStorage != null && !appointmentStorage.isExchangeItem())
			appointmentStorage.setDeleted();
	}
	
	/**
	 * get all {@link Appointment}s to be deleted from the Server
	 * @return {@link HashSet} of {@link Appointment} which represents all items to be deleted from Exchange Server
	 */
	public HashSet<Appointment> getDeletedItems() {
		HashSet<Appointment> returnAppointments = new HashSet<Appointment>();
		for (ExchangeAppointmentStorageObject appointmentStorage : appointments.values()) {
			if(appointmentStorage.isDeleted() && !appointmentStorage.isExchangeItem())
				returnAppointments.add(ExchangeConnectorUtils.getAppointmentById(appointmentStorage.getAppointmentId(), SynchronisationManager.getSyncManagerClientFacade()));
		}
		return returnAppointments;
	}
	
	/**
	 * get all Exchange {Appointment}s which have been created on the Exchange Server
	 * @return {@link HashSet} of {@link Appointment}s which have been created on the Exchange Server
	 */
	public HashSet<Appointment> getExchangeItems() {
		HashSet<Appointment> returnAppointments = new HashSet<Appointment>();
		for (ExchangeAppointmentStorageObject appointmentStorage : appointments.values()) {
			if(appointmentStorage.isExchangeItem()) {
				Appointment appointment = ExchangeConnectorUtils.getAppointmentById(appointmentStorage.getAppointmentId(), SynchronisationManager.getSyncManagerClientFacade());
				if(appointment != null)
					returnAppointments.add(appointment);
			}
		}
		return returnAppointments;
	}


	public Collection<Integer> getExchangeItemIds() {
		HashSet<Integer> exchangeAppointmentIds = new HashSet<Integer>();
		for (Integer key : appointments.keySet()) {
			if (isExternalAppointment(key)) {
				exchangeAppointmentIds.add(key);
			}
		}
		return exchangeAppointmentIds;
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
			if(appointmentsObject.getClass().equals(appointments.getClass()))
				appointments = (HashMap<Integer, ExchangeAppointmentStorageObject>)appointmentsObject;
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
			objectOutputStream.writeObject(appointments);
			objectOutputStream.close();
			fileOutputStream.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}


	public boolean isRaplaItem(String exchangeId) {
		for (Integer appointmentId : getAppointmentIds(exchangeId)) {
			ExchangeAppointmentStorageObject appointmentStorageObject = appointments.get(appointmentId);
			if(appointmentStorageObject != null && !appointmentStorageObject.isExchangeItem())
				return true;
		}
		return false;
	}


	public boolean appointmentExists(Appointment appointment) {
		return (appointment != null && appointments.get(ExchangeConnectorUtils.getAppointmentID(appointment)) != null);
	}


	public void setAllDeleted() {
		for (ExchangeAppointmentStorageObject storageObject : this.appointments.values()) {
			if (!storageObject.isExchangeItem()) {
				storageObject.setDeleted();
			}
		}
	}
	
	public void removeExchangeItems() {
		Collection<Integer> appointmentStorageObjectsKeys = getExchangeItemIds();
		for (Integer appointmentStorageObjectKey: appointmentStorageObjectsKeys) {
			appointments.remove(appointmentStorageObjectKey);
		}
	}


    public boolean appointmentExists(int key) {
        return appointments.containsKey(key);
    }
}