package org.rapla.plugin.exchangeconnector.client;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.components.layout.TableLayout;
import org.rapla.facade.CalendarSelectionModel;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.PublishExtension;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote;
import org.rapla.plugin.exchangeconnector.SynchronizationStatus;

class ExchangePublishExtension extends RaplaGUIComponent implements PublishExtension
{
	JPanel panel = new JPanel();
	CalendarSelectionModel model;
	final JCheckBox checkbox;
    final JTextField dummyURL;
	 
	public ExchangePublishExtension(RaplaContext context, CalendarSelectionModel model, ExchangeConnectorRemote remote)  {
		super(context);
		this.model = model;
		setChildBundleName(ExchangeConnectorPlugin.RESOURCE_FILE);
        panel.setLayout(new TableLayout( new double[][] {{TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.FILL},
                {TableLayout.PREFERRED,5,TableLayout.PREFERRED       }}));
        dummyURL = new JTextField();

    	checkbox = new JCheckBox(getString("exchange.publish"));
    	checkbox.addChangeListener(new ChangeListener()
    	{
           public void stateChanged(ChangeEvent e)
           {
           }
    	});
        panel.add(checkbox,"0,0");
        boolean enabled = false;
        try
        {
        	SynchronizationStatus synchronizationStatus = remote.getSynchronizationStatus();
        	enabled = synchronizationStatus.enabled;
        }
        catch (RaplaException ex)
        {
        	getLogger().error( ex.getMessage(), ex);
        }
        checkbox.setEnabled( enabled);
        final String entry = model.getOption(ExchangeConnectorPlugin.EXCHANGE_EXPORT);
        boolean selected = entry != null && entry.equals("true");
		checkbox.setSelected( selected);
	}
	
	public JPanel getPanel() 
	{
		return panel;
	}

	public void mapOptionTo() 
	{
		 final String selected = checkbox.isSelected() ? "true" : "false";
         model.setOption( ExchangeConnectorPlugin.EXCHANGE_EXPORT, selected);
	}
	
	public JTextField getURLField() 
	{
		return dummyURL;
	}

	public boolean hasAddressCreationStrategy() {
		return false;
	}

	public String getAddress(String filename, String generator) {
		return null;
	}
	
	public String getGenerator() {
	     return "exchange";
	}
	

}

