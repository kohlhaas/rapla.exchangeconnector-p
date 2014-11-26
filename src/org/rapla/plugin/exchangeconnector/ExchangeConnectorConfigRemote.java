package org.rapla.plugin.exchangeconnector;

import java.util.List;

import javax.jws.WebService;

import org.rapla.framework.DefaultConfiguration;
import org.rapla.framework.RaplaException;

@WebService
public interface ExchangeConnectorConfigRemote 
{
	public DefaultConfiguration getConfig() throws RaplaException;
	public List<String> getTimezones() throws RaplaException;

}