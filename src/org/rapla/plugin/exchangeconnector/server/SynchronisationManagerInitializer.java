package org.rapla.plugin.exchangeconnector.server;

import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.server.ServerExtension;

/** Starts the synchronization manager when the server is started*/
public class SynchronisationManagerInitializer extends RaplaComponent implements ServerExtension{
	SynchronisationManager manager;
	public SynchronisationManagerInitializer(RaplaContext context) throws RaplaContextException {
		super(context);
		manager = context.lookup( SynchronisationManager.class);
	}

}
