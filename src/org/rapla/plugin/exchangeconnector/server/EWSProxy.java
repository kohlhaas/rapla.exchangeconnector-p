/**
 *
 */
package org.rapla.plugin.exchangeconnector.server;

import java.text.SimpleDateFormat;

import microsoft.exchange.webservices.data.DefaultExtendedPropertySet;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ExtendedPropertyDefinition;
import microsoft.exchange.webservices.data.MapiPropertyType;
import microsoft.exchange.webservices.data.WebCredentials;

import org.rapla.entities.User;
import org.rapla.entities.domain.Appointment;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorPlugin;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAccountInformationStorage;
import org.rapla.plugin.exchangeconnector.server.datastorage.ExchangeAppointmentStorage;

/**
 * This class provides all functionality in terms of Exchange Server communication
 *
 * @author lutz
 */

class EWSProxy extends RaplaComponent {

    protected static final String RAPLA_NOSYNC_KEYWORD = "<==8NO_SYNC8==>";
    protected static final String RAPLA_BODY_MESSAGE = "Please do not change this item, to prevent inconsistencies!\n\n\n";
    public static SimpleDateFormat EWS_DEFAULT_FORMATTER;
    public static ExtendedPropertyDefinition raplaAppointmentPropertyDefinition;
    private ExchangeService service;
    private ClientFacade facade;
    private User raplaUser;

    /**
     * The constructor
     *
     * @param facade    : {@link ClientFacade}
     * @param raplaUser : {@link User}
     * @throws Exception
     */
    public EWSProxy(RaplaContext context, ClientFacade facade, User raplaUser) throws Exception {
        super(context);
        setFacade(facade);
        setRaplaUser(raplaUser);
        setService(raplaUser);
        initProxy();
    }


    /**
     * The constructor
     *
     * @param clientFacade  : {@link ClientFacade}
     * @param raplaUsername : {@link String}
     * @throws Exception
     */
    public EWSProxy(RaplaContext context, ClientFacade clientFacade, String raplaUsername) throws Exception {
        this(context, clientFacade, clientFacade.getUser(raplaUsername));
    }

    /**
     * @param clientFacade
     * @param appointment
     * @throws Exception
     */
    public EWSProxy(RaplaContext context, ClientFacade clientFacade, Appointment appointment) throws Exception {
        super(context);
        // see if rapla user is stored with appointment in exchange db
        // inviting user is the one who is going to authenticate against exchange
        // so might be the current user, but also a user who just moves it
        // so we are just looking for credentials of any person resource in this appointment
        String invitingUserName = ExchangeAppointmentStorage.getInstance().getRaplaUsername(appointment);
        if (invitingUserName == null || invitingUserName.isEmpty()) {
            for (User candidateUser : ExchangeConnectorUtils.getAppointmentUsers(appointment, clientFacade)) {
                if (ExchangeAccountInformationStorage.getInstance().hasUser(candidateUser)) {
                    invitingUserName = candidateUser.getUsername();
                    break;
                }
            }
        }
        if (!invitingUserName.isEmpty()) {
            setFacade(clientFacade);
            setService(invitingUserName);
            setRaplaUser(clientFacade.getUser(invitingUserName));
            initProxy();
        }
    }

    /**
     *
     */
    private void initProxy() {
        EWS_DEFAULT_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            raplaAppointmentPropertyDefinition = new ExtendedPropertyDefinition(DefaultExtendedPropertySet.Appointment, "isRaplaMeeting", MapiPropertyType.Boolean);
        } catch (Exception e) {
            SynchronisationManager.logException(e);
        }
    }


    /**
     * @return {@link ExchangeService}
     */
    protected ExchangeService getService() {
        return this.service;
    }


    /**
     * @param service : {@link ExchangeService} the service to set
     */
    private void setService(ExchangeService service) {
        this.service = service;

    }

    /**
     * @param raplaUser : {@link User}
     * @throws Exception
     */
    private void setService(User raplaUser) throws Exception {
        setService(raplaUser.getUsername());
    }


    /**
     * Generate and keep the {@link ExchangeService}-instance for the given {@link User}
     *
     * @param raplaUsername : {@link String}
     * @throws Exception
     */
    private void setService(String raplaUsername) throws Exception {
        WebCredentials credentials = ExchangeAccountInformationStorage.getInstance().getWebCredentialsForRaplaUser(raplaUsername);
        setService(credentials);
    }


    /**
     * Generate and keep the {@link ExchangeService}-instance for the given {@link WebCredentials}
     *
     * @param credentials : {@link WebCredentials}
     * @throws Exception
     */
    private void setService(WebCredentials credentials) throws Exception {
        setService(new EWSConnector(ExchangeConnectorPlugin.EXCHANGE_WS_FQDN, credentials).getService());

    }

    /**
     * @return {@link ClientFacade} the facade
     */
    protected ClientFacade getFacade() {
        return facade;
    }

    /**
     * @param facade : {@link ClientFacade} the facade to set
     */
    private void setFacade(ClientFacade facade) {
        this.facade = facade;
    }

    /**
     * @param raplaUser : {@link User} the raplaUser to set
     */
    private void setRaplaUser(User raplaUser) {
        this.raplaUser = raplaUser;
    }

    /**
     * Returns the rapla user for whom the service has been established
     *
     * @return {@link User} the raplaUser
     */
    public User getRaplaUser() {
        return raplaUser;
    }

}