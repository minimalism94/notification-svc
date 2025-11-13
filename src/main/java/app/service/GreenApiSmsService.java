package app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
@Service
@Slf4j
public class GreenApiSmsService implements SmsProvider {

    private final String instanceId;
    private final String apiToken;
    private final String apiUrl;
    private final RestTemplate restTemplate;
    private final boolean configured;

    public GreenApiSmsService(
            @Value("${green-api.instance-id:}") String instanceId,
            @Value("${green-api.api-token:}") String apiToken,
            @Value("${green-api.api-url:https://api.green-api.com}") String apiUrl
    ) {
        this.instanceId = instanceId;
        this.apiToken = apiToken;
        this.apiUrl = apiUrl;
        this.restTemplate = new RestTemplate();

        this.configured = (instanceId != null && !instanceId.isBlank() && 
                           apiToken != null && !apiToken.isBlank());
        
        if (this.configured) {
            log.info("[GREEN-API] Service initialized with instanceId: {}", instanceId);
        } else {
            log.warn("[GREEN-API] Service not configured - credentials missing. SMS will be logged only.");
        }
    }

    @Override
    public boolean sendSms(String to, String messageBody) {
        if (!configured) {
            log.info("[GREEN-API] (LOG ONLY) To: {} Message: {}", to, messageBody);
            return true;
        }

        try {

            String formattedNumber = formatPhoneNumber(to);
            
            log.info("[GREEN-API] Sending WhatsApp message to: {} (formatted: {})", to, formattedNumber);
            log.info("[GREEN-API] Message body length: {} characters", messageBody.length());
            

            String url = String.format("%s/waInstance%s/sendMessage/%s", 
                    apiUrl, instanceId, apiToken);
            

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chatId", formattedNumber + "@c.us"); // WhatsApp формат: номер@c.us
            requestBody.put("message", messageBody);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                

                if (responseBody.containsKey("idMessage")) {
                    String messageId = (String) responseBody.get("idMessage");
                    log.info("[GREEN-API] ✓ Message sent successfully - ID: {}", messageId);
                    log.info("[GREEN-API] Full response: {}", responseBody);
                    return true;
                } else if (responseBody.containsKey("error")) {
                    String error = responseBody.get("error").toString();
                    log.error("[GREEN-API] ✗ Error in response: {}", error);
                    log.error("[GREEN-API] Full error response: {}", responseBody);
                    return false;
                } else {
                    log.warn("[GREEN-API] Unexpected response format: {}", responseBody);
                    return false;
                }
            } else {
                log.error("[GREEN-API] ✗ HTTP error - Status: {}", response.getStatusCode());
                return false;
            }
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("[GREEN-API] ✗ HTTP Client Error: {} - Status: {} - Body: {}", 
                    e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("[GREEN-API] ✗ HTTP Server Error: {} - Status: {} - Body: {}", 
                    e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("[GREEN-API] ✗ Error sending WhatsApp message: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }


    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        

        String cleaned = phoneNumber.trim().replaceAll("[\\s\\-\\(\\)\\.]", "");
        
                if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        

        if (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2);
        }
        

        if (cleaned.startsWith("0")) {
            cleaned = "359" + cleaned.substring(1);
        }
        

        if (cleaned.startsWith("359")) {
            return cleaned;
        }
        

        if (cleaned.matches("^[89]\\d{8}$")) {
            return "359" + cleaned;
        }
        if (cleaned.matches("^\\d{7,15}$")) {
            return cleaned;
        }
        
        throw new IllegalArgumentException(
            String.format("Invalid phone number format: '%s'. Expected format: +359893454943 or 359893454943", 
                phoneNumber));
    }
}

