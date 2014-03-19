package org.rapla.plugin.exchangeconnector.server;

import org.rapla.entities.User;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.Configuration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote;
import org.rapla.plugin.exchangeconnector.SynchronizationStatus;
import org.rapla.plugin.exchangeconnector.server.exchange.EWSConnector;
import org.rapla.server.RaplaKeyStorage;
import org.rapla.server.RaplaKeyStorage.LoginInfo;
import org.rapla.server.RemoteMethodFactory;
import org.rapla.server.RemoteSession;

public class ExchangeConnectorRemoteObjectFactory extends RaplaComponent implements RemoteMethodFactory<ExchangeConnectorRemote>{
	ExchangeConnectorConfig.ConfigReader config;
	final SynchronisationManager manager;
	RaplaKeyStorage keyStorage;
			
	public ExchangeConnectorRemoteObjectFactory(RaplaContext context,Configuration config) throws RaplaContextException {
		super(context);
		this.config = new ExchangeConnectorConfig.ConfigReader( config );
		this.keyStorage = context.lookup( RaplaKeyStorage.class);
		this.manager = context.lookup( SynchronisationManager.class);
	}

	@Override
	public ExchangeConnectorRemote createService(RemoteSession remoteSession) throws RaplaContextException  {
		final User user  = remoteSession.getUser();
		return new ExchangeConnectorRemote() {
			@Override
			public String synchronize() throws RaplaException {
				String returnMessage;
	
					// Synchronize this user after registering
					try {
						getLogger().debug("Invoked change sync for user " + user.getUsername());
						//manager.synchronizeUser(user);
		                returnMessage = "Your registration was successful! " ;
	
		            } catch (Exception e) {
		                returnMessage = "An error occurred - You are not registered!\n\n"+e.getMessage();
	
		                getLogger().error(e.getMessage(),e);
					} 
				return returnMessage;
			}
			
			@Override
			public SynchronizationStatus getSynchronizationStatus() throws RaplaException {
				SynchronizationStatus status = new SynchronizationStatus();
				LoginInfo secrets = keyStorage.getSecrets(user, "exchange");
				boolean connected = secrets != null;
				status.enabled = connected;
				status.username = secrets != null ? secrets.login :"";
				status.status = "synchronized";
				return status;
			}
			
	
			@Override
			public void changeUser(String exchangeUsername, String exchangePassword) throws RaplaException {
				String raplaUsername = user.getUsername();
		        getLogger().debug("Invoked add exchange user for rapla " + raplaUsername + " with exchange user " + exchangeUsername);
				boolean testConnection = true;
				if(testConnection) {
					String fqdn = config.get(ExchangeConnectorConfig.EXCHANGE_WS_FQDN);
					EWSConnector connector = new EWSConnector(fqdn, exchangeUsername, exchangePassword);
					try {
						connector.test();
					} catch (Exception e) {
						throw new RaplaException("Kann die Verbindung zu Exchange nicht herstellen: " + e.getMessage());
					}
				}
				getLogger().debug("Invoked change connection for user " + user.getUsername());
				keyStorage.storeLoginInfo( user, "exchange", exchangeUsername, exchangePassword);
			}
			
			public void removeUser() throws RaplaException 
			{
				getLogger().info("Removing exchange connection for user " + user);
				keyStorage.removeLoginInfo(user, "exchange");
				manager.removeTasks(user);
			}
		};
	}
	

}

//public String completeReconciliation() throws RaplaException {
//String returnMessage = "Disabled!";
//String returnMessage = "Sync all encountered a problem";
//try {
//	scheduledDownloadTimer.cancel();
//	Thread completeReconciliationThread = new Thread(new CompleteReconciliationHandler(clientFacade), "CompleteReconciliationThread");
//	completeReconciliationThread.start();
//	while (completeReconciliationThread.isAlive()) {
//		this.wait(100);
//	}
//	initScheduledDownloadTimer();
//	returnMessage =  "Synchronization of all  items is finished";
//} catch (InterruptedException e) {
//	logException(e);
//}
//return returnMessage;
//}



//public void setDownloadFromExchange( boolean downloadFromExchange) throws RaplaException {
//String raplaUsername = user.getUsername();
//final boolean result = accountStorage.setDownloadFromExchange(raplaUsername, downloadFromExchange);
//if (result)
//    accountStorage.save();
////todo: synch user if new?
//}