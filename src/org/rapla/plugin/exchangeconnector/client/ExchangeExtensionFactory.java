package org.rapla.plugin.exchangeconnector.client;

import java.beans.PropertyChangeListener;

import org.rapla.facade.CalendarSelectionModel;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.PublishExtension;
import org.rapla.gui.PublishExtensionFactory;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote;

public class ExchangeExtensionFactory extends RaplaComponent implements PublishExtensionFactory
{
	ExchangeConnectorRemote remote;
	public ExchangeExtensionFactory(RaplaContext context, ExchangeConnectorRemote remote)
	{
		super(context);
		this.remote = remote;
	}

	public PublishExtension creatExtension(CalendarSelectionModel model,
			PropertyChangeListener revalidateCallback) throws RaplaException 
	{
		return new ExchangePublishExtension(getContext(), model,remote);
	}

	
}
