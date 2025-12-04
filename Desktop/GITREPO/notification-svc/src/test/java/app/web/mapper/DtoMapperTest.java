package app.web.mapper;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.web.dto.NotificationResponse;
import app.web.dto.PreferenceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DtoMapperTest {

    private Notification notification;
    private NotificationPreference notificationPreference;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        notification = Notification.builder()
                .id(UUID.randomUUID())
                .subject("Test Subject")
                .body("Test Body")
                .createdOn(LocalDateTime.now())
                .type(NotificationType.EMAIL)
                .userId(userId)
                .status(NotificationStatus.SUCCEEDED)
                .deleted(false)
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
    void shouldMapNotificationToNotificationResponseCorrectly() {
        NotificationResponse result = whenMapNotificationToResponse(notification);

        thenNotificationResponseShouldContainCorrectData(result, notification);
    }

    @Test
    void shouldMapNotificationPreferenceToPreferenceResponseCorrectly() {
        PreferenceResponse result = whenMapPreferenceToResponse(notificationPreference);

        thenPreferenceResponseShouldContainCorrectData(result, notificationPreference);
    }

    @Test
    void shouldMapSmsNotificationTypeCorrectly() {
        Notification smsNotification = Notification.builder()
                .id(UUID.randomUUID())
                .subject("SMS Subject")
                .body("SMS Body")
                .createdOn(LocalDateTime.now())
                .type(NotificationType.SMS)
                .userId(userId)
                .status(NotificationStatus.SUCCEEDED)
                .deleted(false)
                .build();

        NotificationResponse result = whenMapNotificationToResponse(smsNotification);

        thenNotificationTypeShouldBe(result, NotificationType.SMS);
    }

    @Test
    void shouldMapFailedNotificationStatusCorrectly() {
        Notification failedNotification = Notification.builder()
                .id(UUID.randomUUID())
                .subject("Failed Subject")
                .body("Failed Body")
                .createdOn(LocalDateTime.now())
                .type(NotificationType.EMAIL)
                .userId(userId)
                .status(NotificationStatus.FAILED)
                .deleted(false)
                .build();

        NotificationResponse result = whenMapNotificationToResponse(failedNotification);

        thenNotificationStatusShouldBe(result, NotificationStatus.FAILED);
    }

    @Test
    void shouldMapDisabledPreferenceCorrectly() {
        NotificationPreference disabledPreference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.SMS)
                .enabled(false)
                .contactInfo("359893454943")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        PreferenceResponse result = whenMapPreferenceToResponse(disabledPreference);

        thenPreferenceShouldBeDisabled(result);
        thenPreferenceTypeShouldBe(result, NotificationType.SMS);
    }

    private NotificationResponse whenMapNotificationToResponse(Notification notification) {
        return DtoMapper.from(notification);
    }

    private PreferenceResponse whenMapPreferenceToResponse(NotificationPreference preference) {
        return DtoMapper.from(preference);
    }

    private void thenNotificationResponseShouldContainCorrectData(NotificationResponse result, Notification expected) {
        assertThat(result).isNotNull();
        assertThat(result.getSubject()).isEqualTo(expected.getSubject());
        assertThat(result.getCreatedOn()).isEqualTo(expected.getCreatedOn());
        assertThat(result.getType()).isEqualTo(expected.getType());
        assertThat(result.getStatus()).isEqualTo(expected.getStatus());
    }

    private void thenPreferenceResponseShouldContainCorrectData(PreferenceResponse result, NotificationPreference expected) {
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(expected.getType());
        assertThat(result.getContactInfo()).isEqualTo(expected.getContactInfo());
        assertThat(result.isNotificationEnabled()).isEqualTo(expected.isEnabled());
    }

    private void thenNotificationTypeShouldBe(NotificationResponse result, NotificationType expectedType) {
        assertThat(result.getType()).isEqualTo(expectedType);
    }

    private void thenNotificationStatusShouldBe(NotificationResponse result, NotificationStatus expectedStatus) {
        assertThat(result.getStatus()).isEqualTo(expectedStatus);
    }

    private void thenPreferenceShouldBeDisabled(PreferenceResponse result) {
        assertThat(result.isNotificationEnabled()).isFalse();
    }

    private void thenPreferenceTypeShouldBe(PreferenceResponse result, NotificationType expectedType) {
        assertThat(result.getType()).isEqualTo(expectedType);
    }
}

