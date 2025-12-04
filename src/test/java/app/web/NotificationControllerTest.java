package app.web;

import app.model.Notification;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.service.NotificationService;
import app.web.dto.NotificationRequest;
import app.web.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private UUID userId;
    private NotificationRequest notificationRequest;
    private Notification notification;
    private List<Notification> notificationList;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        notificationRequest = NotificationRequest.builder()
                .userId(userId)
                .subject("Test Subject")
                .body("Test Body")
                .build();

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

        Notification notification2 = Notification.builder()
                .id(UUID.randomUUID())
                .subject("Test Subject 2")
                .body("Test Body 2")
                .createdOn(LocalDateTime.now().minusHours(1))
                .type(NotificationType.SMS)
                .userId(userId)
                .status(NotificationStatus.SUCCEEDED)
                .deleted(false)
                .build();

        notificationList = List.of(notification, notification2);
    }

    @Test
    void shouldSendNotificationAndReturnCreatedStatus() {
        givenNotificationServiceSends(notification);

        ResponseEntity<NotificationResponse> result = whenSendNotificationIsCalled(notificationRequest);

        thenResponseShouldHaveCreatedStatus(result);
        thenResponseBodyShouldContainNotificationData(result.getBody());
        thenNotificationServiceShouldBeCalled();
    }

    @Test
    void shouldReturnNotificationHistoryForUser() {
        givenNotificationServiceReturnsHistory(notificationList);

        ResponseEntity<List<NotificationResponse>> result = whenGetHistoryIsCalled(userId);

        thenResponseShouldHaveOkStatus(result);
        thenResponseBodyShouldContainHistory(result.getBody(), notificationList);
        thenNotificationServiceShouldBeCalledForHistory();
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoNotifications() {
        givenNotificationServiceReturnsHistory(List.of());

        ResponseEntity<List<NotificationResponse>> result = whenGetHistoryIsCalled(userId);

        thenResponseShouldHaveOkStatus(result);
        thenResponseBodyShouldBeEmpty(result.getBody());
    }

    private void givenNotificationServiceSends(Notification notification) {
        when(notificationService.send(any(NotificationRequest.class))).thenReturn(notification);
    }

    private void givenNotificationServiceReturnsHistory(List<Notification> notifications) {
        when(notificationService.getHistory(any(UUID.class))).thenReturn(notifications);
    }

    private ResponseEntity<NotificationResponse> whenSendNotificationIsCalled(NotificationRequest request) {
        return notificationController.sendNotification(request);
    }

    private ResponseEntity<List<NotificationResponse>> whenGetHistoryIsCalled(UUID userId) {
        return notificationController.getHistory(userId);
    }

    private void thenResponseShouldHaveCreatedStatus(ResponseEntity<NotificationResponse> result) {
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void thenResponseShouldHaveOkStatus(ResponseEntity<?> result) {
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void thenResponseBodyShouldContainNotificationData(NotificationResponse response) {
        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("Test Subject");
        assertThat(response.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.SUCCEEDED);
    }

    private void thenResponseBodyShouldContainHistory(List<NotificationResponse> responses, List<Notification> expected) {
        assertThat(responses).hasSize(expected.size());
        assertThat(responses.get(0).getSubject()).isEqualTo(expected.get(0).getSubject());
        assertThat(responses.get(1).getSubject()).isEqualTo(expected.get(1).getSubject());
    }

    private void thenResponseBodyShouldBeEmpty(List<NotificationResponse> responses) {
        assertThat(responses).isEmpty();
    }

    private void thenNotificationServiceShouldBeCalled() {
        verify(notificationService, times(1)).send(any(NotificationRequest.class));
    }

    private void thenNotificationServiceShouldBeCalledForHistory() {
        verify(notificationService, times(1)).getHistory(any(UUID.class));
    }
}

