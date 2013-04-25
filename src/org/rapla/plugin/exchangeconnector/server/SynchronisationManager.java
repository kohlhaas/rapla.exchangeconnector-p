package org.rapla.plugin.exchangeconnector.server;

import java.util.Timer;

import org.rapla.facade.AllocationChangeEvent;
import org.rapla.facade.AllocationChangeListener;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAccountInformationStorage;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;
import org.rapla.plugin.exchangeconnector.server.worker.AppointmentTask;
import org.rapla.plugin.exchangeconnector.server.worker.EWSWorker;
import org.rapla.server.ServerExtension;

/**    
 * @author Alex Heil, Dominik Joder, Lutz Bergendahl, Matthias Hundt
 * @see {@link ExchangeAppointmentStorage}
 * @see {@link ExchangeAccountInformationStorage}
 */
public class SynchronisationManager extends RaplaComponent implements AllocationChangeListener, ModificationListener, ServerExtension {
	private static SynchronisationManager synchronisationManagerInstance;

    /**
	 * The constructor 
	 * 
	 * @param context : {@link RaplaContext}
	 * @throws RaplaException
	 */
	public SynchronisationManager(RaplaContext context) throws RaplaException {
		super(context);
        synchronisationManagerInstance = this;
		
		final ClientFacade clientFacade =  context.lookup(ClientFacade.class);
		clientFacade.addAllocationChangedListener(this);

        final Timer scheduledDownloadTimer = new Timer("ScheduledDownloadThread",true);
        scheduledDownloadTimer.schedule(new ScheduledDownloadHandler(context, clientFacade), 30000, ExchangeConnectorPlugin.PULL_FREQUENCY*1000);
	}


	/**
	 * @return the synchronisationManagerInstance
	 */
	public static SynchronisationManager getInstance() {
		return synchronisationManagerInstance; 
	}



	/**
	 * @return {@link ClientFacade}
	 */
	public static ClientFacade getSyncManagerClientFacade() {
		return SynchronisationManager.synchronisationManagerInstance.getClientFacade();
	}


	/**
	 * @param e
	 */
	/*public static void logException(Exception e){
		SynchronisationManager.getInstance().getLogger().error("Exception in the ExchangeConnector Plugin: ", e);
	}*/

	/**
	 * This method is called when an appointment change has been triggered
	 * 
	 * @param changeEvents : Array of {@link AllocationChangeEvent} with all changes that happened
	 * @see org.rapla.facade.AllocationChangeListener#changed(org.rapla.facade.AllocationChangeEvent[])
	 */
	public synchronized void changed(AllocationChangeEvent[] changeEvents) {

	}
	
	public String addExchangeUser(String raplaUsername, String exchangeUsername, String exchangePassword, Boolean downloadFromExchange) {
        getLogger().debug("Invoked add exchange user for rapla " + raplaUsername + " with exchange user " + exchangeUsername);
        String returnMessage;

			// Synchronize this user after registering
			try {
                ExchangeAccountInformationStorage.getInstance().addAccount(raplaUsername, exchangeUsername, exchangePassword, downloadFromExchange);

                syncUser(raplaUsername);
                returnMessage = "Your registration was successful! " + ExchangeAccountInformationStorage.getInstance().getSMTPAddressForRaplaUser(raplaUsername);

            } catch (Exception e) {
                returnMessage = "An error occurred - You are not registered!";

                getLogger().error(e.getMessage(),e);
			} 
		return returnMessage;
	}

	private void syncUser(String raplaUsername) throws RaplaException {
        getLogger().debug("Invoked change sync for user " + raplaUsername);
        final AppointmentTask task = new AppointmentTask(getContext());
        task.downloadUserAppointments(getClientFacade().getUser(raplaUsername));
        
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
		String returnMessage = "Disabled!";
		/*String returnMessage = "Sync all encountered a problem";
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
		}*/
		return returnMessage;
	}

    public synchronized void dataChanged(ModificationEvent evt) throws RaplaException {
        getLogger().debug("Invoked data change handler for " + evt.getChanged().size() + " objects");
        final AppointmentTask reservationChangedTask = new AppointmentTask(getContext());
        reservationChangedTask.synchronize(evt);

/*
        Thread changeHandlerThread = new Thread(new ChangeHandler(evt, clientFacade), "ChangeHandlerThread");
        //changeHandlerThread.start();
        changeHandlerThread.run();
*/
    }

    public boolean isInvokedOnAWTEventQueue() {
		return false;
	}

/*
    public IMessageServer getMessageServer() {
        return messageServer;
    }
*/

   public static void logInfo(String s) {
        getInstance().getLogger().info("Exchange Connector Plugin: " +s);
    }

    public boolean isExchangeAvailable() throws RaplaException {
        final EWSWorker ewsProxy;
        try {
            ewsProxy = new EWSWorker(getContext(), getUser());

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
