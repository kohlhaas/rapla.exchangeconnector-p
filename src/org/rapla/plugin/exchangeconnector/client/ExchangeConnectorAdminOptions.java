package org.rapla.plugin.exchangeconnector.client;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.*;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.layout.TableLayout;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.DefaultPluginOption;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote;


public class ExchangeConnectorAdminOptions extends DefaultPluginOption implements ActionListener{
	
	private JCheckBox enableSynchronisationBox;// = new JCheckBox();
	private JTextField exchangeWebServiceFQDNTextField ;//= new JTextField();
	private JTextField categoryForRaplaAppointmentsOnExchangeTextField ;//= new JTextField();
	private RaplaNumber syncIntervalPast;// = new RaplaNumber();
	private RaplaNumber syncIntervalFuture;// = new RaplaNumber();
	private RaplaNumber pullFrequency;// = new RaplaNumber();
	private JButton syncallButton;// = new JButton("(Re-)Sync all");
	private JTextArea infoBox ;//= new JTextArea( "Syncronize all Appointments in chosen\n" + "period of time for all user accounts,\n" + "on which syncing is enabled.");

	private JLabel enableSynchronisationLabel;// = new JLabel("Provide syncing to MS Exchange Server");
	private JLabel syncIntervalFutureLabel;// = new JLabel("Synchronise months in future");
	private JLabel pullFrequencyLabel;//= new JLabel("Pull appointments from Exchange in seconds");
	private JLabel syncIntervalPastLabel;// = new JLabel("Synchronise months in past");
	private JLabel categoryForRaplaAppointmentsOnExchangeLabel;// = new JLabel("Default Category on Exchange");
	private JLabel exchangeWebServiceFQDNLabel;// = new JLabel("Exchange-Webservice FQDN");
    private JLabel eventTypeLabel;
    private JComboBox cbEventTypes;

    public ExchangeConnectorAdminOptions( RaplaContext raplaContext ) throws Exception
    {
        super( raplaContext );
        setChildBundleName(ExchangeConnectorPlugin.RESOURCE_FILE);
        initJComponents();

    }
 

    private void initJComponents() {
    	this.enableSynchronisationBox = new JCheckBox();
    	this.exchangeWebServiceFQDNTextField = new JTextField();
    	this.categoryForRaplaAppointmentsOnExchangeTextField = new JTextField();
    	this.syncIntervalPast = new RaplaNumber();
    	this.syncIntervalFuture = new RaplaNumber();
    	this.pullFrequency = new RaplaNumber();
    	this.syncallButton = new JButton(getString("button.sync"));
    	this.infoBox = new JTextArea(getString("infobox.sync"));

    	this.enableSynchronisationLabel = new JLabel(getString("sync.msexchange"));
    	this.syncIntervalFutureLabel = new JLabel(getString("sync.future"));
    	this.pullFrequencyLabel = new JLabel(getString("server.frequency"));
    	this.syncIntervalPastLabel = new JLabel(getString("sync.past"));
    	this.categoryForRaplaAppointmentsOnExchangeLabel = new JLabel(getString("appointment.category"));
    	this.exchangeWebServiceFQDNLabel = new JLabel(getString("msexchange.hosturl"));

        this.eventTypeLabel = new JLabel(getString("event.raplatype"));
        this.cbEventTypes = new JComboBox();


    }


	/* (non-Javadoc)
     * @see org.rapla.gui.DefaultPluginOption#createPanel()
     */
    protected JPanel createPanel() throws RaplaException {
        JPanel parentPanel = super.createPanel();
        JPanel content = new JPanel();
        double[][] sizes = new double[][] {
            {5,TableLayout.PREFERRED, 5,TableLayout.FILL,5}
            ,{TableLayout.PREFERRED,5,TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED,5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED}
        };
        TableLayout tableLayout = new TableLayout(sizes);
        content.setLayout(tableLayout); 
        content.add(enableSynchronisationLabel, "1,0");
        content.add(enableSynchronisationBox,"3,0");
		content.add(exchangeWebServiceFQDNLabel, "1,2");
        content.add(exchangeWebServiceFQDNTextField, "3,2");
		content.add(categoryForRaplaAppointmentsOnExchangeLabel, "1,4");
        content.add(categoryForRaplaAppointmentsOnExchangeTextField, "3,4");
		content.add(syncIntervalPastLabel, "1,6");
        content.add(syncIntervalPast, "3,6");
		content.add(syncIntervalFutureLabel, "1,8");
        content.add(syncIntervalFuture, "3,8");
		content.add(pullFrequencyLabel, "1,10");
        content.add(pullFrequency, "3,10");
        content.add(syncallButton, "1,12");
        content.add(infoBox, "3,12");
        content.add(eventTypeLabel, "1,14");
        content.add(cbEventTypes, "3,14");

        syncallButton.addActionListener(this);
        
        parentPanel.add( content, BorderLayout.CENTER);
        return parentPanel;
    }
        
