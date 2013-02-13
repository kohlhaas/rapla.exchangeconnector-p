/**
 * 
 */
package org.rapla.plugin.exchangeconnector;

import org.rapla.framework.RaplaException;

/**
 * @author lutz
 *
 */
public class ExchangeConnectorRemoteObject implements ExchangeConnectorRemote {

	/**
	 * 
	 */
	public ExchangeConnectorRemoteObject() {}

	/* (non-Javadoc)
	 * @see org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote#addExchangeUser(java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean)
	 */
	public String addExchangeUser(String raplaUsername,
			String exchangeUsername, String exchangePassword,
			Boolean downloadFromExchange) throws RaplaException {
		return SynchronisationManager.getInstance().addExchangeUser(raplaUsername, exchangeUsername, exchangePassword, downloadFromExchange);
	}

	/* (non-Javadoc)
	 * @see org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote#removeExchangeUser(java.lang.String)
	 */
	public String removeExchangeUser(String raplaUsername)
			throws RaplaException {
		return SynchronisationManager.getInstance().removeExchangeUser(raplaUsername);
	}

	/* (non-Javadoc)
	 * @see org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote#completeReconciliation()
	 */
	public String completeReconciliation() throws RaplaException {
		return SynchronisationManager.getInstance().completeReconciliation();
	}

    public boolean isExchangeAvailable() throws RaplaException {
        return SynchronisationManager.getInstance().isExchangeAvailable();
    }

    public void setDownloadFromExchange(String raplaUsername, boolean downloadFromExchange) throws RaplaException {
        if (SynchronisationManager.getInstance().setDownloadFromExchange(raplaUsername, downloadFromExchange))
            //SynchronisationManager.getInstance().
       ; //todo: synch user if new?
    }

}
