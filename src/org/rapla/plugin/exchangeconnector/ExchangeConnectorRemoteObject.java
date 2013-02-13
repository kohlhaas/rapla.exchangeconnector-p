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
	@Override
	public String addExchangeUser(String raplaUsername,
			String exchangeUsername, String exchangePassword,
			Boolean downloadFromExchange) throws RaplaException {
		return SynchronisationManager.getInstance().addExchangeUser(raplaUsername, exchangeUsername, exchangePassword, downloadFromExchange);
	}

	/* (non-Javadoc)
	 * @see org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote#removeExchangeUser(java.lang.String)
	 */
	@Override
	public String removeExchangeUser(String raplaUsername)
			throws RaplaException {
		return SynchronisationManager.getInstance().removeExchangeUser(raplaUsername);
	}

	/* (non-Javadoc)
	 * @see org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote#completeReconciliation()
	 */
	@Override
	public String completeReconciliation() throws RaplaException {
		return SynchronisationManager.getInstance().completeReconciliation();
	}

    @Override
    public boolean isExchangeAvailable() throws RaplaException {
        return SynchronisationManager.getInstance().isExchangeAvailable();
    }

    @Override
    public void setDownloadFromExchange(String raplaUsername, boolean downloadFromExchange) throws RaplaException {
        if (SynchronisationManager.getInstance().setDownloadFromExchange(raplaUsername, downloadFromExchange))
            //SynchronisationManager.getInstance().
       ; //todo: synch user if new?
    }

}
