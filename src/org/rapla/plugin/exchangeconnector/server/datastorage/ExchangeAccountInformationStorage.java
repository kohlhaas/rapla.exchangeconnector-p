/**
 * 
 */
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
import java.util.Set;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.WebCredentials;

import org.rapla.entities.User;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.EWSConnector;


/**
 * This singleton-class is the single-point-of-access to the account information within the plugin.
 * It should be treated as a <b>black-box</b> and not changed! Besides that, the value of 
 * the parameter <i>PEPPER</i> in {@link CryptoHandler} should be altered to a random String
 * before building the application, due to the projects open-source character.
 * 
 * @author lutz
 * @see CryptoHandler
 */
public class ExchangeAccountInformationStorage {
	private String storageFilePath = "";
	private static String DEFAULT_STORAGE_FILE_PATH = "data/accountManager.dat";
	
	private HashMap<String, ExchangeAccountInformationObject> accountInformation;
	
	private static ExchangeAccountInformationStorage SINGLETON_INSTANCE;
	
	/**
	 * @return the single, accessible instance of {@link ExchangeAccountInformationStorage}
	 */
	public static ExchangeAccountInformationStorage getInstance(){
		if(SINGLETON_INSTANCE == null)
			SINGLETON_INSTANCE = new ExchangeAccountInformationStorage();
		
		return SINGLETON_INSTANCE;	
	}

	/**
	 * private constructor of the class to read the file
	 */
	private ExchangeAccountInformationStorage() {
		this(DEFAULT_STORAGE_FILE_PATH);
	}
	
	/**
	 * public constructor of the class to read a particular file

	 * @param filePath : {@link String} absolute path to the file which saves the Rapla username, Exchange username and Exchange password
	 */
	public ExchangeAccountInformationStorage(String filePath) {
		storageFilePath = filePath;
		accountInformation = new HashMap<String, ExchangeAccountInformationObject>();
		load();
		
		// remember this reference (forget old reference)
		SINGLETON_INSTANCE = this;
	}

	/**
	 * Adds a further account to the data
	 * 
	 * @param raplaUsername : {@link String}
	 * @param exchangeUsername : {@link String}
	 * @param exchangePassword : {@link String}
	 * @param testConnection : {@link Boolean}
	 * @param downloadFromExchange : {@link Boolean} true if the user wants to download the {@link Appointment}s from Exchange to the Rapla System
	 * @return true : {@link Boolean} if the information has been added to the dataset
	 */
	public void addAccount(String raplaUsername,String exchangeUsername, String exchangePassword, boolean downloadFromExchange, boolean testConnection) throws Exception {
		ExchangeAccountInformationObject accountInformationObject = new ExchangeAccountInformationObject(raplaUsername, exchangeUsername, exchangePassword, downloadFromExchange);
			if(testConnection) {
				String smtpAddress = retrieveSMTPAddress(accountInformationObject);
				accountInformationObject.setSMTPAddress(smtpAddress);
			}
			accountInformation.put(raplaUsername, accountInformationObject);
			save();

	}
	
	/**
	 * Adds a further account to the data
	 * 
	 * @param raplaUser : {@link User}
	 * @param exchangeUsername : {@link String}
	 * @param exchangePassword : {@link String}
	 * @param testConnection : {@link Boolean}
	 * @param downloadFromExchange : {@link Boolean} true if the user wants to download the {@link Appointment}s from Exchange to the Rapla System
	 * @return true : {@link Boolean} if the information has been added to the dataset
	 *//*
	public void addAccount(User raplaUser, String exchangeUsername, String exchangePassword, boolean downloadFromExchange, boolean testConnection) throws Exception {
		// prohibit adding "null" user objects
		if(raplaUser == null)
			return;
		addAccount(raplaUser.getUsername(), exchangeUsername, exchangePassword, downloadFromExchange, testConnection);
	}*/
	
