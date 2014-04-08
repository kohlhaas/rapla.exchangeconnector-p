package org.rapla.plugin.exchangeconnector;

import org.rapla.client.ClientServiceContainer;
import org.rapla.client.RaplaClientExtensionPoints;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContextException;
import org.rapla.plugin.exchangeconnector.client.ExchangeConnectorAdminOptions;
import org.rapla.plugin.exchangeconnector.client.ExchangeConnectorUserOptions;
import org.rapla.plugin.exchangeconnector.client.ExchangeExtensionFactory;


public class ExchangeConnectorPlugin implements PluginDescriptor<ClientServiceContainer>, ExchangeConnectorConfig {

	public final static boolean ENABLE_BY_DEFAULT = false;

    public static final String PLUGIN_CLASS = ExchangeConnectorPlugin.class.getName();
    public static final String EXCHANGE_EXPORT = PLUGIN_CLASS+".selected";
    
    public String toString() {
        return "Exchange-Connector";
    }

    public void provideServices(ClientServiceContainer container, Configuration config) throws RaplaContextException {
        container.addContainerProvidedComponent(RESOURCE_FILE, I18nBundleImpl.class, I18nBundleImpl.createConfig(RESOURCE_FILE.getId()));
        container.addContainerProvidedComponent(RaplaClientExtensionPoints.PLUGIN_OPTION_PANEL_EXTENSION, ExchangeConnectorAdminOptions.class);
        if (config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT)) {
            container.addContainerProvidedComponent(RaplaClientExtensionPoints.USER_OPTION_PANEL_EXTENSION, ExchangeConnectorUserOptions.class);
    	    container.addContainerProvidedComponent( RaplaClientExtensionPoints.PUBLISH_EXTENSION_OPTION, ExchangeExtensionFactory.class);
        }
    }

}
