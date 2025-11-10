package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService preferenceService;
    private  final MailSender mailSender;
    private final SmsNotificationService smsNotificationService;

    public NotificationService(NotificationRepository notificationRepository, NotificationPreferenceService preferenceService, MailSender mailSender, SmsNotificationService smsNotificationService) {
        this.notificationRepository = notificationRepository;
        this.preferenceService = preferenceService;
        this.mailSender = mailSender;
        this.smsNotificationService = smsNotificationService;
    }

    public Notification send(NotificationRequest request) {

        NotificationPreference preference = preferenceService.getByUserId(request.getUserId());

        boolean enabled = preference.isEnabled();
        if (!enabled) {
            throw new IllegalStateException("User with id=[%s] turned off their notifications.".formatted(request.getUserId()));
        }

        Notification notification = Notification.builder()
                .subject(request.getSubject())
                .body(request.getBody())
                .createdOn(LocalDateTime.now())
                .type(preference.getType())
                .userId(request.getUserId())
                .deleted(false)
                .build();


        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(preference.getContactInfo());
        mailMessage.setSubject(request.getSubject());
        mailMessage.setText(request.getBody());

        try {
            if (preference.getType() == NotificationType.EMAIL) {
                mailSender.send(mailMessage);

            } else if (preference.getType() == NotificationType.SMS) {
                smsNotificationService.sendSms(preference.getContactInfo(), request.getBody());
            }
            notification.setStatus(NotificationStatus.SUCCEEDED);
        } catch (Exception e) {
            log.error("Failed email due to: {}", e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
        }

        return notificationRepository.save(notification);
    }


    public List<Notification> getHistory(UUID userId) {

        return notificationRepository.findByUserId(userId).stream().filter(n -> !n.isDeleted()).toList();
    }
}