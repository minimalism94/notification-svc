package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.repository.NotificationRepository;
import app.repository.NotificationPreferenceRepository;
import app.web.dto.NotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @MockitoBean
    private MailSender mailSender;

    @MockitoBean
    private SmsProvider smsProvider;

    private UUID userId;
    private NotificationRequest notificationRequest;
    private NotificationPreference emailPreference;
    private NotificationPreference smsPreference;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();

        userId = UUID.randomUUID();

        notificationRequest = NotificationRequest.builder()
                .userId(userId)
                .subject("Integration Test Subject")
                .body("Integration Test Body")
                .build();

        emailPreference = NotificationPreference.builder()
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(true)
                .contactInfo("test@example.com")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        smsPreference = NotificationPreference.builder()
                .userId(userId)
                .type(NotificationType.SMS)
                .enabled(true)
                .contactInfo("359893454943")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldSaveSmsNotificationToDatabaseWhenSendingSuccessfully() {
        givenPreferenceExistsInDatabase(smsPreference);
        givenSmsProviderReturns(true);

        Notification result = whenSendNotificationIsCalled(notificationRequest);

        thenNotificationShouldBePersistedInDatabase(result);
        thenNotificationShouldHaveCorrectData(result);
        thenSmsProviderShouldBeCalled();
    }

    @Test
    void shouldSaveNotificationWithFailedStatusWhenEmailSendingFails() {
        givenPreferenceExistsInDatabase(emailPreference);
        givenMailSenderThrowsException();

        Notification result = whenSendNotificationIsCalled(notificationRequest);

        thenNotificationShouldBePersistedInDatabase(result);
        thenNotificationStatusShouldBe(result, NotificationStatus.FAILED);
    }

    @Test
    void shouldSaveNotificationWithFailedStatusWhenSmsSendingFails() {
        givenPreferenceExistsInDatabase(smsPreference);
        givenSmsProviderReturns(false);

        Notification result = whenSendNotificationIsCalled(notificationRequest);

        thenNotificationShouldBePersistedInDatabase(result);
        thenNotificationStatusShouldBe(result, NotificationStatus.FAILED);
    }

    @Test
    void shouldThrowExceptionWhenUserPreferenceDoesNotExist() {
        assertThatThrownBy(() -> whenSendNotificationIsCalled(notificationRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Preference for this user does not exist");

        thenNoNotificationsShouldBeSaved();
    }

    @Test
    void shouldThrowExceptionWhenUserNotificationsAreDisabled() {
        NotificationPreference disabledPreference = NotificationPreference.builder()
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(false)
                .contactInfo("test@example.com")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        givenPreferenceExistsInDatabase(disabledPreference);

        assertThatThrownBy(() -> whenSendNotificationIsCalled(notificationRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("turned off their notifications");

        thenNoNotificationsShouldBeSaved();
    }

    @Test
    void shouldRetrieveNotificationHistoryFromDatabaseForUser() {
        givenPreferenceExistsInDatabase(emailPreference);
        givenMailSenderIsMocked();
        givenNotificationsExistInDatabase();

        List<Notification> result = whenGetHistoryIsCalled(userId);

        thenHistoryShouldContainExpectedNotifications(result);
        thenHistoryShouldNotContainDeletedNotifications(result);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoNotifications() {
        givenPreferenceExistsInDatabase(emailPreference);

        List<Notification> result = whenGetHistoryIsCalled(userId);

        thenHistoryShouldBeEmpty(result);
    }

    @Test
    void shouldPersistMultipleNotificationsForSameUser() {
        givenPreferenceExistsInDatabase(emailPreference);
        givenMailSenderIsMocked();

        Notification firstNotification = whenSendNotificationIsCalled(notificationRequest);
        NotificationRequest secondRequest = NotificationRequest.builder()
                .userId(userId)
                .subject("Second Subject")
                .body("Second Body")
                .build();
        Notification secondNotification = whenSendNotificationIsCalled(secondRequest);

        thenBothNotificationsShouldBePersisted(firstNotification, secondNotification);
        thenRepositoryShouldContainTwoNotifications(userId);
    }

    private void givenPreferenceExistsInDatabase(NotificationPreference preference) {
        preferenceRepository.save(preference);
    }

    private void givenMailSenderIsMocked() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
    }

    private void givenMailSenderThrowsException() {
        doThrow(new RuntimeException("Mail service unavailable")).when(mailSender).send(any(SimpleMailMessage.class));
    }

    private void givenSmsProviderReturns(boolean success) {
        when(smsProvider.sendSms(anyString(), anyString())).thenReturn(success);
    }

    private void givenNotificationsExistInDatabase() {
        Notification notification1 = Notification.builder()
                .subject("Subject 1")
                .body("Body 1")
                .createdOn(LocalDateTime.now())
                .type(NotificationType.EMAIL)
                .userId(userId)
                .status(NotificationStatus.SUCCEEDED)
                .deleted(false)
                .build();

        Notification notification2 = Notification.builder()
                .subject("Subject 2")
                .body("Body 2")
                .createdOn(LocalDateTime.now().minusHours(1))
                .type(NotificationType.SMS)
                .userId(userId)
                .status(NotificationStatus.SUCCEEDED)
                .deleted(false)
                .build();

        Notification deletedNotification = Notification.builder()
                .subject("Deleted Subject")
                .body("Deleted Body")
                .createdOn(LocalDateTime.now().minusDays(1))
                .type(NotificationType.EMAIL)
                .userId(userId)
                .status(NotificationStatus.SUCCEEDED)
                .deleted(true)
                .build();

        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(deletedNotification);
    }

    private Notification whenSendNotificationIsCalled(NotificationRequest request) {
        return notificationService.send(request);
    }

    private List<Notification> whenGetHistoryIsCalled(UUID userId) {
        return notificationService.getHistory(userId);
    }

    private void thenNotificationShouldBePersistedInDatabase(Notification notification) {
        assertThat(notification.getId()).isNotNull();
        assertThat(notificationRepository.existsById(notification.getId())).isTrue();
    }

    private void thenNotificationShouldHaveCorrectData(Notification notification) {
        assertThat(notification.getSubject()).isEqualTo("Integration Test Subject");
        assertThat(notification.getBody()).isEqualTo("Integration Test Body");
        assertThat(notification.getUserId()).isEqualTo(userId);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SUCCEEDED);
        assertThat(notification.isDeleted()).isFalse();
    }

    private void thenNotificationStatusShouldBe(Notification notification, NotificationStatus expectedStatus) {
        assertThat(notification.getStatus()).isEqualTo(expectedStatus);
    }

    private void thenSmsProviderShouldBeCalled() {
        verify(smsProvider, times(1)).sendSms(anyString(), anyString());
    }

    private void thenNoNotificationsShouldBeSaved() {
        assertThat(notificationRepository.count()).isZero();
    }

    private void thenHistoryShouldContainExpectedNotifications(List<Notification> result) {
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Notification::getSubject)
                .containsExactlyInAnyOrder("Subject 1", "Subject 2");
    }

    private void thenHistoryShouldNotContainDeletedNotifications(List<Notification> result) {
        assertThat(result).noneMatch(Notification::isDeleted);
    }

    private void thenHistoryShouldBeEmpty(List<Notification> result) {
        assertThat(result).isEmpty();
    }

    private void thenBothNotificationsShouldBePersisted(Notification first, Notification second) {
        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isNotNull();
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    private void thenRepositoryShouldContainTwoNotifications(UUID userId) {
        List<Notification> allNotifications = notificationRepository.findByUserId(userId);
        assertThat(allNotifications).hasSize(2);
    }
}

