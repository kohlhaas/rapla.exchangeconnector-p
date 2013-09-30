package org.rapla.plugin.exchangeconnector;

import org.rapla.entities.User;
import org.rapla.framework.RaplaException;

public interface ExchangeConnectorRemote {
	
	/** 
	 * Add an Exchange user to the user list (register a user to the Exchange Server)
	 * (User wants to have his Exchange Account synchronized with the Rapla system)
	 * 
	 * @param raplaUsername
	 * @param exchangeUsername  
	 * @param exchangePassword
	 * @param downloadFromExchange
	 * @return {@link ClientMessage}
	 * @throws RaplaException
	 */
	public String addExchangeUser(String raplaUsername, String exchangeUsername, String exchangePassword, Boolean downloadFromExchange) throws RaplaException;

	/**
	 * Remove an existing user from the user list (unregister a user from the Exchange Server)
	 * (The User and the password will no longer be saved)
	 * 
	 * @param raplaUsername : {@link String} name of the Rapla {@link User} which should be removed from the user list
	 * @return {@link ClientMessage}
	 * @throws RaplaException
	 */
	public String removeExchangeUser(String raplaUsername) throws RaplaException;

	/**
	 * This method initialises a so called "complete reconciliation" - meaning a re-sync of all existing appointments on both systems.
	 * @return {@link ClientMessage}
	 * @throws RaplaException
	 */
	public String completeReconciliation() throws RaplaException;

    /**
     * sync an Exchange user
     *
     * @param raplaUsername
     * @return {@link ClientMessage}
     * @throws RaplaException
     */
    public String synchronizeUser(String raplaUsername) throws RaplaException;


    /**
     * checks wether exchange is available
     * @return true, if service is available und connected
     * @throws RaplaException
     */
    public boolean isExchangeAvailable() throws RaplaException;

    /**
     * enables/disable pull
     * @param raplaUsername
     * @param downloadFromExchange
     * @throws RaplaException
     */
    public void setDownloadFromExchange(String raplaUsername, boolean downloadFromExchange) throws RaplaException;


}