package org.rapla.plugin.exchangeconnector.server;

import java.net.URI;

import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ExchangeVersion;
import microsoft.exchange.webservices.data.NameResolutionCollection;
import microsoft.exchange.webservices.data.ResolveNameSearchLocation;
import microsoft.exchange.webservices.data.WebCredentials;

import org.rapla.components.util.DateTools;

/**
 * This class is obliged with the task to provide a connection to a specific Exchange Server-instance
 * by means of generating an {@link ExchangeService} object
 *
 * @author lutz
 */
public class EWSConnector {

    private static final int SERVICE_DEFAULT_TIMEOUT = 10000;
    private String fqdn;
    private WebCredentials credentials;
    private ExchangeService service;

//	private final Character DOMAIN_SEPERATION_SYMBOL = new Character('@');

    /**
     * The constructor
     *
     * @param fqdn        : {@link String}
     * @param credentials : {@link WebCredentials}
     * @throws Exception
     */
    public EWSConnector(String fqdn, WebCredentials credentials) throws Exception {
        super();
        setFqdn(fqdn);
        setCredentials(credentials);
        setService();
    }

    /**
     * @return {@link WebCredentials} the credentials
     */
    private WebCredentials getCredentials() {
        return credentials;
    }

    /**
     * @param credentials : {@link WebCredentials} the credentials to set
     */
    private void setCredentials(WebCredentials credentials) {
        this.credentials = credentials;
    }

    /**
     * @return {@link ExchangeService} the service
     */
    public ExchangeService getService() {
        return service;
    }

    /**
     * @return {@link String} the fqdn
     */
    public String getFqdn() {
        return fqdn;
    }

    /**
     * @param fqdn : {@link String} the fqdn to set
     */
    private void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    /**
     * @return {@link String} the userName
     */
    private String getUserName() {
        return getCredentials().getUser();
    }

    /**
     * @param service : {@link ExchangeService} the service to set
     */
    private void setService(ExchangeService service) {
        this.service = service;
    }

    /**
     * Creates the {@link ExchangeService} object
     *
     * @throws Exception
     */
    private void setService() throws Exception {
        ExchangeService tmpService = new ExchangeService(ExchangeVersion.Exchange2010_SP1, DateTools.getTimeZone());

        tmpService.setCredentials(getCredentials());
        tmpService.setTimeout(SERVICE_DEFAULT_TIMEOUT);

        //define connection url to mail server, assume https

        final URI uri = new URI(getFqdn() + "/ews/Exchange.asmx");
        tmpService.setUrl(uri);

        // removed because auto is discovery not yet support
        // tmpService.autodiscoverUrl(getUserName()+this.DOMAIN_SEPERATION_SYMBOL+getFqdn());//autodiscover the URL with the parameter "username@fqdn
        setService(tmpService);

        /*   // test if connection works
        getService().resolveName(getUserName(), ResolveNameSearchLocation.DirectoryOnly, true);
        FindItemsResults<Item> items = tmpService.findItems(WellKnownFolderName.Calendar, new SearchFilter.IsEqualTo(EmailMessageSchema.IsRead, Boolean.TRUE), new ItemView(100));
        for (Item item : items) {
            System.out.println(item.getSubject());
        }*/
        SynchronisationManager.logInfo("Connected to Exchange at + " + uri + " at timezone: " + DateTools.getTimeZone());



    }

    /**
     * Retrieves the SMTP address associated with the current service-connection
     *
     * @return : {@link String}
     */
    public String getSMPTAddress() {
        String returnVal = new String();

        try {
            NameResolutionCollection nameResolutionCollection = getService().resolveName(getUserName(), ResolveNameSearchLocation.DirectoryOnly, true);
            if (nameResolutionCollection.getCount() == 1) {
                returnVal = nameResolutionCollection.nameResolutionCollection(0).getMailbox().getAddress();
            }
        } catch (Exception e) {
            e.printStackTrace();
            SynchronisationManager.logException(e);
        }
        return returnVal;
    }
}
