package org.rapla.plugin.exchangeconnector;

import org.rapla.client.ClientServiceContainer;
import org.rapla.client.RaplaClientExtensionPoints;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContextException;
import org.rapla.plugin.exchangeconnector.client.ExchangeConnectorAdminOptions;
import org.rapla.plugin.exchangeconnector.client.ExchangeConnectorUserOptions;


public class ExchangeConnectorPlugin implements PluginDescriptor<ClientServiceContainer>, ExchangeConnectorConfig {

	public final static boolean ENABLE_BY_DEFAULT = false;

    public static final String PLUGIN_CLASS = ExchangeConnectorPlugin.class.getName();
 

    public String toString() {
        return "Exchange-Connector";
    }

    /* (non-Javadoc)
      * @see org.rapla.framework.PluginDescriptor#provideServices(org.rapla.framework.Container, org.apache.avalon.framework.configuration.Configuration)
      */
    public void provideServices(ClientServiceContainer container, Configuration config) throws RaplaContextException {
        container.addContainerProvidedComponent(RESOURCE_FILE, I18nBundleImpl.class, I18nBundleImpl.createConfig(RESOURCE_FILE.getId()));
        container.addContainerProvidedComponent(RaplaClientExtensionPoints.PLUGIN_OPTION_PANEL_EXTENSION, ExchangeConnectorAdminOptions.class);
        if (config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT)) {
            //loadConfigParameters(config);
            container.addContainerProvidedComponent(RaplaClientExtensionPoints.USER_OPTION_PANEL_EXTENSION, ExchangeConnectorUserOptions.class);
        }
    }

}
