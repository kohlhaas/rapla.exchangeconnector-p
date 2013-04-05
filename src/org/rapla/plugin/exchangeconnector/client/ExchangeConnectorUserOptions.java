package org.rapla.plugin.exchangeconnector.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.rapla.components.layout.TableLayout;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.DefaultPluginOption;
import org.rapla.gui.internal.edit.reservation.SortedListModel;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote;

/**
 * @author lutz
 */
public class ExchangeConnectorUserOptions extends DefaultPluginOption  {

    private static final String DEFAULT_DISPLAYED_VALUE = "******";
    private Preferences preferences;

    private String exchangeUsername;
    private String exchangePassword;
    private boolean downloadFromExchange;
    private boolean enableSynchronisation;


    private JPanel optionsPanel;
    private JTextField usernameTextField;
    private JPasswordField passwordTextField;
    private JCheckBox enableSynchronisationBox;
    private JCheckBox downloadFromExchangeBox;
    private JLabel securityInformationLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel filterCategoryLabel;
    private JTextField filterCategoryField;
    private String filterCategory;
    private JLabel eventTypesLabel;
    private JList eventTypesList;
    private String eventTypeKeys;


    public ExchangeConnectorUserOptions(RaplaContext raplaContext) throws Exception {
        super(raplaContext);
        setChildBundleName(ExchangeConnectorPlugin.RESOURCE_FILE);
    }

    public JComponent getComponent() {
        return optionsPanel;
    }

    public String getName(Locale locale) {
        return "Exchange Connector";
    }

    public void show() throws RaplaException {
        readValuesFromPrefs();
        initJComponents();
        setValuesToJComponents();
    }


    public void actionPerformed(ActionEvent e) {

    }


    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public void commit() throws RaplaException {
        if (applyUsersettings()) {
            saveUsersettings();
        }

        preferences.putEntry(ExchangeConnectorPlugin.ENABLED_BY_USER_KEY, String.valueOf(enableSynchronisationBox.isSelected()));
        preferences.putEntry(ExchangeConnectorPlugin.SYNC_FROM_EXCHANGE_ENABLED_KEY, String.valueOf(downloadFromExchangeBox.isSelected()));
        preferences.putEntry(ExchangeConnectorPlugin.EXCHANGE_INCOMING_FILTER_CATEGORY_KEY, filterCategoryField.getText());
        preferences.putEntry(ExchangeConnectorPlugin.EXPORT_EVENT_TYPE_KEY, getSelectedEventTypeKeysAsCSV());

        getWebservice(ExchangeConnectorRemote.class).setDownloadFromExchange(preferences.getOwner().getUsername(), downloadFromExchangeBox.isSelected());
        readValuesFromPrefs();

    }

    private void selectEventTypesInListFromCSV(String csv) {
        if (csv == null || csv.length() == 0 || csv.equalsIgnoreCase(ExchangeConnectorPlugin.DEFAULT_EXPORT_EVENT_TYPE))
            eventTypesList.addSelectionInterval(0, eventTypesList.getModel().getSize() - 1);
        else
            for (int i = 0; i < eventTypesList.getModel().getSize(); i++) {
                final StringWrapper<DynamicType> elementAt = (StringWrapper<DynamicType>) eventTypesList.getModel().getElementAt(i);
                if (csv.contains(
                        elementAt.forObject.getElementKey())) {
                    eventTypesList.addSelectionInterval(i, i);
                }

            }
    }

    private String getSelectedEventTypeKeysAsCSV() {
        StringBuilder b = new StringBuilder();
        Object[] selectedValues = eventTypesList.getSelectedValues();
        for (int i = 0, selectedValuesLength = selectedValues.length; i < selectedValuesLength; i++) {
            Object o = selectedValues[i];
            StringWrapper<DynamicType> et = (StringWrapper<DynamicType>) o;
            b.append(et.forObject.getElementKey());
            if (i < selectedValues.length - 1) {
                b.append(",");
            }
        }
        return b.toString();
    }

