package org.rapla.plugin.exchangeconnector.server;

import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContextException;
import org.rapla.plugin.RaplaServerExtensionPoints;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.server.ServerServiceContainer;


public class ExchangeConnectorServerPlugin implements PluginDescriptor<ServerServiceContainer> {

    
    /* (non-Javadoc)
      * @see org.rapla.framework.PluginDescriptor#provideServices(org.rapla.framework.Container, org.apache.avalon.framework.configuration.Configuration)
      */
    public void provideServices(ServerServiceContainer container, Configuration config) throws RaplaContextException {
        container.addContainerProvidedComponent(ExchangeConnectorPlugin.RESOURCE_FILE, I18nBundleImpl.class, I18nBundleImpl.createConfig(ExchangeConnectorPlugin.RESOURCE_FILE.getId()));
        if (!config.getAttributeAsBoolean("enabled", ExchangeConnectorPlugin.ENABLE_BY_DEFAULT)) {
        	return;
        }
        ExchangeConnectorPlugin.loadConfigParameters(config);
        container.addContainerProvidedComponent(RaplaServerExtensionPoints.SERVER_EXTENSION, SynchronisationManager.class);
        container.addRemoteMethodFactory(ExchangeConnectorRemote.class, ExchangeConnectorRemoteObject.class);
    }



}
