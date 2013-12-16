/**
 * 
 */
package org.rapla.plugin.exchangeconnector.server;

import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.NameResolutionCollection;
import microsoft.exchange.webservices.data.ResolveNameSearchLocation;
import microsoft.exchange.webservices.data.WebCredentials;

import org.rapla.entities.User;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig.ConfigReader;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote;
import org.rapla.plugin.exchangeconnector.server.worker.AppointmentTask;

public class ExchangeConnectorRemoteObject extends RaplaComponent implements ExchangeConnectorRemote {

	private User user;
	final AppointmentTask task;
	ConfigReader config;
	
	public ExchangeConnectorRemoteObject(RaplaContext context,ConfigReader config, User user) throws RaplaContextException {
		super(context);
		this.user = user;
		this.config = config;
		this.task = context.lookup( AppointmentTask.class);
	}

	/* (non-Javadoc)
	 * @see org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote#addExchangeUser(java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean)
	 */
	public String synchronize(String exchangeUsername, String exchangePassword) throws RaplaException {
		String raplaUsername = user.getUsername();
        getLogger().debug("Invoked add exchange user for rapla " + raplaUsername + " with exchange user " + exchangeUsername);
        String returnMessage;

			// Synchronize this user after registering
			try {
				
				
				boolean testConnection = true;
				if(testConnection) {
					WebCredentials cred = new WebCredentials(exchangeUsername, exchangePassword);
					test(cred);
				}
				getLogger().debug("Invoked change sync for user " + user.getUsername());
				task.synchronizeUser(user);
                returnMessage = "Your registration was successful! " ;

            } catch (Exception e) {
                returnMessage = "An error occurred - You are not registered!\n\n"+e.getMessage();

                getLogger().error(e.getMessage(),e);
			} 
		return returnMessage;
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
	private void test(WebCredentials credentials) throws Exception {
		String fqdn = config.get(ExchangeConnectorConfig.EXCHANGE_WS_FQDN);
		EWSConnector ewsConnector = new EWSConnector(fqdn, credentials);
		String user2 = credentials.getUser();
		NameResolutionCollection nameResolutionCollection = ewsConnector.getService().resolveName(user2, ResolveNameSearchLocation.DirectoryOnly, true);
		if (nameResolutionCollection.getCount() == 1) {
			String smtpAddress = nameResolutionCollection.nameResolutionCollection(0).getMailbox().getAddress();
			if (!smtpAddress.isEmpty()) {
				//return smtpAddress;
			}
		}
		//throw new Exception("Credentials are invalid!");
	}


//	public String completeReconciliation() throws RaplaException {
//		String returnMessage = "Disabled!";
//		String returnMessage = "Sync all encountered a problem";
//		try {
//			scheduledDownloadTimer.cancel();
//			Thread completeReconciliationThread = new Thread(new CompleteReconciliationHandler(clientFacade), "CompleteReconciliationThread");
//			completeReconciliationThread.start();
//			while (completeReconciliationThread.isAlive()) {
//				this.wait(100);
//			}
//			initScheduledDownloadTimer();
//			returnMessage =  "Synchronization of all  items is finished";
//		} catch (InterruptedException e) {
//			logException(e);
//		}
//		return returnMessage;
//	}

   
 
//    public void setDownloadFromExchange( boolean downloadFromExchange) throws RaplaException {
//    	String raplaUsername = user.getUsername();
//    	final boolean result = accountStorage.setDownloadFromExchange(raplaUsername, downloadFromExchange);
//        if (result)
//            accountStorage.save();
//        //todo: synch user if new?
//    }


}
