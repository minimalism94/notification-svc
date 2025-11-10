package app.service;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.lookups.v1.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsNotificationService {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public SmsNotificationService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.from-number}") String fromNumber
    ) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;

        if (this.accountSid != null && !this.accountSid.isBlank()) {
            Twilio.init(this.accountSid, this.authToken);
        } else {
            System.out.println("[SMS] Twilio credentials not configured; SMS will be logged only.");
        }
    }

    public void sendSms(String to, String messageBody) {
        if (accountSid == null || accountSid.isBlank()) {
            System.out.println("[SMS] (LOG ONLY) To: " + to + " Message: " + messageBody);
            return;
        }

        try {
            Message message = Message.creator(
                    new com.twilio.type.PhoneNumber(to),
                    new com.twilio.type.PhoneNumber(fromNumber),
                    messageBody
            ).create();
            System.out.println("[SMS] Sent via Twilio SID=" + message.getSid());
        } catch (ApiException ex) {
            System.err.println("[SMS] Twilio API error: " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("[SMS] Error sending SMSS: " + ex.getMessage());
        }
    }
}
