package app.web;

import app.model.NotificationPreference;
import app.model.NotificationType;
import app.service.NotificationPreferenceService;
import app.web.dto.PreferenceRequest;
import app.web.dto.PreferenceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreferenceControllerTest {

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @InjectMocks
    private PreferenceController preferenceController;

    private UUID userId;
    private PreferenceRequest preferenceRequest;
    private NotificationPreference notificationPreference;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        preferenceRequest = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("test@example.com")
                .type(NotificationType.EMAIL)
                .build();

        notificationPreference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(true)
                .contactInfo("test@example.com")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldCreatePreferenceAndReturnCreatedStatus() {
        givenPreferenceServiceUpserts(notificationPreference);

        ResponseEntity<PreferenceResponse> result = whenCreatePreferenceIsCalled(preferenceRequest);

        thenResponseShouldHaveCreatedStatus(result);
        thenResponseBodyShouldContainPreferenceData(result.getBody());
        thenPreferenceServiceShouldBeCalled();
    }

    @Test
    void shouldReturnPreferenceForUser() {
        givenPreferenceServiceReturns(notificationPreference);

        ResponseEntity<PreferenceResponse> result = whenGetPreferenceIsCalled(userId);

        thenResponseShouldHaveOkStatus(result);
        thenResponseBodyShouldContainPreferenceData(result.getBody());
        thenPreferenceServiceShouldBeCalledForGet();
    }

    @Test
    void shouldUpdateExistingPreferenceAndReturnCreatedStatus() {
        NotificationPreference updatedPreference = NotificationPreference.builder()
                .id(notificationPreference.getId())
                .userId(userId)
                .type(NotificationType.SMS)
                .enabled(false)
                .contactInfo("359893454943")
                .createdOn(notificationPreference.getCreatedOn())
                .updatedOn(LocalDateTime.now())
                .build();
        givenPreferenceServiceUpserts(updatedPreference);

        PreferenceRequest updateRequest = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(false)
                .contactInfo("359893454943")
                .type(NotificationType.SMS)
                .build();

        ResponseEntity<PreferenceResponse> result = whenCreatePreferenceIsCalled(updateRequest);

        thenResponseShouldHaveCreatedStatus(result);
        thenResponseBodyShouldContainUpdatedPreferenceData(result.getBody(), updatedPreference);
    }

    private void givenPreferenceServiceUpserts(NotificationPreference preference) {
        when(notificationPreferenceService.upsert(any(PreferenceRequest.class))).thenReturn(preference);
    }

    private void givenPreferenceServiceReturns(NotificationPreference preference) {
        when(notificationPreferenceService.getByUserId(any(UUID.class))).thenReturn(preference);
    }

    private ResponseEntity<PreferenceResponse> whenCreatePreferenceIsCalled(PreferenceRequest request) {
        return preferenceController.createPreference(request);
    }

    private ResponseEntity<PreferenceResponse> whenGetPreferenceIsCalled(UUID userId) {
        return preferenceController.getPreference(userId);
    }

    private void thenResponseShouldHaveCreatedStatus(ResponseEntity<PreferenceResponse> result) {
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void thenResponseShouldHaveOkStatus(ResponseEntity<PreferenceResponse> result) {
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void thenResponseBodyShouldContainPreferenceData(PreferenceResponse response) {
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(response.getContactInfo()).isEqualTo("test@example.com");
        assertThat(response.isNotificationEnabled()).isTrue();
    }

    private void thenResponseBodyShouldContainUpdatedPreferenceData(PreferenceResponse response, NotificationPreference expected) {
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(expected.getType());
        assertThat(response.getContactInfo()).isEqualTo(expected.getContactInfo());
        assertThat(response.isNotificationEnabled()).isEqualTo(expected.isEnabled());
    }

    private void thenPreferenceServiceShouldBeCalled() {
        verify(notificationPreferenceService, times(1)).upsert(any(PreferenceRequest.class));
    }

    private void thenPreferenceServiceShouldBeCalledForGet() {
        verify(notificationPreferenceService, times(1)).getByUserId(any(UUID.class));
    }
}

