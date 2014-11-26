package org.rapla.plugin.exchangeconnector.server;

import java.util.List;

import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.DefaultConfiguration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfigRemote;
import org.rapla.server.RemoteMethodFactory;
import org.rapla.server.RemoteSession;
import org.rapla.storage.RaplaSecurityException;

public class ExchangeConnectorRemoteConfigFactory extends RaplaComponent implements RemoteMethodFactory<ExchangeConnectorConfigRemote>{
			
	public ExchangeConnectorRemoteConfigFactory(RaplaContext context) {
		super(context);
	}

	@Override
	public ExchangeConnectorConfigRemote createService(final RemoteSession remoteSession) throws RaplaContextException  {
		return new ExchangeConnectorConfigRemote() {
					
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
            
            @SuppressWarnings("unused")
            public List<String> getTimezones() throws RaplaException 
            {
                return ExchangeConnectorServerPlugin.TIMEZONES;
            }
            
		};
	}
	

}
