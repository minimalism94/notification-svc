package app.service;


public interface SmsProvider {
    

    boolean sendSms(String to, String messageBody);

    boolean isConfigured();
}

