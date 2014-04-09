package org.rapla.plugin.exchangeconnector.server;

import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.DefaultConfiguration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote;
import org.rapla.plugin.exchangeconnector.SynchronizationStatus;
import org.rapla.plugin.exchangeconnector.SynchronizeResult;
import org.rapla.server.RaplaKeyStorage;
import org.rapla.server.RaplaKeyStorage.LoginInfo;
import org.rapla.server.RemoteMethodFactory;
import org.rapla.server.RemoteSession;
import org.rapla.storage.RaplaSecurityException;

public class ExchangeConnectorRemoteObjectFactory extends RaplaComponent implements RemoteMethodFactory<ExchangeConnectorRemote>{
	final SynchronisationManager manager;
	RaplaKeyStorage keyStorage;
			
	public ExchangeConnectorRemoteObjectFactory(RaplaContext context) throws RaplaContextException {
		super(context);
		this.keyStorage = context.lookup( RaplaKeyStorage.class);
		this.manager = context.lookup( SynchronisationManager.class);
	}

	@Override
	public ExchangeConnectorRemote createService(final RemoteSession remoteSession) throws RaplaContextException  {
		final User user  = remoteSession.getUser();
		return new ExchangeConnectorRemote() {
			@Override
			public SynchronizeResult synchronize() throws RaplaException {
	
				// Synchronize this user after registering
				getLogger().debug("Invoked change sync for user " + user.getUsername());
				SynchronizeResult result = manager.synchronizeUser(user);
				return result;
			}
			
			@Override
			public SynchronizationStatus getSynchronizationStatus() throws RaplaException {
				SynchronizationStatus status = new SynchronizationStatus();
				LoginInfo secrets = keyStorage.getSecrets(user, ExchangeConnectorServerPlugin.EXCHANGE_USER_STORAGE);
				boolean connected = secrets != null;
				status.enabled = connected;
				status.username = secrets != null ? secrets.login :"";
				if ( secrets != null)
				{
					status.unsynchronizedEvents = manager.getOpenTasksCount( user);
				}
				return status;
			}
			
			@Override
			public SynchronizeResult retry() throws RaplaException 
			{
				LoginInfo secrets = keyStorage.getSecrets(user, ExchangeConnectorServerPlugin.EXCHANGE_USER_STORAGE);
				if ( secrets != null)
				{
					String exchangeUsername = secrets.login;
					String exchangePassword = secrets.secret;
					manager.testConnection(exchangeUsername, exchangePassword);
					return manager.retry(user);
				}
				else
				{
					throw new RaplaException("User " + user.getUsername() + " not connected to exchange");
				}
			}
			
	
			@Override
			public void changeUser(String exchangeUsername, String exchangePassword) throws RaplaException {
				String raplaUsername = user.getUsername();
		        getLogger().debug("Invoked add exchange user for rapla " + raplaUsername + " with exchange user " + exchangeUsername);
		        manager.testConnection( exchangeUsername, exchangePassword);
				getLogger().debug("Invoked change connection for user " + user.getUsername());
				keyStorage.storeLoginInfo( user, ExchangeConnectorServerPlugin.EXCHANGE_USER_STORAGE, exchangeUsername, exchangePassword);
			}
			
			public void removeUser() throws RaplaException 
			{
				getLogger().info("Removing exchange connection for user " + user);
				keyStorage.removeLoginInfo(user, ExchangeConnectorServerPlugin.EXCHANGE_USER_STORAGE);
				manager.removeTasksAndExports(user);
			}
			
            @Override
			public DefaultConfiguration getConfig() throws RaplaException {
			    User user = remoteSession.getUser();
                if ( !user.isAdmin())
                {
                    throw new RaplaSecurityException("Access only for admin users");
                }
                Preferences preferences = getQuery().getSystemPreferences();
                RaplaConfiguration config = preferences.getEntry( ExchangeConnectorConfig.EXCHANGESERVER_CONFIG, new RaplaConfiguration());
                return config;
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