	/**
	 * Adds a further account to the data
	 * 
	 * @param raplaUsername : {@link String}
	 * @param exchangeUsername : {@link String}
	 * @param exchangePassword : {@link String}
	 * @param downloadFromExchange : {@link Boolean} true if the user wants to download the {@link Appointment}s from Exchange to the Rapla System
	 * @return true : {@link Boolean} if the information has been added to the dataset
	 */
	public void addAccount(String raplaUsername,String exchangeUsername, String exchangePassword, boolean downloadFromExchange) throws Exception {
		addAccount(raplaUsername, exchangeUsername, exchangePassword, downloadFromExchange, true);
	}
	
	/**
	 * Adds a further account to the data
	 * 
	 * @param raplaUser : {@link User}
	 * @param exchangeUsername : {@link String}
	 * @param exchangePassword : {@link String}
	 * @param downloadFromExchange : {@link Boolean} true if the user wants to download the {@link Appointment}s from Exchange to the Rapla System
	 * @return true : {@link Boolean} if the information has been added to the dataset
	 */
	public boolean addAccount(User raplaUser, String exchangeUsername, String exchangePassword, boolean downloadFromExchange) {
		return addAccount(raplaUser, exchangeUsername, exchangePassword, true);			
	}
	
	/**
	 * Remove an account from the stored data
	 * 
	 * @param raplaUsername : {@link String}
	 * @return {@link Boolean}
	 */
	public boolean removeAccount(String raplaUsername) {
		try {
			accountInformation.remove(raplaUsername);
			save();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the Exchange {@link WebCredentials} associated with the username
	 * 
	 * @param raplaUsername : {@link String}
	 * @return {@link WebCredentials}
	 */
	public WebCredentials getWebCredentialsForRaplaUser(String raplaUsername) throws Exception {
		WebCredentials returnVal = null;
		if(accountInformation.containsKey(raplaUsername))
			returnVal = accountInformation.get(raplaUsername).getCredentials();
		return returnVal;
	}
	
	/**
	 * Returns the Exchange {@link WebCredentials} associated with the {@link User}
	 *  
	 * @param raplaUser : {@link User}
	 * @return {@link WebCredentials}
	 */
	public WebCredentials getWebCredentialsForRaplaUser(User raplaUser) throws Exception {
		if (raplaUser != null) {
			return getWebCredentialsForRaplaUser(raplaUser.getUsername());
		}
		else{
			return null;
		}
	}
	
	/**
	 * Returns all existing Exchange {@link WebCredentials}
	 * 
	 * @return {@link WebCredentials}
	 */
	public Collection<WebCredentials> getAllWebCredentials() throws Exception {
		Collection<WebCredentials> returnVal = new ArrayList<WebCredentials>();
		for (ExchangeAccountInformationObject accountInformation : this.accountInformation.values()) {
			returnVal.add(accountInformation.getCredentials());
		}
		return returnVal;
	}
	
	/**
	 * @return usernames of all users that have registered the Synchronization Plugin
	 */
	public Set<String> getAllRaplaUsernames() {
		return accountInformation.keySet();
	}
	
	/**
	 * Returns the flag "downloadFromExchange"
	 * 
	 * @param raplaUser : {@link User}
	 * @return {@link Boolean} : true if the user wants to download the {@link Appointment}s from the Exchange Server
	 */
	public boolean isDownloadFromExchange(User raplaUser) {
		return isDownloadFromExchange(raplaUser.getUsername());
	}
	
	/**
	 * Returns the flag "downloadFromExchange"
	 * 
	 * @param raplaUsername : {@link String}
	 * @return {@link Boolean} : true if the user wants to download the {@link Appointment}s from the Exchange Server
	 */
	public boolean isDownloadFromExchange(String raplaUsername) {
		if(accountInformation.containsKey(raplaUsername))
			return accountInformation.get(raplaUsername).isDownloadFromExchange();
		return false;
	}
	
	/**
	 * Returns the smtp-address associated with the {@link User}
	 * 
	 * @param raplaUser : {@link User}
	 * @return {@link String}
	 */
	public String getSMTPAddressForRaplaUser(User raplaUser){
		return getSMTPAddressForRaplaUser(raplaUser.getUsername());
	}
	/**
	 * @param raplaUsername
	 * @return
	 */
	public String getSMTPAddressForRaplaUser(String raplaUsername) {
		String returnVal = null;
		if(accountInformation.containsKey(raplaUsername))
			returnVal = accountInformation.get(raplaUsername).getSMTPAddress();
		return returnVal;
	}
	
	/**
	 * Checks if the passed {@link ExchangeAccountInformationObject} object is valid, by instantiating an {@link ExchangeService}
	 * and retrieving the associated smtp-address from the Exchange Server. In case of the address being not empty,
	 * it is returned.
	 * 
	 * @param accountInformationObject
	 * @return {@link String} if the account information is valid
	 * @throws Exception 
	 */
	private String retrieveSMTPAddress(ExchangeAccountInformationObject accountInformationObject) throws Exception {
		String smtpAddress = new EWSConnector(ExchangeConnectorPlugin.EXCHANGE_WS_FQDN, accountInformationObject.getCredentials()).getSMPTAddress();
		
		if (!smtpAddress.isEmpty()) {
			return smtpAddress;
		}
		else
			throw new Exception("Credentials are invalid!");
	}

	/**
	 * This method finds the Rapla username associated with the passed SMTP address
	 * 
	 * @param smtpAddress : {@link String}
	 * @return {@link String}
	 */
	public String getUsernameOfSMTPAddress(String smtpAddress) {
		for (ExchangeAccountInformationObject tmpAccount : accountInformation.values()) {
			if(tmpAccount.getSMTPAddress().equalsIgnoreCase(smtpAddress));
				return tmpAccount.getRaplaUsername();
		}
		return null;
	}
	
	/**
	 * This method finds the Rapla usernames associated with the passed SMTP addresses
	 * 
	 * @param smtpAddresses : {@link Collection} of {@link String}
	 * @return {@link Collection} of {@link String}
	 */
	public Collection<String> getUsernamesOfSMTPAddresses(Collection<String> smtpAddresses){
		HashSet<String> raplaUsernames = new HashSet<String>();
		String tmpUsername;
		for (String smtpAddress : smtpAddresses) {
			if((tmpUsername=getSMTPAddressForRaplaUser(smtpAddress))!=null)
				raplaUsernames.add(tmpUsername);
		}		
		return raplaUsernames;
		
	}
	
	/**
	 * Loads the file into the account information list
	 * @return {@link Boolean} true if loading the serialized file succeeded
	 */
	@SuppressWarnings("unchecked")
	public boolean load() {
		try {
			File file = new File(storageFilePath );
			FileInputStream fileInputStream = new FileInputStream(file);
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
			Object accountInformationObject = objectInputStream.readObject();
			if(accountInformationObject.getClass().equals(accountInformation.getClass()))
				accountInformation = (HashMap<String, ExchangeAccountInformationObject>)accountInformationObject;
			objectInputStream.close();
			fileInputStream.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * save all data (the account information) to a serialized file
	 * @return {@link Boolean} true if saving the serialized file succeeded
	 */
	public boolean save() {
		try {
			File file = new File(storageFilePath );
			FileOutputStream fileOutputStream = new FileOutputStream(storageFilePath);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
			objectOutputStream.writeObject(accountInformation);
			objectOutputStream.close();
			fileOutputStream.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public boolean hasUser(User user) {
		return this.accountInformation.containsKey(user.getUsername());
	}

    public boolean setDownloadFromExchange(String raplaUsername, boolean downloadFromExchange) {
        final ExchangeAccountInformationObject exchangeAccountInformationObject = accountInformation.get(raplaUsername);
        if (exchangeAccountInformationObject != null) {
            exchangeAccountInformationObject.setDownloadFromExchange(downloadFromExchange);
            return true;
        }
       return false;
    }
}
