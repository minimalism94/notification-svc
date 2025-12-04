package app.service;

import app.model.NotificationPreference;
import app.model.NotificationType;
import app.repository.NotificationPreferenceRepository;
import app.web.dto.PreferenceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationPreferenceServiceIntegrationTest {

    @Autowired
    private NotificationPreferenceService preferenceService;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    private UUID userId;
    private PreferenceRequest preferenceRequest;

    @BeforeEach
    void setUp() {
        preferenceRepository.deleteAll();

        userId = UUID.randomUUID();

        preferenceRequest = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("test@example.com")
                .type(NotificationType.EMAIL)
                .build();
    }

    @Test
    void shouldPersistNewPreferenceToDatabaseWhenCreating() {
        NotificationPreference result = whenUpsertIsCalled(preferenceRequest);

        thenPreferenceShouldBePersistedInDatabase(result);
        thenPreferenceShouldHaveCorrectData(result);
    }

    @Test
    void shouldUpdateExistingPreferenceInDatabaseWhenUpdating() {
        givenPreferenceExistsInDatabase();

        PreferenceRequest updateRequest = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(false)
                .contactInfo("updated@example.com")
                .type(NotificationType.SMS)
                .build();

        NotificationPreference result = whenUpsertIsCalled(updateRequest);

        thenPreferenceShouldBeUpdatedInDatabase(result, updateRequest);
        thenRepositoryShouldContainOnlyOnePreference(userId);
    }

    @Test
    void shouldDetectEmailTypeFromContactInfoWhenTypeNotProvided() {
        PreferenceRequest requestWithoutType = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("user@example.com")
                .build();

        NotificationPreference result = whenUpsertIsCalled(requestWithoutType);

        thenNotificationTypeShouldBe(result, NotificationType.EMAIL);
    }

    @Test
    void shouldDetectSmsTypeFromContactInfoWhenTypeNotProvided() {
        PreferenceRequest requestWithoutType = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("359893454943")
                .build();

        NotificationPreference result = whenUpsertIsCalled(requestWithoutType);

        thenNotificationTypeShouldBe(result, NotificationType.SMS);
    }

    @Test
    void shouldRetrievePreferenceFromDatabaseByUserId() {
        givenPreferenceExistsInDatabase();

        NotificationPreference result = whenGetByUserIdIsCalled(userId);

        thenPreferenceShouldBeRetrieved(result);
    }

    @Test
    void shouldThrowExceptionWhenPreferenceDoesNotExistInDatabase() {
        assertThatThrownBy(() -> whenGetByUserIdIsCalled(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Preference for this user does not exist");
    }

    @Test
    void shouldUpdateTimestampWhenPreferenceIsUpdated() {
        NotificationPreference existing = givenPreferenceExistsInDatabase();
        LocalDateTime originalUpdatedOn = existing.getUpdatedOn();

        PreferenceRequest updateRequest = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(false)
                .contactInfo("updated@example.com")
                .type(NotificationType.EMAIL)
                .build();

        NotificationPreference result = whenUpsertIsCalled(updateRequest);

        thenUpdatedTimestampShouldBeAfterOriginal(result, originalUpdatedOn);
    }

    @Test
    void shouldPersistMultiplePreferencesForDifferentUsers() {
        UUID secondUserId = UUID.randomUUID();

        PreferenceRequest firstRequest = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("first@example.com")
                .type(NotificationType.EMAIL)
                .build();

        PreferenceRequest secondRequest = PreferenceRequest.builder()
                .userId(secondUserId)
                .notificationEnabled(true)
                .contactInfo("second@example.com")
                .type(NotificationType.EMAIL)
                .build();

        NotificationPreference firstResult = whenUpsertIsCalled(firstRequest);
        NotificationPreference secondResult = whenUpsertIsCalled(secondRequest);

        thenBothPreferencesShouldBePersisted(firstResult, secondResult);
        thenRepositoryShouldContainTwoPreferences();
    }

    @Test
    void shouldMaintainUniqueConstraintOnUserId() {
        givenPreferenceExistsInDatabase();

        PreferenceRequest duplicateRequest = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("duplicate@example.com")
                .type(NotificationType.EMAIL)
                .build();

        NotificationPreference result = whenUpsertIsCalled(duplicateRequest);

        thenRepositoryShouldContainOnlyOnePreference(userId);
        thenPreferenceShouldBeUpdatedNotCreated(result);
    }

    private NotificationPreference givenPreferenceExistsInDatabase() {
        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(true)
                .contactInfo("existing@example.com")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        return preferenceRepository.save(preference);
    }

    private NotificationPreference whenUpsertIsCalled(PreferenceRequest request) {
        return preferenceService.upsert(request);
    }

    private NotificationPreference whenGetByUserIdIsCalled(UUID userId) {
        return preferenceService.getByUserId(userId);
    }

    private void thenPreferenceShouldBePersistedInDatabase(NotificationPreference preference) {
        assertThat(preference.getId()).isNotNull();
        assertThat(preferenceRepository.existsById(preference.getId())).isTrue();
    }

    private void thenPreferenceShouldHaveCorrectData(NotificationPreference preference) {
        assertThat(preference.getUserId()).isEqualTo(userId);
        assertThat(preference.isEnabled()).isTrue();
        assertThat(preference.getContactInfo()).isEqualTo("test@example.com");
        assertThat(preference.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(preference.getCreatedOn()).isNotNull();
        assertThat(preference.getUpdatedOn()).isNotNull();
    }

    private void thenPreferenceShouldBeUpdatedInDatabase(NotificationPreference result, PreferenceRequest request) {
        assertThat(result.getContactInfo()).isEqualTo(request.getContactInfo());
        assertThat(result.isEnabled()).isEqualTo(request.isNotificationEnabled());
        assertThat(result.getType()).isEqualTo(request.getType());
    }

    private void thenNotificationTypeShouldBe(NotificationPreference result, NotificationType expectedType) {
        assertThat(result.getType()).isEqualTo(expectedType);
    }

    private void thenPreferenceShouldBeRetrieved(NotificationPreference result) {
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        Optional<NotificationPreference> fromDb = preferenceRepository.findByUserId(userId);
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getId()).isEqualTo(result.getId());
    }

    private void thenUpdatedTimestampShouldBeAfterOriginal(NotificationPreference result, LocalDateTime original) {
        assertThat(result.getUpdatedOn()).isAfter(original);
    }

    private void thenBothPreferencesShouldBePersisted(NotificationPreference first, NotificationPreference second) {
        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isNotNull();
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    private void thenRepositoryShouldContainTwoPreferences() {
        assertThat(preferenceRepository.count()).isEqualTo(2);
    }

    private void thenRepositoryShouldContainOnlyOnePreference(UUID userId) {
        assertThat(preferenceRepository.findByUserId(userId)).isPresent();
        assertThat(preferenceRepository.count()).isEqualTo(1);
    }

    private void thenPreferenceShouldBeUpdatedNotCreated(NotificationPreference result) {
        Optional<NotificationPreference> fromDb = preferenceRepository.findByUserId(userId);
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getId()).isEqualTo(result.getId());
        assertThat(result.getContactInfo()).isEqualTo("duplicate@example.com");
    }
}

