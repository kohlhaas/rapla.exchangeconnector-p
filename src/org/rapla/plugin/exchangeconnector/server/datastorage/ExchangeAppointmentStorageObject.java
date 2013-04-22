package org.rapla.plugin.exchangeconnector.server.datastorage;

import java.io.Serializable;

import org.rapla.entities.domain.Appointment;

/**
 * This class represents the mapping between an <b>exchange item</b> and an <b>exchange id</b>.
 * It contains
 * - the unique id of the RAPLA {@link Appointment}
 * - the respective unique id of the Exchange {@link microsoft.exchange.webservices.data.Appointment}
 * - the flag, if the appointment is an Exchange Item, which means that the appointment is private (created in Outlook, not in RAPLA)
 * The last flag is needed to avoid uploading appointments back to the Exchange Server when they originate from the Exchange Server.
 * 
 * @author Dominik Joder
 * @see ExchangeAppointmentStorage
 * @see ExchangeAccountInformationStorage
 * @see Appointment
 * @see microsoft.exchange.webservices.data.Appointment
 */
public class ExchangeAppointmentStorageObject implements Serializable {
	private static final long serialVersionUID = -8885900937714586442L;
	private final int appointmentId;
	private final String exchangeId;
	private boolean isExchangeItem;
	private final String raplaUsername;
	private boolean deleted;
	
	/**
	 * Parameterless constructor to generate an ExchangeAppointmentStorageObject object without any values 
	 *//*
	public ExchangeAppointmentStorageObject() {
		this("");
	}
	*//**
	 * Constructor to generate an appointment with a particular id of the Exchange {@link microsoft.exchange.webservices.data.Appointment}
	 * @param exchangeId : {@link String}
	 *//*
	public ExchangeAppointmentStorageObject(String exchangeId) {
		this(0, exchangeId, "", false);
	}*/

	/**
	 * Constructor to generate an appointment with a particular unique id of the {@link Appointment}, the id of the Exchange {@link microsoft.exchange.webservices.data.Appointment} and if the appointment has been created in Outlook
	 * @param appointmentId : {@link Integer} the unique id of the Rapla {@link Appointment}
	 * @param exchangeId : {@link String} the unique id of the Exchange {@link microsoft.exchange.webservices.data.Appointment} (the exchangeId is empty until the upload to the Exchange Server succeeds)
	 * @param isExternalAppointment : {@link Boolean} true if the appointment has been created in Outlook/Exchange, false if it has been created in Rapla
	 */
/*	public ExchangeAppointmentStorageObject(int appointmentId, String exchangeId, String raplaUsername, boolean isExternalAppointment) {
		this(appointmentId, exchangeId, raplaUsername, isExternalAppointment, false);
	}*/
	
	/**
	 * Constructor to generate an appointment with a particular unique id of the {@link Appointment}, the id of the Exchange {@link microsoft.exchange.webservices.data.Appointment} and if the appointment has been created in Outlook
	 * @param appointmentId : {@link Integer} the unique id of the Rapla {@link Appointment}
	 * @param exchangeId : {@link String} the unique id of the Exchange {@link microsoft.exchange.webservices.data.Appointment} (the exchangeId is empty until the upload to the Exchange Server succeeds)
	 * @param isExchangeItem : {@link Boolean} true if the appointment has been created in Outlook/Exchange, false if it has been created in Rapla
	 * @param deleted : {@link Boolean} true if this appointment is flagged as deleted from the Rapla System
	 */
	public ExchangeAppointmentStorageObject(int appointmentId, String exchangeId, String raplaUsername, boolean isExchangeItem, boolean deleted) {
		super();
		this.appointmentId = appointmentId;
		this.exchangeId = exchangeId;
		this.raplaUsername = raplaUsername;
		this.isExchangeItem = isExchangeItem;
		this.deleted = deleted;
	}
	
	/**
	 * @return appointmentId : {@link Integer} the unique id of the Rapla {@link Appointment}
	 */
	public int getAppointmentId() {
		return appointmentId;
	}
	
	/**
	 * @param appointmentId : {@link Integer} the unique id of the Rapla {@link Appointment}
	 *//*
	public void setAppointmentId(int appointmentId) {
		this.appointmentId = appointmentId;
	}*/
	/**
	 * @return exchangeId : {@link String} the unique id of the Exchange {@link microsoft.exchange.webservices.data.Appointment} (the exchangeId is empty until the upload to the Exchange Server succeeds)
	 */
	public String getExchangeId() {
		return exchangeId;
	}
	/**
	 * @param exchangeId : {@link String} the unique id of the Exchange {@link microsoft.exchange.webservices.data.Appointment}
	public void setExchangeId(String exchangeId) {
		this.exchangeId = exchangeId;
	}
	/**
	 * @return isExternalAppointment : {@link Boolean} true if the appointment has been created in Outlook/Exchange, false if it has been created in Rapla
	 */
	public boolean isExchangeItem() {
		return isExchangeItem;
	}
	/**
	 * @param isExchangeItem : {@link Boolean} true if the appointment has been created in Outlook/Exchange, false if it has been created in Rapla
	 */
	public void setExchangeItem(boolean isExchangeItem) {
		this.isExchangeItem = isExchangeItem;
	}
	/**
	 * @return getRaplaUsername : {@link String} the Rapla name of the user that owns this appointment on the Exchange Server
	 */
	public String getRaplaUsername() {
		return raplaUsername;
	}
	/**
	 * @param raplaUsername : {@link String} the Rapla name of the user that owns this appointment on the Exchange Server
	 */
	/*public void setRaplaUsername(String raplaUsername) {
		this.raplaUsername = raplaUsername;
	}*/
	
	/**
	 * @return deleted : {@link Boolean} true if the appointment has been deleted from the Rapla system
	 */
	public boolean isDeleted() {
		return deleted;
	}
	
	/**
	 * @param deleted {@link Boolean} set or unset the deleted flag
	 */
	private void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	/**
	 * flag this appointment as deleted
	 */
	public void setDeleted() {
		setDeleted(true);
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExchangeAppointmentStorageObject that = (ExchangeAppointmentStorageObject) o;

        if (appointmentId != that.appointmentId) return false;
        if (exchangeId != null ? !exchangeId.equals(that.exchangeId) : that.exchangeId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = appointmentId;
        result = 31 * result + (exchangeId != null ? exchangeId.hashCode() : 0);
        return result;
    }
}