    protected void addChildren( DefaultConfiguration newConfig) {
    	try {
    		readUserInputValues();
    		ExchangeConnectorPlugin.storeParametersToConfig(newConfig);
    	} catch (ConfigurationException e) {
			getLogger().error("An error has occurred saving the Exchange Connector Configuration " + e.getMessage());
		}

	}

    private void readUserInputValues() {
		ExchangeConnectorPlugin.ENABLED_BY_ADMIN = enableSynchronisationBox.isSelected();
		ExchangeConnectorPlugin.EXCHANGE_WS_FQDN = exchangeWebServiceFQDNTextField.getText();
		ExchangeConnectorPlugin.EXCHANGE_APPOINTMENT_CATEGORY = categoryForRaplaAppointmentsOnExchangeTextField.getText();
		ExchangeConnectorPlugin.SYNCING_PERIOD_PAST = syncIntervalPast.getNumber().intValue();
		ExchangeConnectorPlugin.SYNCING_PERIOD_FUTURE= syncIntervalFuture.getNumber().intValue();
		ExchangeConnectorPlugin.PULL_FREQUENCY = pullFrequency.getNumber().intValue();
        ExchangeConnectorPlugin.IMPORT_EVENT_TYPE = ((StringWrapper<DynamicType>)cbEventTypes.getSelectedItem()).forObject.getElementKey();


	}


	protected void readConfig( Configuration config){
        try {
            DynamicType[] dynamicTypes = getClientFacade().getDynamicTypes(DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
            StringWrapper<DynamicType> [] eventTypes= new StringWrapper [dynamicTypes.length];
            for (int i = 0, dynamicTypesLength = dynamicTypes.length; i < dynamicTypesLength; i++) {
                DynamicType dynamicType = dynamicTypes[i];
                eventTypes[i] = new StringWrapper<DynamicType>(dynamicType);
            }
            this.cbEventTypes.setModel(new DefaultComboBoxModel(eventTypes));
        } catch (RaplaException e) {
        }

        try {
			ExchangeConnectorPlugin.loadConfigParameters(config);
			enableSynchronisationBox.setSelected(ExchangeConnectorPlugin.ENABLED_BY_ADMIN);
			exchangeWebServiceFQDNTextField.setText(ExchangeConnectorPlugin.EXCHANGE_WS_FQDN);
			categoryForRaplaAppointmentsOnExchangeTextField.setText(ExchangeConnectorPlugin.EXCHANGE_APPOINTMENT_CATEGORY);
			syncIntervalPast.setNumber(ExchangeConnectorPlugin.SYNCING_PERIOD_PAST);
			syncIntervalFuture.setNumber(ExchangeConnectorPlugin.SYNCING_PERIOD_FUTURE);
			pullFrequency.setNumber(ExchangeConnectorPlugin.PULL_FREQUENCY);
            try {
                cbEventTypes.setSelectedItem(new StringWrapper<DynamicType>(ExchangeConnectorPlugin.getImportEventType(getClientFacade())));
            } catch (RaplaException e) {
            }
        } catch (ConfigurationException e) {
			getLogger().error("An error occurred while reading the configuration! ", e);
		}  
    }

    
    public String getDescriptorClassName() {
        return ExchangeConnectorPlugin.class.getName();
    }
    
    public String getName(Locale locale) {
        return "Exchange Connector Plugin";
    }


	public void actionPerformed(ActionEvent actionEvent) {
		if (actionEvent.getSource().equals(syncallButton)) {
			try {
				String returnedMessage = getWebservice(ExchangeConnectorRemote.class).completeReconciliation();
                JOptionPane.showMessageDialog(
                        getMainComponent(), returnedMessage, "Information", JOptionPane.INFORMATION_MESSAGE);
            } catch (RaplaException e) {
                JOptionPane.showMessageDialog(
                        getMainComponent(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

                getLogger().error("Error occurred while executing the sync-all! ", e);
			}
		}
	}
}



