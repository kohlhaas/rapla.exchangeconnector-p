/**
 * 
 */
package org.rapla.plugin.exchangeconnector.server.datastorage;

import java.io.Serializable;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.WebCredentials;

import org.rapla.plugin.exchangeconnector.server.SynchronisationManager;

/**
 * This class contains all pieces of information related to a specific account on an Exchange Server
 * 
 * @author lutz
 * 
 */
public class ExchangeAccountInformationObject implements Serializable {

	/**
	 * Asserts that only objects of the same version are parsed
	 */
	private static final long serialVersionUID = 3771017275523103372L;
	private String raplaUsername;
	private String exchangeUsername;
	private String smtpAddress;
	private ExchangePassword exchangePassword;
	private boolean downloadFromExchange;
	


	/**
	 * The constructor
	 * 
	 * @param raplaUsername : {@link String}
	 * @param exchangeUsername : {@link String}
	 * @param exchangePassword : {@link String}
	 * @param downloadFromExchange : {@link Boolean} 
	 * @param smtpAddress : {@link String}
	 */
	public ExchangeAccountInformationObject(String raplaUsername, String exchangeUsername,String exchangePassword, boolean downloadFromExchange, String smtpAddress) throws Exception {
		this(raplaUsername,exchangeUsername, exchangePassword, downloadFromExchange);
		setSMTPAddress(smtpAddress);
	}

	/**
	 * The constructor
	 * 
	 * @param raplaUsername : {@link String}
	 * @param exchangeUsername : {@link String}
	 * @param exchangePassword : {@link String}
	 * @param downloadFromExchange : {@link Boolean}
	 */
	public ExchangeAccountInformationObject(String raplaUsername, String exchangeUsername,String exchangePassword, boolean downloadFromExchange) throws Exception {
		super();
			setRaplaUsername(raplaUsername);
			setExchangeUsername(exchangeUsername);
			setDownloadFromExchange(downloadFromExchange);
			setExchangePassword(new String(getBytes()),exchangePassword);

	}
	/**
	 * @return {@link Boolean} true if private {@link Appointment}s (from Exchange) should be downloaded to the Rapla system
	 */
	public boolean isDownloadFromExchange() {
		return downloadFromExchange;
	}
	
	/**
	 * @param downloadFromExchange : {@link Boolean} true if private {@link Appointment}s (from Exchange) should be downloaded to the Rapla system
	 */
	void setDownloadFromExchange(boolean downloadFromExchange) {
		this.downloadFromExchange = downloadFromExchange;
	}

	/**
	 * @return {@link String} 
	 */
	public String getRaplaUsername() {
		return raplaUsername;
	}

	/**
	 * @param raplaUsername : {@link String}
	 *            the raplaUsername to set
	 */
	private void setRaplaUsername(String raplaUsername) {
		this.raplaUsername = raplaUsername;
	}
	

	private byte[] getBytes() {
		return getRaplaUsername().getBytes();
	}
	
	/**
	 * @return {@link ExchangeUsername} the exchangeUsername
	 */
	private String getExchangeUsername() {
		return exchangeUsername;
	}

	/**
	 * @param exchangeUsername : {@link String}
	 *            the exchangeUsername to set
	 */
	private void setExchangeUsername(String exchangeUsername) {
		this.exchangeUsername = exchangeUsername;
	}

	/**
	 * @return {@link ExchangePassword} the exchangePassword
	 */
	private ExchangePassword getExchangePassword() {
		return exchangePassword;
	}

	/**
	 * @param exchangePassword : {@link ExchangePassword}
	 *            the exchangePassword to set
	 */
	private void setExchangePassword(ExchangePassword exchangePassword) {
		this.exchangePassword = exchangePassword;
	}
	
	/**
	 * @param exchangePassword : {@link String}
	 *            the exchangePassword to set as String
	 * @throws Exception 
	 */
	private void setExchangePassword(String string, String exchangePassword) throws Exception {
		setExchangePassword(new ExchangePassword(string,exchangePassword));
	}
	
	/**
	 * @return {@link String} the smtpAddress
	 */
	public String getSMTPAddress() {
		return smtpAddress;
	}

	/**
	 * @param smtpAddress : {@link String} the smtpAddress to set
	 */
	public void setSMTPAddress(String smtpAddress) {
		this.smtpAddress = smtpAddress;
	}

	/**
	 * @return {@link WebCredentials}
	 */
	public WebCredentials getCredentials() throws Exception {
			return new WebCredentials(getExchangeUsername(), getExchangePassword().getExchangePassword());

		
	}
}
