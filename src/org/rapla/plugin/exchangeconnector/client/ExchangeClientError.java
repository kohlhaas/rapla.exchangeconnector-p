package org.rapla.plugin.exchangeconnector.client;

import java.util.Date;

import javax.inject.Inject;

import org.rapla.client.ClientExtension;
import org.rapla.components.util.Command;
import org.rapla.components.util.CommandScheduler;
import org.rapla.components.util.ParseDateException;
import org.rapla.entities.configuration.Preferences;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.TypedComponentRole;
import org.rapla.plugin.exchangeconnector.ExchangeConnectorRemote;
import org.rapla.plugin.exchangeconnector.SynchronizationStatus;

public class ExchangeClientError extends RaplaComponent implements ClientExtension, ModificationListener 
{
    CommandScheduler scheduler;
    Date changedDate;
    ExchangeConnectorRemote remote;
    
    TypedComponentRole<String> LAST_SYNC_ERROR_SHOWN = new TypedComponentRole<String>("org.rapla.plugin.exchangconnector.last_sync_error_shown");
    
    
    @Inject
    public ExchangeClientError(RaplaContext context, final ExchangeConnectorRemote remote) throws RaplaException {
        super(context);
        this.remote = remote;
        CommandScheduler scheduler = context.lookup(CommandScheduler.class);
        getClientFacade().addModificationListener( this );
        // wait a bit to so we don't interfere with startup time
        if ( changed())
        {
      
            int initialDelay = 3000;
            scheduler.schedule(new Command() {
                
                @Override
                public void execute() throws Exception {
                    showNotificationDialogOnError();
                }
            }, initialDelay);
        }
    }
    
    @Override
    public void dataChanged(ModificationEvent evt) throws RaplaException {
        if (changed())
        {
            showNotificationDialogOnError();
        }
    }
    
    private void showNotificationDialogOnError() throws RaplaException {
        SynchronizationStatus synchronizationStatus = remote.getSynchronizationStatus();
        if ( synchronizationStatus.synchronizationErrors.size() > 0)
        {
            SyncResultDialog dlg = new SyncResultDialog( getContext());
            dlg.showResultDialog( synchronizationStatus);
        }
    }
    
    Date lastShown;

    public boolean changed() throws RaplaException
    {
        Preferences preferences = getQuery().getPreferences();
        Date lastChanged = parseDate(preferences, ExchangeConnectorRemote.LAST_SYNC_ERROR_CHANGE);
        Date lastShown = this.lastShown;//parseDate(preferences, LAST_SYNC_ERROR_SHOWN);
        if ( lastChanged != null && ( lastShown == null  || lastChanged.after(lastShown))) 
        {
            this.lastShown = getClientFacade().getOperator().getCurrentTimestamp();
            return true;
        }
        return false;
    }

    private Date parseDate(Preferences preferences, TypedComponentRole<String> entryName) throws RaplaException {
        Date date = null;
        String entry = preferences.getEntryAsString( entryName, null);
        if (entry != null)
        {
            try {
                date = getRaplaLocale().getSerializableFormat().parseTimestamp( entry);
            } catch (ParseDateException e) {
                throw new RaplaException( e.getMessage() , e);
            }
        }
        return date;
    }   
    
}
