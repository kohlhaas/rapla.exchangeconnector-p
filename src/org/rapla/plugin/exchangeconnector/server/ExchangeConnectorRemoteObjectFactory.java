package org.rapla.plugin.exchangeconnector.server;

import org.rapla.entities.User;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.Configuration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;
import org.rapla.server.RemoteMethodFactory;
import org.rapla.server.RemoteSession;

public class ExchangeConnectorRemoteObjectFactory extends RaplaComponent implements RemoteMethodFactory<ExchangeConnectorRemote>{
	ExchangeAppointmentStorage appointmentStorage;
	ExchangeConnectorConfig.ConfigReader config;
			
	public ExchangeConnectorRemoteObjectFactory(RaplaContext context,Configuration config) throws RaplaContextException {
		super(context);
		this.config = new ExchangeConnectorConfig.ConfigReader( config );
		appointmentStorage = context.lookup( ExchangeAppointmentStorage.class);
	}

	@Override
	public ExchangeConnectorRemote createService(RemoteSession remoteSession) throws RaplaContextException  {
		User user  = remoteSession.getUser();
		return new ExchangeConnectorRemoteObject(getContext(), config,user);
	}

}
