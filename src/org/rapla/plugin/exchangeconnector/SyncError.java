package org.rapla.plugin.exchangeconnector;

public class SyncError    {
    public SyncError(String appointmentDetail, String errorMessage) {
        this.appointmentDetail = appointmentDetail;
        this.errorMessage = errorMessage;
    }
    String appointmentDetail;
    String errorMessage;
    public String getAppointmentDetail() {
        return appointmentDetail;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
}