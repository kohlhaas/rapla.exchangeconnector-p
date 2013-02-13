package org.rapla.plugin.exchangeconnector.jms;

import org.rapla.facade.AllocationChangeEvent;

import java.io.Serializable;

/**
 * User: kuestermann
 * Date: 10.06.12
 * Time: 16:42
 */
public class MessageContainer implements Serializable {
    private Serializable object;
    private String type;
    private String exchangeAppointmentID;

    public MessageContainer(Serializable object, String type, String exchangeAppointmentID) {
        this.object = object;
        this.type = type;
        this.exchangeAppointmentID = exchangeAppointmentID;
    }


    public String getExchangeAppointmentID() {
        return exchangeAppointmentID;
    }

    public void setExchangeAppointmentID(String exchangeAppointmentID) {
        this.exchangeAppointmentID = exchangeAppointmentID;
    }

    public Serializable getObject() {
        return object;
    }

    public void setObject(Serializable object) {
        this.object = object;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AllocationChangeEvent.Type getTypeForString() {
        return type.equals(AllocationChangeEvent.ADD.toString()) ? AllocationChangeEvent.ADD :
                type.equals(AllocationChangeEvent.CHANGE.toString()) ? AllocationChangeEvent.CHANGE : AllocationChangeEvent.REMOVE;
    }
}
