package org.rapla.plugin.exchangeconnector.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;

import org.rapla.components.layout.TableLayout;
import org.rapla.entities.configuration.Preferences;
import org.rapla.facade.CalendarSelectionModel;
import org.rapla.facade.ClassifiableFilter;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.DefaultPluginOption;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.RaplaButton;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorConfig;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote;

/**
 * @author lutz
 */
public class ExchangeConnectorUserOptions extends DefaultPluginOption  {

   // private static final String DEFAULT_DISPLAYED_VALUE = "******";
    private Preferences preferences;

    //private String exchangeUsername;
//    private String exchangePassword;
    //private boolean downloadFromExchange;
    //private boolean enableSynchronisation;
    //FilterEditButton filter;
    RaplaButton loginButton = new RaplaButton();
    RaplaButton removeButton = new RaplaButton();
    RaplaButton syncButton = new RaplaButton();
    private JPanel optionsPanel;
    //private JCheckBox enableSynchronisationBox;
    //private JCheckBox downloadFromExchangeBox;
    //private JLabel securityInformationLabel;
    //private JLabel filterCategoryLabel;
    //private JTextField filterCategoryField;
    //private String filterCategory;
    //private JLabel eventTypesLabel;
//    private JList eventTypesList;
    ExchangeConnectorRemote service;

    public ExchangeConnectorUserOptions(RaplaContext raplaContext,ExchangeConnectorRemote service) throws Exception {
        super(raplaContext);
        setChildBundleName(ExchangeConnectorConfig.RESOURCE_FILE);
        this.service = service;
    }

    public JComponent getComponent() {
        return optionsPanel;
    }

    public String getName(Locale locale) {
        return "Exchange Connector";
    }

    public void show() throws RaplaException {
        initJComponents();
        setValuesToJComponents();
    }



    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public void commit() throws RaplaException {
//        if (applyUsersettings()) {
//            saveUsersettings();
//        }

    	//String exchangeUsername = usernameTextField.getText();
    	//String exchangePassword = new String(passwordTextField.getPassword());
//        preferences.putEntry(ExchangeConnectorConfig.ENABLED_BY_USER_KEY,enableSynchronisationBox.isSelected());
        //preferences.putEntry(ExchangeConnectorConfig.SYNC_FROM_EXCHANGE_ENABLED_KEY, downloadFromExchangeBox.isSelected());
//        preferences.putEntry(ExchangeConnectorConfig.EXCHANGE_INCOMING_FILTER_CATEGORY_KEY, filterCategoryField.getText());
//        preferences.putEntry(ExchangeConnectorConfig.EXPORT_EVENT_TYPE_KEY, getSelectedEventTypeKeysAsCSV());

        //getWebservice(ExchangeConnectorRemote.class).setDownloadFromExchange(downloadFromExchangeBox.isSelected());
   
    }

//    private void selectEventTypesInListFromCSV(String csv) {
//        if (csv == null || csv.length() == 0 || csv.equalsIgnoreCase(ExchangeConnectorConfig.DEFAULT_EXPORT_EVENT_TYPE))
//            eventTypesList.addSelectionInterval(0, eventTypesList.getModel().getSize() - 1);
//        else
//            for (int i = 0; i < eventTypesList.getModel().getSize(); i++) {
//                final StringWrapper<DynamicType> elementAt = (StringWrapper<DynamicType>) eventTypesList.getModel().getElementAt(i);
//                if (csv.contains(
//                        elementAt.forObject.getElementKey())) {
//                    eventTypesList.addSelectionInterval(i, i);
//                }
//
//            }
//    }

//    private String getSelectedEventTypeKeysAsCSV() {
//        StringBuilder b = new StringBuilder();
//        Object[] selectedValues = eventTypesList.getSelectedValues();
//        for (int i = 0, selectedValuesLength = selectedValues.length; i < selectedValuesLength; i++) {
//            Object o = selectedValues[i];
//            StringWrapper<DynamicType> et = (StringWrapper<DynamicType>) o;
//            b.append(et.forObject.getElementKey());
//            if (i < selectedValues.length - 1) {
//                b.append(",");
//            }
//        }
//        return b.toString();
//    }

    /**
     *
     */
//    private void saveUsersettings() {
//        try {
//            String returnedMessageString;
//            if (enableSynchronisationBox.isSelected()) {
//                returnedMessageString = getWebservice(ExchangeConnectorRemote.class).addExchangeUser( exchangeUsername, exchangePassword/*, Boolean.valueOf(downloadFromExchange)*/);
//            } else {
//                returnedMessageString = getWebservice(ExchangeConnectorRemote.class).removeExchangeUser();
//            }
//            JOptionPane.showMessageDialog(
//                    getMainComponent(), returnedMessageString, "Information", JOptionPane.INFORMATION_MESSAGE);
//        } catch (RaplaException e) {
//            JOptionPane.showMessageDialog(
//                    getMainComponent(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
//
//            getLogger().error("The operation was not successful!", e);
//        }
//    }

