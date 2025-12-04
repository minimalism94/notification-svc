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
    private final SmsProvider smsProvider;

    public NotificationService(NotificationRepository notificationRepository, 
                              NotificationPreferenceService preferenceService, 
                              MailSender mailSender, 
                              SmsProvider smsProvider) {
        this.notificationRepository = notificationRepository;
        this.preferenceService = preferenceService;
        this.mailSender = mailSender;
        this.smsProvider = smsProvider;
    }

    public Notification send(NotificationRequest request) {

        NotificationPreference preference = preferenceService.getByUserId(request.getUserId());

        boolean enabled = preference.isEnabled();
        if (!enabled) {
            throw new IllegalStateException("User with id=[%s] turned off their notifications.".formatted(request.getUserId()));
        }

        log.info("[Notification] Sending notification - UserId: {}, Type: {}, ContactInfo: {}, Subject: {}", 
                request.getUserId(), preference.getType(), preference.getContactInfo(), request.getSubject());

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
                log.info("[Notification] Sending EMAIL to: {}", preference.getContactInfo());
                mailSender.send(mailMessage);
                log.info("[Notification] EMAIL sent successfully");
                notification.setStatus(NotificationStatus.SUCCEEDED);

            } else if (preference.getType() == NotificationType.SMS) {
                log.info("[Notification] Sending SMS/WhatsApp to: {}", preference.getContactInfo());
                boolean smsSent = smsProvider.sendSms(preference.getContactInfo(), request.getBody());
                if (smsSent) {
                    log.info("[Notification] SMS/WhatsApp sent and delivered successfully");
                    notification.setStatus(NotificationStatus.SUCCEEDED);
                } else {
                    log.error("[Notification] SMS/WhatsApp failed to deliver - marking as FAILED");
                    notification.setStatus(NotificationStatus.FAILED);
                }
            } else {
                log.warn("[Notification] Unknown notification type: {}", preference.getType());
                notification.setStatus(NotificationStatus.FAILED);
            }
        } catch (Exception e) {
            log.error("[Notification] Failed to send {} notification due to: {}", preference.getType(), e.getMessage(), e);
            notification.setStatus(NotificationStatus.FAILED);
        }

        return notificationRepository.save(notification);
    }


    public List<Notification> getHistory(UUID userId) {

        return notificationRepository.findByUserId(userId).stream().filter(n -> !n.isDeleted()).toList();
    }
}