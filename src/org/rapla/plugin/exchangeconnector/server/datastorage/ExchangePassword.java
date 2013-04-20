/**
 * 
 */
package org.rapla.plugin.exchangeconnector.server.datastorage;

import java.io.Serializable;

/**
 * This class is an envelope for the Exchange Password of a specific user
 * 
 * @author lutz
 *
 */
public class ExchangePassword implements Serializable {

	/**
	 * Asserts that only objects of the same version are parsed
	 */
	private static final long serialVersionUID = 527813629069092471L;
	private String byteField;
	private String exchangePasswordHASH;
	
	
	/**
	 * The constructor
	 * 
	 * @param string : {@link String} which is permanently associated with the Password
	 * @param exchangePassword : {@link String}
	 * @throws Exception
	 */
	public ExchangePassword(String string, String exchangePassword) throws Exception {
		setByteField(string);
		setExchangePasswordHASH(getByteField(),exchangePassword);
	}
	
	/**
	 * Returns the decrypted, plaintext Password
	 * 
	 * @return {@link String} the exchangePassword
	 * @throws Exception 
	 */
	public String getExchangePassword() throws Exception {
		return CryptoHandler.decrypt(getExchangePasswordHASH(), getByteField());
	}

	/**
	 * @return the exchangePasswordHASH
	 */
	private String getExchangePasswordHASH() {
		return exchangePasswordHASH;
	}
	
	
	/**
	 * @param additive : {@link String} 
	 * @param exchangePassword : {@link String}
	 * @throws Exception 
	 */
	private void setExchangePasswordHASH(String additive, String exchangePassword) throws Exception {
	    exchangePasswordHASH = CryptoHandler.encrypt(exchangePassword, additive);
	}
	/**
	 * @return the byteField
	 */
	private String getByteField() {
		return byteField;
	}
	/**
	 * @param byteField : {@link String} the byteField to set
	 */
	private void setByteField(String userName) {
		this.byteField = userName;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ExchangePassword ";
	}

}
