package app.web.dto;

import app.model.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceRequest {

    @JsonProperty("userId")
    private UUID userId;

    @JsonProperty("notificationEnabled")
    private boolean notificationEnabled;
    
    @JsonProperty("contactInfo")
    private String contactInfo;
    
    @JsonProperty("type")
    private NotificationType type;
}
