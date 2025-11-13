package app.web;
import app.model.NotificationPreference;
import app.service.NotificationPreferenceService;
import app.web.dto.PreferenceRequest;
import app.web.dto.PreferenceResponse;
import app.web.mapper.DtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/preferences")
public class PreferenceController {

    private final NotificationPreferenceService notificationPreferenceService;

    public PreferenceController(NotificationPreferenceService notificationPreferenceService) {
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @PostMapping
    public ResponseEntity<PreferenceResponse> createPreference(@RequestBody PreferenceRequest request) {

            NotificationPreference preference =notificationPreferenceService.upsert(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DtoMapper.from(preference));
    }
    @GetMapping
    public ResponseEntity<PreferenceResponse> getPreference(@RequestParam ("userId") UUID userId) {

        NotificationPreference userNotification = notificationPreferenceService.getByUserId(userId);

        return ResponseEntity.ok(DtoMapper.from(userNotification));
    }
}
