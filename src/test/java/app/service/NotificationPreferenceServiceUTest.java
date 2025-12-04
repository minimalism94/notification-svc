package app.service;

import app.model.NotificationPreference;
import app.model.NotificationType;
import app.repository.NotificationPreferenceRepository;
import app.web.dto.PreferenceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceUTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private NotificationPreferenceService preferenceService;

    private UUID userId;
    private PreferenceRequest preferenceRequest;
    private NotificationPreference existingPreference;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        preferenceRequest = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("test@example.com")
                .type(NotificationType.EMAIL)
                .build();

        existingPreference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(false)
                .contactInfo("old@example.com")
                .createdOn(LocalDateTime.now().minusDays(1))
                .updatedOn(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void shouldCreateNewPreferenceWhenUserPreferenceDoesNotExist() {
        givenPreferenceRepositoryReturns(Optional.empty());
        givenPreferenceRepositorySaves();

        NotificationPreference result = whenUpsertIsCalled(preferenceRequest);

        thenNewPreferenceShouldBeCreated(result);
        thenPreferenceRepositoryShouldSave();
    }

    @Test
    void shouldDetectEmailTypeFromContactInfoContainingAt() {
        PreferenceRequest requestWithoutType = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("user@example.com")
                .build();
        givenPreferenceRepositoryReturns(Optional.empty());
        givenPreferenceRepositorySaves();

        NotificationPreference result = whenUpsertIsCalled(requestWithoutType);

        thenNotificationTypeShouldBe(result, NotificationType.EMAIL);
    }

    @Test
    void shouldDetectSmsTypeFromContactInfoWithoutAt() {
        PreferenceRequest requestWithoutType = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("359893454943")
                .build();
        givenPreferenceRepositoryReturns(Optional.empty());
        givenPreferenceRepositorySaves();

        NotificationPreference result = whenUpsertIsCalled(requestWithoutType);

        thenNotificationTypeShouldBe(result, NotificationType.SMS);
    }

    @Test
    void shouldDefaultToEmailTypeWhenContactInfoIsNull() {
        PreferenceRequest requestWithoutType = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo(null)
                .build();
        givenPreferenceRepositoryReturns(Optional.empty());
        givenPreferenceRepositorySaves();

        NotificationPreference result = whenUpsertIsCalled(requestWithoutType);

        thenNotificationTypeShouldBe(result, NotificationType.EMAIL);
    }

    @Test
    void shouldDefaultToEmailTypeWhenContactInfoIsEmpty() {
        PreferenceRequest requestWithoutType = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("   ")
                .build();
        givenPreferenceRepositoryReturns(Optional.empty());
        givenPreferenceRepositorySaves();

        NotificationPreference result = whenUpsertIsCalled(requestWithoutType);

        thenNotificationTypeShouldBe(result, NotificationType.EMAIL);
    }

    @Test
    void shouldUseProvidedTypeWhenTypeIsExplicitlySet() {
        PreferenceRequest requestWithType = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("359893454943")
                .type(NotificationType.EMAIL)
                .build();
        givenPreferenceRepositoryReturns(Optional.empty());
        givenPreferenceRepositorySaves();

        NotificationPreference result = whenUpsertIsCalled(requestWithType);

        thenNotificationTypeShouldBe(result, NotificationType.EMAIL);
    }

    @Test
    void shouldUpdateTypeWhenUpdatingExistingPreferenceWithNewType() {
        PreferenceRequest updateRequest = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("359893454943")
                .type(NotificationType.SMS)
                .build();
        givenPreferenceRepositoryReturns(Optional.of(existingPreference));
        givenPreferenceRepositorySaves();

        NotificationPreference result = whenUpsertIsCalled(updateRequest);

        thenNotificationTypeShouldBe(result, NotificationType.SMS);
    }

    @Test
    void shouldThrowExceptionWhenGettingPreferenceForNonExistentUser() {
        givenPreferenceRepositoryReturns(Optional.empty());

        assertThatThrownBy(() -> whenGetByUserIdIsCalled(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Preference for this user does not exist");
    }

    @Test
    void shouldReturnPreferenceWhenGettingPreferenceForExistingUser() {
        givenPreferenceRepositoryReturns(Optional.of(existingPreference));

        NotificationPreference result = whenGetByUserIdIsCalled(userId);

        thenPreferenceShouldBeReturned(result, existingPreference);
    }

    private void givenPreferenceRepositoryReturns(Optional<NotificationPreference> preference) {
        when(preferenceRepository.findByUserId(userId)).thenReturn(preference);
    }

    private void givenPreferenceRepositorySaves() {
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(invocation -> {
            NotificationPreference preference = invocation.getArgument(0);
            if (preference.getId() == null) {
                preference.setId(UUID.randomUUID());
            }
            return preference;
        });
    }

    private NotificationPreference whenUpsertIsCalled(PreferenceRequest request) {
        return preferenceService.upsert(request);
    }

    private NotificationPreference whenGetByUserIdIsCalled(UUID userId) {
        return preferenceService.getByUserId(userId);
    }

    private void thenNewPreferenceShouldBeCreated(NotificationPreference result) {
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getContactInfo()).isEqualTo("test@example.com");
        assertThat(result.getCreatedOn()).isNotNull();
        assertThat(result.getUpdatedOn()).isNotNull();
    }

    private void thenExistingPreferenceShouldBeUpdated(NotificationPreference result) {
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingPreference.getId());
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getContactInfo()).isEqualTo("test@example.com");
        assertThat(result.getUpdatedOn()).isAfter(existingPreference.getUpdatedOn());
    }

    private void thenNotificationTypeShouldBe(NotificationPreference result, NotificationType expectedType) {
        assertThat(result.getType()).isEqualTo(expectedType);
    }

    private void thenPreferenceRepositoryShouldSave() {
        verify(preferenceRepository, times(1)).save(any(NotificationPreference.class));
    }

    private void thenPreferenceShouldBeReturned(NotificationPreference result, NotificationPreference expected) {
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(expected.getId());
        assertThat(result.getUserId()).isEqualTo(expected.getUserId());
    }
}

