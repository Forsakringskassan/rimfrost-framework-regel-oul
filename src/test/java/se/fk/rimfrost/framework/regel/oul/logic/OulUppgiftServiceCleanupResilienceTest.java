package se.fk.rimfrost.framework.regel.oul.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.regel.oul.base.OulUppgiftServiceTestBase;
import se.fk.rimfrost.framework.regel.oul.helpers.OulTestData;
import se.fk.rimfrost.framework.regel.oul.helpers.WireMockRegelOul;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.oul.storage.CloudEventDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.ProcessTopicInfoStorage;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Tests {@link OulUppgiftService#cleanupCorrelation(UUID)} best-effort
 * semantics (FROUL-FR-03.6): a failure in one of the three delete operations
 * must not prevent the other two from being executed.
 *
 * <p>Uses {@code @InjectMock} on {@link ProcessTopicInfoStorage} configured to
 * throw on {@code delete}, while the other two storage beans remain the real
 * JPA implementations so that "the other two were deleted" can be asserted by
 * reading through the real storage API after seeding real rows.
 *
 * <p>The happy-path counterpart (FROUL-FR-03.5) lives in
 * {@code OulUppgiftServiceEndAndCleanupTest} — see the note there for why the
 * two scenarios are in separate classes.
 */
@QuarkusTest
@QuarkusTestResource(WireMockRegelOul.class)
class OulUppgiftServiceCleanupResilienceTest extends OulUppgiftServiceTestBase
{
   @Inject
   OulUppgiftService oulUppgiftService;

   @Inject
   CloudEventDataStorage cloudEventDataStorage;

   @InjectMock
   ProcessTopicInfoStorage processTopicInfoStorage;

   @Inject
   RegelCommonDataStorage regelCommonDataStorage;

   @Test
   @DisplayName("FROUL-FR-03.6: cleanupCorrelation fortsätter även om en enskild radering misslyckas")
   void cleanupCorrelation_should_continue_when_one_delete_fails()
   {
      UUID handlaggningId = UUID.randomUUID();
      doThrow(new RuntimeException("simulated delete failure"))
            .when(processTopicInfoStorage).deleteProcessTopicInfo(any());

      CloudEventData cloudEventData = OulTestData.cloudEventData();
      cloudEventDataStorage.setCloudEventData(handlaggningId, cloudEventData);
      RegelCommonData regelCommonData = ImmutableRegelCommonData.builder()
            .uppgift(OulTestData.seedUppgift())
            .oulUppgiftId(UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID))
            .build();
      regelCommonDataStorage.setRegelCommonData(handlaggningId, regelCommonData);

      assertThat(cloudEventDataStorage.getCloudEventData(handlaggningId)).isNotNull();
      assertThat(regelCommonDataStorage.getRegelCommonData(handlaggningId)).isNotNull();

      oulUppgiftService.cleanupCorrelation(handlaggningId);

      verify(processTopicInfoStorage).deleteProcessTopicInfo(handlaggningId);
      assertThat(cloudEventDataStorage.getCloudEventData(handlaggningId)).isNull();
      assertThat(regelCommonDataStorage.getRegelCommonData(handlaggningId)).isNull();
   }
}