    /**
     *
     */
    private void saveUsersettings() {
        try {
            String returnedMessageString;
            if (enableSynchronisationBox.isSelected()) {
                returnedMessageString = getWebservice(ExchangeConnectorRemote.class).addExchangeUser(preferences.getOwner().getUsername(), exchangeUsername, exchangePassword, Boolean.valueOf(downloadFromExchange));
            } else {
                returnedMessageString = getWebservice(ExchangeConnectorRemote.class).removeExchangeUser(preferences.getOwner().getUsername());
            }
            JOptionPane.showMessageDialog(
                    getMainComponent(), returnedMessageString, "Information", JOptionPane.INFORMATION_MESSAGE);
        } catch (RaplaException e) {
            JOptionPane.showMessageDialog(
                    getMainComponent(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

            getLogger().error("The operation was not successful!", e);
        }
    }

    /**
     * Read the values from the user input
     *
     * @return boolean indicating if the user entered username and password
     */
    private boolean applyUsersettings() {
        // if was not enabled before and has not been enabled now just return
        if (!enableSynchronisation && !enableSynchronisationBox.isSelected())
            return false;
        if (enableSynchronisationBox.isSelected() &&
                (usernameTextField.getText().equals(DEFAULT_DISPLAYED_VALUE)
                        || passwordTextField.getParent().toString().equals(DEFAULT_DISPLAYED_VALUE)))
            return false;
        else {
            this.exchangeUsername = usernameTextField.getText();
            this.exchangePassword = new String(passwordTextField.getPassword());
            this.downloadFromExchange = downloadFromExchangeBox.isSelected();
            this.enableSynchronisation = enableSynchronisationBox.isSelected();
            this.eventTypeKeys = getSelectedEventTypeKeysAsCSV();

            return true;
        }
    }

    /**
     *
     */
    private void initJComponents() {
        this.optionsPanel = new JPanel();
        this.usernameTextField = new JTextField();
        this.passwordTextField = new JPasswordField();
        this.filterCategoryField = new JTextField();
        this.eventTypesLabel = new JLabel(getString("event.raplatypes"));
        this.eventTypesList = new JList();
        this.eventTypesList.setVisibleRowCount(5);
        this.eventTypesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        this.enableSynchronisationBox = new JCheckBox(getString("enable.sync.rapla.exchange"));
        UpdateComponentsListener updateComponentsListener = new UpdateComponentsListener();
        this.enableSynchronisationBox.addActionListener(updateComponentsListener);
        this.downloadFromExchangeBox = new JCheckBox(getString("enable.sync.exchange.rapla"));
        this.downloadFromExchangeBox.addActionListener(updateComponentsListener);
        this.securityInformationLabel = new JLabel(getString("security.info"));
        this.passwordLabel = new JLabel(getString("password.server"));
        this.usernameLabel = new JLabel(getString("username.server"));
        this.filterCategoryLabel = new JLabel(getString("category.filter"));

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
        this.optionsPanel.add(this.enableSynchronisationBox, "1,0");
        this.optionsPanel.add(this.usernameLabel, "1,2");
        this.optionsPanel.add(this.usernameTextField, "3,2");
        this.optionsPanel.add(this.passwordLabel, "1,4");
        this.optionsPanel.add(this.passwordTextField, "3,4");
        this.optionsPanel.add(this.eventTypesLabel, "1,6");
        this.optionsPanel.add(new JScrollPane(this.eventTypesList), "3,6");
        this.optionsPanel.add(this.downloadFromExchangeBox, "1,8");
        this.optionsPanel.add(this.filterCategoryLabel, "1,10");
        this.optionsPanel.add(this.filterCategoryField, "3,10");
        this.optionsPanel.add(this.securityInformationLabel, "3,12");
        updateComponentState();
    }

    /**
     *
     */
    private void readValuesFromPrefs() {
        exchangeUsername = DEFAULT_DISPLAYED_VALUE;
        exchangePassword = DEFAULT_DISPLAYED_VALUE;
        enableSynchronisation = preferences.getEntryAsBoolean(ExchangeConnectorPlugin.ENABLED_BY_USER_KEY, ExchangeConnectorPlugin.DEFAULT_ENABLED_BY_USER);
        downloadFromExchange = preferences.getEntryAsBoolean(ExchangeConnectorPlugin.SYNC_FROM_EXCHANGE_ENABLED_KEY, ExchangeConnectorPlugin.DEFAULT_SYNC_FROM_EXCHANGE_ENABLED);
        filterCategory = preferences.getEntryAsString(ExchangeConnectorPlugin.EXCHANGE_INCOMING_FILTER_CATEGORY_KEY, ExchangeConnectorPlugin.DEFAULT_EXCHANGE_INCOMING_FILTER_CATEGORY);
        eventTypeKeys = preferences.getEntryAsString(ExchangeConnectorPlugin.EXPORT_EVENT_TYPE_KEY, ExchangeConnectorPlugin.DEFAULT_EXPORT_EVENT_TYPE);
    }

    private void updateComponentState() {
        //
        enableSynchronisationBox.setEnabled(ExchangeConnectorPlugin.ENABLED_BY_ADMIN);
        usernameTextField.setEnabled(ExchangeConnectorPlugin.ENABLED_BY_ADMIN && enableSynchronisationBox.isSelected());
        passwordTextField.setEnabled(ExchangeConnectorPlugin.ENABLED_BY_ADMIN && enableSynchronisationBox.isSelected());
        downloadFromExchangeBox.setEnabled(ExchangeConnectorPlugin.ENABLED_BY_ADMIN && enableSynchronisationBox.isSelected());
        filterCategoryField.setEnabled(ExchangeConnectorPlugin.ENABLED_BY_ADMIN && enableSynchronisationBox.isSelected() && downloadFromExchangeBox.isSelected());
        eventTypesList.setEnabled(ExchangeConnectorPlugin.ENABLED_BY_ADMIN && enableSynchronisationBox.isSelected());
        selectEventTypesInListFromCSV(eventTypeKeys);
    }

    /**
     *
     */
    private void setValuesToJComponents() {
        usernameTextField.setText(exchangeUsername);
        passwordTextField.setText(exchangePassword);
        enableSynchronisationBox.setSelected(ExchangeConnectorPlugin.ENABLED_BY_ADMIN && enableSynchronisation);
        downloadFromExchangeBox.setSelected(ExchangeConnectorPlugin.ENABLED_BY_ADMIN && downloadFromExchange);
        filterCategoryField.setText(filterCategory);

        final DefaultListModel model = new DefaultListModel();
        try {
            for (DynamicType event : getClientFacade().getDynamicTypes(DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION)) {
                // event type of "import from exchange" will be ignored!
                if (!ExchangeConnectorPlugin.IMPORT_EVENT_TYPE.equalsIgnoreCase(event.getElementKey()))
                    model.addElement(new StringWrapper<DynamicType>(event));
            }
        } catch (RaplaException e) {
        }
        eventTypesList.setModel(new SortedListModel(model));
        eventTypesList.setModel(model);

        updateComponentState();
    }

    /**
     * @see org.rapla.gui.DefaultPluginOption#getDescriptorClassName()
     */
    public String getDescriptorClassName() {
        return ExchangeConnectorPlugin.class.getName();
    }

    private class UpdateComponentsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updateComponentState();
        }
    }
}