    /**
     * Read the values from the user input
     *
     * @return boolean indicating if the user entered username and password
     */
//    private boolean applyUsersettings() {
//        // if was not enabled before and has not been enabled now just return
//        if (!enableSynchronisation && !enableSynchronisationBox.isSelected())
//            return false;
//        if (enableSynchronisationBox.isSelected() &&
//                (usernameTextField.getText().equals(DEFAULT_DISPLAYED_VALUE)
//                        || passwordTextField.getParent().toString().equals(DEFAULT_DISPLAYED_VALUE)))
//            return false;
//        else {
//            this.exchangeUsername = usernameTextField.getText();
//            this.exchangePassword = new String(passwordTextField.getPassword());
//            //this.downloadFromExchange = downloadFromExchangeBox.isSelected();
//            this.enableSynchronisation = enableSynchronisationBox.isSelected();
//            this.eventTypeKeys = getSelectedEventTypeKeysAsCSV();
//
//            return true;
//        }
//    }

 
    private void initJComponents() throws RaplaException {
        this.optionsPanel = new JPanel();
        boolean isResourceSelection =false;
		ClassifiableFilter model = createModel();
		ChangeListener listener = null;
		
		//filter = new FilterEditButton(getContext(), model, listener, isResourceSelection);
        //this.filterCategoryField = new JTextField();
        //this.eventTypesLabel = new JLabel(getString("event.raplatypes"));
//        this.eventTypesList = new JList();
//        this.eventTypesList.setVisibleRowCount(5);
//        this.eventTypesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        //this.enableSynchronisationBox = new JCheckBox(getString("enable.sync.rapla.exchange"));
        UpdateComponentsListener updateComponentsListener = new UpdateComponentsListener();
        //this.enableSynchronisationBox.addActionListener(updateComponentsListener);
      //  this.downloadFromExchangeBox = new JCheckBox(getString("enable.sync.exchange.rapla"));
//        this.downloadFromExchangeBox.addActionListener(updateComponentsListener);
        //this.securityInformationLabel = new JLabel(getString("security.info"));
        //this.filterCategoryLabel = new JLabel(getString("category.filter"));

        double[][] sizes = new double[][]{
                {5, TableLayout.PREFERRED, 5, TableLayout.FILL, 5},
                {TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED}};

        TableLayout tableLayout = new TableLayout(sizes);
        this.optionsPanel.setLayout(tableLayout);
        //this.optionsPanel.add(this.enableSynchronisationBox, "1,0");
        //  this.optionsPanel.add(this.usernameLabel, "1,2");
//        this.optionsPanel.add(this.usernameTextField, "3,2");
//        this.optionsPanel.add(this.passwordLabel, "1,4");
//        this.optionsPanel.add(this.passwordTextField, "3,4");
//        this.optionsPanel.add(this.eventTypesLabel, "1,6");
        //this.optionsPanel.add(filter.getButton(), "3,6");
        this.optionsPanel.add(new JLabel("Re-Sync User"), "1, 8");
        final JButton syncButton = new JButton("Re-Sync User");
        syncButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	
            	boolean modal = true;
				String[] options = new String[] {"connect","abort"};
				SyncDialog content = new SyncDialog();
				content.init("");
				try
				{
					DialogUI dialog = DialogUI.create(getContext(), getComponent(), modal, content, options);
					dialog.start();
					int selectedIndex = dialog.getSelectedIndex();
					if ( selectedIndex == 0)
					{
						String username = content.getUsername();
						String password = content.getPassword();
					     try {
					    	 final String returnedMessageString = service.changeUser(username,password);
					    	 getLogger().info(returnedMessageString);
					     } catch (RaplaException ex) {
		                	 showException(ex, getMainComponent());
		                     getLogger().error("The operation was not successful!", ex);
		                 }
					}
				}
				catch (RaplaException ex)
				{
					 showException(ex, getMainComponent());
                     getLogger().error("The operation was not successful!", ex);
				}
//                 if (JOptionPane.showConfirmDialog(getMainComponent(),
//                         getString("button.sync.user.confirm"))==JOptionPane.YES_OPTION) {
//                 try {
//                	 String password = new String( passwordTextField.getPassword());
//                	 String username = usernameTextField.getText();
//                	 DialogUI dialog = DialogUI.create(getContext(), getComponent(), false,"Exchange result",returnedMessageString);
//                	 dialog.start();
////                     JOptionPane.showMessageDialog(
////                             getMainComponent(), returnedMessageString, "Information", JOptionPane.INFORMATION_MESSAGE);
//                 } catch (RaplaException ex) {
//                	 showException(ex, getMainComponent());
//                     getLogger().error("The operation was not successful!", ex);
//                 }
            	
            }
        });   
        this.optionsPanel.add(syncButton, "3, 8");
      //  this.optionsPanel.add(this.downloadFromExchangeBox, "1,8");
        //this.optionsPanel.add(this.filterCategoryLabel, "1,10");
        //this.optionsPanel.add(this.filterCategoryField, "3,10");
        //this.optionsPanel.add(this.securityInformationLabel, "3,12");
    }

    private ClassifiableFilter createModel() throws RaplaException {
    	CalendarSelectionModel model = getModification().newCalendarModel(getUser());
    	return model;
	}

	/**
     *
     */
    

    private void updateComponentState() {
        //
        //enableSynchronisationBox.setEnabled(ExchangeConnectorConfig.ENABLED_BY_ADMIN);
        //syncButton.setEnabled( enableSynchronisationBox.isSelected());
        //passwordTextField.setEnabled(enableSynchronisationBox.isSelected());
        //downloadFromExchangeBox.setEnabled(ExchangeConnectorConfig.ENABLED_BY_ADMIN && enableSynchronisationBox.isSelected());
        //filterCategoryField.setEnabled(ExchangeConnectorConfig.ENABLED_BY_ADMIN && enableSynchronisationBox.isSelected() && downloadFromExchangeBox.isSelected());
        //filter.getButton().setEnabled( enableSynchronisationBox.isSelected());
    }

    /**
     *
     */
    private void setValuesToJComponents() {
    	//boolean    enableSynchronisation = preferences.getEntryAsBoolean(ExchangeConnectorConfig.ENABLED_BY_USER_KEY, ExchangeConnectorConfig.DEFAULT_ENABLED_BY_USER);
            //downloadFromExchange = preferences.getEntryAsBoolean(ExchangeConnectorConfig.SYNC_FROM_EXCHANGE_ENABLED_KEY, ExchangeConnectorConfig.DEFAULT_SYNC_FROM_EXCHANGE_ENABLED);
            //filterCategory = preferences.getEntryAsString(ExchangeConnectorConfig.EXCHANGE_INCOMING_FILTER_CATEGORY_KEY, ExchangeConnectorConfig.DEFAULT_EXCHANGE_INCOMING_FILTER_CATEGORY);
    	//String eventTypeKeys = preferences.getEntryAsString(ExchangeConnectorConfig.EXPORT_EVENT_TYPE_KEY, ExchangeConnectorConfig.DEFAULT_EXPORT_EVENT_TYPE);
//    	usernameTextField.setText( preferences.getEntryAsString( ExchangeConnectorConfig.USERNAME, ""));
//        passwordTextField.setText( preferences.getEntryAsString( ExchangeConnectorConfig.PASSWORD, ""));
        //enableSynchronisationBox.setSelected( enableSynchronisation);
        //downloadFromExchangeBox.setSelected(ExchangeConnectorConfig.ENABLED_BY_ADMIN && downloadFromExchange);
        //filterCategoryField.setText(filterCategory);

//        final DefaultListModel model = new DefaultListModel();
//        try {
//            DynamicType[] dynamicTypes = getClientFacade().getDynamicTypes(DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION);
//			for (DynamicType event : dynamicTypes) {
//                // event type of "import from exchange" will (for now not) be ignored!
// //               String elementKey = event.getElementKey();
////				String iMPORT_EVENT_TYPE = ExchangeConnectorConfig.IMPORT_EVENT_TYPE;
////				if (!iMPORT_EVENT_TYPE.equalsIgnoreCase(elementKey))
//                    model.addElement(new StringWrapper<DynamicType>(event));
//            }
//        } catch (RaplaException e) {
//        }
//        eventTypesList.setModel(new SortedListModel(model));
//        eventTypesList.setModel(model);
//        selectEventTypesInListFromCSV(eventTypeKeys);
        updateComponentState();
    }

    /**
     * @see org.rapla.gui.DefaultPluginOption#getPluginClass()
     */
    public Class<? extends PluginDescriptor<?>> getPluginClass() {
        return ExchangeConnectorPlugin.class;
    }

    private class UpdateComponentsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updateComponentState();
        }
    }
    
    class SyncDialog extends JPanel
    {
        private JTextField usernameTextField;
        private JPasswordField passwordTextField;
        private JLabel usernameLabel;
        private JLabel passwordLabel;
        {
            this.usernameTextField = new JTextField();
            this.passwordTextField = new JPasswordField();
            this.passwordLabel = new JLabel(getString("password.server"));
            this.usernameLabel = new JLabel(getString("username.server"));
            double[][] sizes = new double[][]{
                    {5, TableLayout.PREFERRED, 5, 200, 5},
                    {TableLayout.PREFERRED, 5,TableLayout.PREFERRED}
            };
            setLayout( new TableLayout( sizes));
            add( usernameLabel, "1,0");
            add( usernameTextField, "3,0");
            add( passwordLabel, "1,2");
            add( passwordTextField, "3,2");
        }
        
        public void init(String username)
        {
        	usernameTextField.setText( username);
        }
        
        public String getUsername()
        {
        	return usernameTextField.getText();
        }
        public String getPassword()
        {
        	return new String(passwordTextField.getPassword());
        }
        
        
    }
    
}






