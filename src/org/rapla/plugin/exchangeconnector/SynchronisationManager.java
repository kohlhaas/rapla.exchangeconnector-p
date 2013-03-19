package org.rapla.plugin.exchangeconnector;

import java.util.Timer;

import org.rapla.facade.*;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.datastorage.ExchangeAppointmentStorage;
import org.rapla.plugin.exchangeconnector.datastorage.ExchangeAccountInformationStorage;
import org.rapla.plugin.exchangeconnector.jms.IMessageReceiver;
import org.rapla.plugin.exchangeconnector.jms.IMessageServer;
import org.rapla.plugin.exchangeconnector.jms.MessageServerFactory;
import org.rapla.plugin.exchangeconnector.jms.ReservationMessageListener;

import javax.jms.JMSException;

/**    
 * @author Alex Heil, Dominik Joder, Lutz Bergendahl, Matthias Hundt
 * @see {@link ExchangeAppointmentStorage}
 * @see {@link ExchangeAccountInformationStorage}
 */
public class SynchronisationManager extends RaplaComponent implements AllocationChangeListener, ModificationListener {
	
	
	private static SynchronisationManager synchronisationManagerInstance = null;
	private static ClientFacade clientFacade;
	private Timer scheduledDownloadTimer;
    private IMessageServer messageServer;

    /**
	 * The constructor 
	 * 
	 * @param context : {@link RaplaContext}
	 * @throws RaplaException
	 */
	public SynchronisationManager(RaplaContext context) throws RaplaException {
		super(context);
		setInstance(this);
		
		clientFacade = (ClientFacade) context.lookup(ClientFacade.ROLE);
		clientFacade.addAllocationChangedListener(this);
        clientFacade.addModificationListener(this);



        try {

            messageServer = MessageServerFactory.getMessageServer();
            final IMessageReceiver messageReceiver = MessageServerFactory.getMessageReveiver();

            final ReservationMessageListener rsm = new ReservationMessageListener(clientFacade);
            messageReceiver.addMessageListener(rsm);
        } catch (JMSException e) {
            logException(e);
        }


	    initScheduledDownloadTimer();		
	}




	/**
	 * Init the scheduledDownloadTimer
	 */
	private void initScheduledDownloadTimer() {
		scheduledDownloadTimer = new Timer("ScheduledDownloadThread");
		scheduledDownloadTimer.schedule(new ScheduledDownloadHandler(clientFacade), 30000, ExchangeConnectorPlugin.PULL_FREQUENCY*1000);
	}
	

	

	/**
	 * @return the synchronisationManagerInstance
	 */
	public static SynchronisationManager getInstance() {
		return synchronisationManagerInstance; 
	}

	/**
	 * @param synchronisationManagerInstance the synchronisationManagerInstance to set
	 */
	public static void setInstance(
			SynchronisationManager synchronisationManagerInstance) {
		SynchronisationManager.synchronisationManagerInstance = synchronisationManagerInstance;
	}
	
	
	/**
	 * @return {@link ClientFacade}
	 */
	public static ClientFacade getSyncManagerClientFacade() {
		return SynchronisationManager.clientFacade;
	}


	/**
	 * @param e
	 */
	public static void logException(Exception e){
		SynchronisationManager.getInstance().getLogger().error("Exception in the ExchangeConnector Plugin: ", e);
	}

	/**
	 * This method is called when an appointment change has been triggered
	 * 
	 * @param changeEvents : Array of {@link AllocationChangeEvent} with all changes that happened
	 * @see org.rapla.facade.AllocationChangeListener#changed(org.rapla.facade.AllocationChangeEvent[])
	 */
	public void changed(AllocationChangeEvent[] changeEvents) {
        SynchronisationManager.logInfo("Invoked change handler for " + changeEvents.length + " events");
		Thread changeHandlerThread = new Thread(new ChangeHandler(changeEvents, clientFacade), "ChangeHandlerThread");
		changeHandlerThread.start();
	}
	
	public String addExchangeUser(String raplaUsername, String exchangeUsername, String exchangePassword, Boolean downloadFromExchange) throws RaplaException {
        SynchronisationManager.logInfo("Invoked add exchange user for rapla " + raplaUsername+" with exchange user "+exchangeUsername);
		boolean success = ExchangeAccountInformationStorage.getInstance().addAccount(raplaUsername, exchangeUsername, exchangePassword, downloadFromExchange);
		String returnMessage;
		if(success) {
			// Synchronize this user after registering
			try {
				syncUser(raplaUsername);
			} catch (RaplaException e) {
				logException(e);
			} 
			returnMessage = "Your registration was successful! " + ExchangeAccountInformationStorage.getInstance().getSMTPAddressForRaplaUser(raplaUsername);
		}
		else {
			returnMessage = "An error occurred - You are not registred!";
		}
		return returnMessage;
	}

	private void syncUser(String raplaUsername) throws RaplaException {
        SynchronisationManager.logInfo("Invoked change sync for user "+raplaUsername);
        Thread thread = new Thread(new SyncUserHandler(clientFacade, clientFacade.getUser(raplaUsername)), "SyncUserThread");
		thread.start();
	}

	public String removeExchangeUser(String raplaUsername) throws RaplaException{
		String returnMessage;
		if(ExchangeAccountInformationStorage.getInstance().removeAccount(raplaUsername)){
			returnMessage = "Your account is no longer synchronized with the Exchange Server!";
		}
		else{
			returnMessage = "The action could not be performed correctly!";
		}
		return returnMessage;
	}
	
	public synchronized String completeReconciliation() throws RaplaException{
		String returnMessage = "Sync all encountered a problem";
		try {
			scheduledDownloadTimer.cancel();
			Thread completeReconciliationThread = new Thread(new CompleteReconciliationHandler(clientFacade), "CompleteReconciliationThread");
			completeReconciliationThread.start();
			while (completeReconciliationThread.isAlive()) {
				this.wait(100);
			}
			initScheduledDownloadTimer();
			returnMessage =  "Synchronization of all  items is finished";
		} catch (InterruptedException e) {
			logException(e);
		}
		return returnMessage;
	}

    public void dataChanged(ModificationEvent evt) throws RaplaException {
        SynchronisationManager.logInfo("Invoked data change handler for " + evt.getChanged().size() + " objects");
        Thread changeHandlerThread = new Thread(new ChangeHandler(evt, clientFacade), "ChangeHandlerThread");
        changeHandlerThread.start();
    }

    public boolean isInvokedOnAWTEventQueue() {
		return false;
	}

    public IMessageServer getMessageServer() {
        return messageServer;
    }

    public static void logInfo(String s) {
        getInstance().getLogger().info("Exchange Connector Plugin: " +s);
    }

    public boolean isExchangeAvailable() throws RaplaException {
        final EWSProxy ewsProxy;
        try {
            ewsProxy = new EWSProxy(clientFacade, getUser());

        } catch (Exception e) {
            throw new RaplaException(e);
        }
        return ewsProxy.getService() != null;
    }

    public boolean setDownloadFromExchange(String raplaUsername, boolean downloadFromExchange) {
        final boolean result = ExchangeAccountInformationStorage.getInstance().setDownloadFromExchange(raplaUsername, downloadFromExchange);
        if (result)
            ExchangeAccountInformationStorage.getInstance().save();
        return result;
    }
}
