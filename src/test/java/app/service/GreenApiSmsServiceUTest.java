package app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GreenApiSmsServiceUTest {

    @Mock
    private RestTemplate restTemplate;

    private GreenApiSmsService smsService;
    private String instanceId;
    private String apiToken;
    private String apiUrl;

    @BeforeEach
    void setUp() {
        instanceId = "testInstanceId";
        apiToken = "testApiToken";
        apiUrl = "https://api.green-api.com";
    }

    @Test
    void shouldSendSmsSuccessfullyWhenApiReturnsSuccessResponse() {
        givenSmsServiceIsConfigured();
        givenRestTemplateReturnsSuccessResponse();

        boolean result = whenSendSmsIsCalled("359893454943", "Test message");

        thenSmsShouldBeSentSuccessfully(result);
        thenRestTemplateShouldBeCalled();
    }

    @Test
    void shouldReturnFalseWhenApiReturnsErrorInResponse() {
        givenSmsServiceIsConfigured();
        givenRestTemplateReturnsErrorResponse();

        boolean result = whenSendSmsIsCalled("359893454943", "Test message");

        thenSmsShouldFail(result);
    }

    @Test
    void shouldReturnFalseWhenHttpClientErrorOccurs() {
        givenSmsServiceIsConfigured();
        givenRestTemplateThrowsHttpClientError();

        boolean result = whenSendSmsIsCalled("359893454943", "Test message");

        thenSmsShouldFail(result);
    }

    @Test
    void shouldReturnFalseWhenHttpServerErrorOccurs() {
        givenSmsServiceIsConfigured();
        givenRestTemplateThrowsHttpServerError();

        boolean result = whenSendSmsIsCalled("359893454943", "Test message");

        thenSmsShouldFail(result);
    }

    @Test
    void shouldReturnFalseWhenGenericExceptionOccurs() {
        givenSmsServiceIsConfigured();
        givenRestTemplateThrowsGenericException();

        boolean result = whenSendSmsIsCalled("359893454943", "Test message");

        thenSmsShouldFail(result);
    }

    @Test
    void shouldReturnTrueAndLogWhenServiceIsNotConfigured() {
        givenSmsServiceIsNotConfigured();

        boolean result = whenSendSmsIsCalled("359893454943", "Test message");

        thenSmsShouldBeLoggedOnly(result);
        thenRestTemplateShouldNotBeCalled();
    }

    @Test
    void shouldFormatPhoneNumberWithPlusPrefixCorrectly() {
        givenSmsServiceIsConfigured();
        givenRestTemplateReturnsSuccessResponse();

        whenSendSmsIsCalled("+359893454943", "Test message");

        thenPhoneNumberShouldBeFormattedCorrectly("359893454943");
    }

    @Test
    void shouldFormatPhoneNumberWith00PrefixCorrectly() {
        givenSmsServiceIsConfigured();
        givenRestTemplateReturnsSuccessResponse();

        whenSendSmsIsCalled("00359893454943", "Test message");

        thenPhoneNumberShouldBeFormattedCorrectly("359893454943");
    }

    @Test
    void shouldFormatPhoneNumberStartingWith0Correctly() {
        givenSmsServiceIsConfigured();
        givenRestTemplateReturnsSuccessResponse();

        whenSendSmsIsCalled("0893454943", "Test message");

        thenPhoneNumberShouldBeFormattedCorrectly("359893454943");
    }

    @Test
    void shouldFormatPhoneNumberStartingWith8Or9Correctly() {
        givenSmsServiceIsConfigured();
        givenRestTemplateReturnsSuccessResponse();

        whenSendSmsIsCalled("893454943", "Test message");

        thenPhoneNumberShouldBeFormattedCorrectly("359893454943");
    }

    @Test
    void shouldReturnTrueWhenIsConfiguredIsCalledAndServiceIsConfigured() {
        givenSmsServiceIsConfigured();

        boolean result = whenIsConfiguredIsCalled();

        thenServiceShouldBeConfigured(result);
    }

    @Test
    void shouldReturnFalseWhenIsConfiguredIsCalledAndServiceIsNotConfigured() {
        givenSmsServiceIsNotConfigured();

        boolean result = whenIsConfiguredIsCalled();

        thenServiceShouldNotBeConfigured(result);
    }

    private void givenSmsServiceIsConfigured() {
        smsService = new GreenApiSmsService(instanceId, apiToken, apiUrl);
        ReflectionTestUtils.setField(smsService, "restTemplate", restTemplate);
    }

    private void givenSmsServiceIsNotConfigured() {
        smsService = new GreenApiSmsService("", "", apiUrl);
        ReflectionTestUtils.setField(smsService, "restTemplate", restTemplate);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void givenRestTemplateReturnsSuccessResponse() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("idMessage", "12345");
        ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class))).thenReturn(response);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void givenRestTemplateReturnsErrorResponse() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", "Invalid credentials");
        ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class))).thenReturn(response);
    }

    @SuppressWarnings("unchecked")
    private void givenRestTemplateThrowsHttpClientError() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));
    }

    @SuppressWarnings("unchecked")
    private void givenRestTemplateThrowsHttpServerError() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));
    }

    @SuppressWarnings("unchecked")
    private void givenRestTemplateThrowsGenericException() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenThrow(new RestClientException("Connection failed"));
    }

    private boolean whenSendSmsIsCalled(String phoneNumber, String message) {
        return smsService.sendSms(phoneNumber, message);
    }

    private boolean whenIsConfiguredIsCalled() {
        return smsService.isConfigured();
    }

    private void thenSmsShouldBeSentSuccessfully(boolean result) {
        assertThat(result).isTrue();
    }

    private void thenSmsShouldFail(boolean result) {
        assertThat(result).isFalse();
    }

    private void thenSmsShouldBeLoggedOnly(boolean result) {
        assertThat(result).isTrue();
    }

    @SuppressWarnings("unchecked")
    private void thenRestTemplateShouldBeCalled() {
        verify(restTemplate, times(1)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Map.class));
    }

    @SuppressWarnings("unchecked")
    private void thenRestTemplateShouldNotBeCalled() {
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Map.class));
    }

    @SuppressWarnings("unchecked")
    private void thenPhoneNumberShouldBeFormattedCorrectly(String expectedFormattedNumber) {
        ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), any(HttpMethod.class), entityCaptor.capture(), eq(Map.class));
        
        HttpEntity<?> capturedEntity = entityCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> requestBody = (Map<String, Object>) capturedEntity.getBody();
        assertThat(requestBody).isNotNull();
        String chatId = (String) requestBody.get("chatId");
        
        assertThat(chatId).isEqualTo(expectedFormattedNumber + "@c.us");
    }

    private void thenServiceShouldBeConfigured(boolean result) {
        assertThat(result).isTrue();
    }

    private void thenServiceShouldNotBeConfigured(boolean result) {
        assertThat(result).isFalse();
    }
}

