package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private MailSender mailSender;

    @Mock
    private SmsProvider smsProvider;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private NotificationRequest notificationRequest;
    private NotificationPreference emailPreference;
    private NotificationPreference smsPreference;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        notificationRequest = NotificationRequest.builder()
                .userId(userId)
                .subject("Test Subject")
                .body("Test Body")
                .build();

        emailPreference = NotificationPreference.builder()
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(true)
                .contactInfo("test@example.com")
                .build();

        smsPreference = NotificationPreference.builder()
                .userId(userId)
                .type(NotificationType.SMS)
                .enabled(true)
                .contactInfo("359893454943")
                .build();

    }

    @Test
    void shouldSendEmailNotificationSuccessfully() {
        givenPreferenceServiceReturns(emailPreference);
        givenNotificationRepositorySaves();

        Notification result = whenSendNotificationIsCalled(notificationRequest);

        thenNotificationShouldBeSaved(result);
        thenMailSenderShouldBeCalled();
        thenSmsProviderShouldNotBeCalled();
        thenNotificationStatusShouldBe(result, NotificationStatus.SUCCEEDED);
    }

    @Test
    void shouldSendSmsNotificationSuccessfully() {
        givenPreferenceServiceReturns(smsPreference);
        givenSmsProviderReturns(true);
        givenNotificationRepositorySaves();

        Notification result = whenSendNotificationIsCalled(notificationRequest);

        thenNotificationShouldBeSaved(result);
        thenSmsProviderShouldBeCalled();
        thenMailSenderShouldNotBeCalled();
        thenNotificationStatusShouldBe(result, NotificationStatus.SUCCEEDED);
    }

    @Test
    void shouldMarkNotificationAsFailedWhenSmsProviderFails() {
        givenPreferenceServiceReturns(smsPreference);
        givenSmsProviderReturns(false);
        givenNotificationRepositorySaves();

        Notification result = whenSendNotificationIsCalled(notificationRequest);

        thenNotificationShouldBeSaved(result);
        thenSmsProviderShouldBeCalled();
        thenNotificationStatusShouldBe(result, NotificationStatus.FAILED);
    }

    @Test
    void shouldThrowExceptionWhenUserNotificationsAreDisabled() {
        NotificationPreference disabledPreference = NotificationPreference.builder()
                .userId(userId)
                .enabled(false)
                .build();
        givenPreferenceServiceReturns(disabledPreference);

        assertThatThrownBy(() -> whenSendNotificationIsCalled(notificationRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("turned off their notifications");

        thenNotificationRepositoryShouldNotBeCalled();
    }

    @Test
    void shouldMarkNotificationAsFailedWhenEmailSendingThrowsException() {
        givenPreferenceServiceReturns(emailPreference);
        givenMailSenderThrowsException(new RuntimeException("Email service unavailable"));
        givenNotificationRepositorySaves();

        Notification result = whenSendNotificationIsCalled(notificationRequest);

        thenNotificationShouldBeSaved(result);
        thenNotificationStatusShouldBe(result, NotificationStatus.FAILED);
    }

    @Test
    void shouldMarkNotificationAsFailedWhenSmsSendingThrowsException() {
        givenPreferenceServiceReturns(smsPreference);
        givenSmsProviderThrowsException(new RuntimeException("SMS service unavailable"));
        givenNotificationRepositorySaves();

        Notification result = whenSendNotificationIsCalled(notificationRequest);

        thenNotificationShouldBeSaved(result);
        thenNotificationStatusShouldBe(result, NotificationStatus.FAILED);
    }

    @Test
    void shouldReturnNotificationHistoryForUser() {
        Notification notification1 = createNotification(UUID.randomUUID(), false);
        Notification notification2 = createNotification(UUID.randomUUID(), false);
        Notification deletedNotification = createNotification(UUID.randomUUID(), true);
        
        givenNotificationRepositoryReturns(userId, List.of(notification1, notification2, deletedNotification));

        List<Notification> result = whenGetHistoryIsCalled(userId);

        thenHistoryShouldContainOnlyNonDeletedNotifications(result, List.of(notification1, notification2));
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoNotifications() {
        givenNotificationRepositoryReturns(userId, List.of());

        List<Notification> result = whenGetHistoryIsCalled(userId);

        thenHistoryShouldBeEmpty(result);
    }

    private void givenPreferenceServiceReturns(NotificationPreference preference) {
        when(preferenceService.getByUserId(userId)).thenReturn(preference);
    }

    private void givenNotificationRepositorySaves() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getId() == null) {
                notification.setId(UUID.randomUUID());
            }
            return notification;
        });
    }

    private void givenSmsProviderReturns(boolean success) {
        when(smsProvider.sendSms(anyString(), anyString())).thenReturn(success);
    }

    private void givenSmsProviderThrowsException(Exception exception) {
        when(smsProvider.sendSms(anyString(), anyString())).thenThrow(exception);
    }

    private void givenMailSenderThrowsException(Exception exception) {
        doThrow(exception).when(mailSender).send(any(SimpleMailMessage.class));
    }

    private void givenNotificationRepositoryReturns(UUID userId, List<Notification> notifications) {
        when(notificationRepository.findByUserId(userId)).thenReturn(notifications);
    }

    private Notification whenSendNotificationIsCalled(NotificationRequest request) {
        return notificationService.send(request);
    }

    private List<Notification> whenGetHistoryIsCalled(UUID userId) {
        return notificationService.getHistory(userId);
    }

    private void thenNotificationShouldBeSaved(Notification notification) {
        verify(notificationRepository, times(1)).save(any(Notification.class));
        assertThat(notification).isNotNull();
        assertThat(notification.getSubject()).isEqualTo("Test Subject");
        assertThat(notification.getBody()).isEqualTo("Test Body");
        assertThat(notification.getUserId()).isEqualTo(userId);
    }

    private void thenMailSenderShouldBeCalled() {
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    private void thenMailSenderShouldNotBeCalled() {
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    private void thenSmsProviderShouldBeCalled() {
        verify(smsProvider, times(1)).sendSms(anyString(), anyString());
    }

    private void thenSmsProviderShouldNotBeCalled() {
        verify(smsProvider, never()).sendSms(anyString(), anyString());
    }

    private void thenNotificationStatusShouldBe(Notification notification, NotificationStatus expectedStatus) {
        assertThat(notification.getStatus()).isEqualTo(expectedStatus);
    }

    private void thenNotificationRepositoryShouldNotBeCalled() {
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    private void thenHistoryShouldContainOnlyNonDeletedNotifications(List<Notification> result, List<Notification> expected) {
        assertThat(result).hasSize(expected.size());
        assertThat(result).containsExactlyElementsOf(expected);
        assertThat(result).allMatch(n -> !n.isDeleted());
    }

    private void thenHistoryShouldBeEmpty(List<Notification> result) {
        assertThat(result).isEmpty();
    }

    private Notification createNotification(UUID id, boolean deleted) {
        return Notification.builder()
                .id(id)
                .subject("Subject")
                .body("Body")
                .createdOn(LocalDateTime.now())
                .type(NotificationType.EMAIL)
                .userId(userId)
                .status(NotificationStatus.SUCCEEDED)
                .deleted(deleted)
                .build();
    }
}

