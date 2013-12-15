package org.rapla.plugin.exchangeconnector.server;

import org.rapla.facade.ClientFacade;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.server.worker.AppointmentTask;
import org.rapla.server.ServerExtension;

public class SynchronisationManager extends RaplaComponent implements ModificationListener, ServerExtension {

	final AppointmentTask appointmentTask;
	public SynchronisationManager(RaplaContext context) throws RaplaException {
		super(context);
		
		final ClientFacade clientFacade =  context.lookup(ClientFacade.class);
		clientFacade.addModificationListener(this);
		appointmentTask = context.lookup( AppointmentTask.class);
        //final Timer scheduledDownloadTimer = new Timer("ScheduledDownloadThread",true);
        //scheduledDownloadTimer.schedule(new ScheduledDownloadHandler(context, clientFacade, getLogger()), 30000, ExchangeConnectorPlugin.PULL_FREQUENCY*1000);
	}
	
	public synchronized void dataChanged(ModificationEvent evt) throws RaplaException {
		appointmentTask.synchronize(evt);
	}


}
