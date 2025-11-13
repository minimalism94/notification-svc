package app.service;

import app.model.NotificationPreference;
import app.model.NotificationType;
import app.repository.NotificationPreferenceRepository;
import app.web.dto.PreferenceRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    public NotificationPreference upsert(PreferenceRequest request) {

        Optional<NotificationPreference> preferenceOpt = preferenceRepository.findByUserId(request.getUserId());
        if (preferenceOpt.isPresent()) {
            NotificationPreference preference = preferenceOpt.get();
            preference.setEnabled(request.isNotificationEnabled());
            preference.setContactInfo(request.getContactInfo());
            

            if (request.getType() != null) {
                preference.setType(request.getType());
            } else {
                NotificationType detectedType = detectTypeFromContactInfo(request.getContactInfo());
                preference.setType(detectedType);
            }
            
            preference.setUpdatedOn(LocalDateTime.now());
            return preferenceRepository.save(preference);
        }
        

        NotificationType type;
        if (request.getType() != null) {
            type = request.getType();
        } else {
            type = detectTypeFromContactInfo(request.getContactInfo());
        }

        NotificationPreference preference = NotificationPreference.builder()
                .userId(request.getUserId())
                .type(type)
                .enabled(request.isNotificationEnabled())
                .contactInfo(request.getContactInfo())
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        return preferenceRepository.save(preference);
    }
    
    private NotificationType detectTypeFromContactInfo(String contactInfo) {
        if (contactInfo == null || contactInfo.trim().isEmpty()) {
            return NotificationType.EMAIL;
        }
        if (contactInfo.contains("@")) {
            return NotificationType.EMAIL;
        } else {
            return NotificationType.SMS;
        }
    }

    public NotificationPreference getByUserId(UUID userId) {

        return preferenceRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Preference for this user does not exist."));
    }
}