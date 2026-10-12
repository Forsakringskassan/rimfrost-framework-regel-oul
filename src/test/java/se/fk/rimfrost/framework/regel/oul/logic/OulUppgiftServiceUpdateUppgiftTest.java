package se.fk.rimfrost.framework.regel.oul.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.regel.logic.RegelCancelledException;
import se.fk.rimfrost.framework.regel.oul.base.OulUppgiftServiceTestBase;
import se.fk.rimfrost.framework.regel.oul.helpers.OulTestData;
import se.fk.rimfrost.framework.regel.oul.helpers.WireMockRegelOul;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Tests for {@link OulUppgiftService#updateUppgift(UUID, Uppgift, se.fk.rimfrost.framework.regel.logic.entity.CloudEventData)}
 * covering FROUL-FR-01.15.
 */
@QuarkusTest
@QuarkusTestResource(WireMockRegelOul.class)
class OulUppgiftServiceUpdateUppgiftTest extends OulUppgiftServiceTestBase
{
   private static final ObjectMapper MAPPER = new ObjectMapper();
   private static final String NEW_STATUS = "UNDER_BEHANDLING";
   private static final int SEEDED_VERSION = 2;

   @Inject
   OulUppgiftService oulUppgiftService;

   @Inject
   RegelCommonDataStorage regelCommonDataStorage;

   /**
    * Seeds a {@link RegelCommonData} row for {@code handlaggningId} at version
    * {@link #SEEDED_VERSION} so tests have prior state to update against.
    *
    * @param handlaggningId handläggning id to seed under
    */
   private void seedRegelCommonData(UUID handlaggningId)
   {
      Uppgift uppgift = ImmutableUppgift.builder()
            .id(UUID.randomUUID())
            .version(SEEDED_VERSION)
            .aktivitetId(UUID.randomUUID())
            .skapadTS(OffsetDateTime.now())
            .uppgiftStatus("NY")
            .fSSAinformation("FSSAinformation.HANDLAGGNING_PAGAR")
            .uppgiftSpecifikation(ImmutableUppgiftSpecifikation.builder()
                  .id(UUID.randomUUID())
                  .version(1)
                  .build())
            .build();
      regelCommonDataStorage.setRegelCommonData(handlaggningId, ImmutableRegelCommonData.builder()
            .uppgift(uppgift)
            .oulUppgiftId(UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID))
            .build());
   }

   @Test
   @DisplayName("FROUL-FR-01.15: updateUppgift lagrar uppdaterad uppgift i RegelCommonData")
   void updateUppgift_should_persist_updated_uppgift()
   {
      UUID handlaggningId = UUID.fromString(WireMockRegelOul.DEFAULT_HANDLAGGNING_ID);
      seedRegelCommonData(handlaggningId);
      Uppgift updated = ImmutableUppgift.builder()
            .from(regelCommonDataStorage.getRegelCommonData(handlaggningId).uppgift())
            .version(SEEDED_VERSION + 1)
            .uppgiftStatus(NEW_STATUS)
            .build();

      oulUppgiftService.updateUppgift(handlaggningId, updated, OulTestData.cloudEventData());

      RegelCommonData stored = regelCommonDataStorage.getRegelCommonData(handlaggningId);
      assertThat(stored.uppgift().version()).isEqualTo(SEEDED_VERSION + 1);
      assertThat(stored.uppgift().uppgiftStatus()).isEqualTo(NEW_STATUS);
   }

   @Test
   @DisplayName("FROUL-FR-01.15: updateUppgift synkroniserar uppdaterad uppgift till handläggning via PUT")
   void updateUppgift_should_sync_to_handlaggning_via_put() throws Exception
   {
      UUID handlaggningId = UUID.fromString(WireMockRegelOul.DEFAULT_HANDLAGGNING_ID);
      seedRegelCommonData(handlaggningId);
      Uppgift updated = ImmutableUppgift.builder()
            .from(regelCommonDataStorage.getRegelCommonData(handlaggningId).uppgift())
            .version(SEEDED_VERSION + 1)
            .uppgiftStatus(NEW_STATUS)
            .build();

      oulUppgiftService.updateUppgift(handlaggningId, updated, OulTestData.cloudEventData());

      List<LoggedRequest> puts = WireMockRegelOul.waitForHandlaggningRequests(
            handlaggningId.toString(), RequestMethod.PUT, 1);
      assertThat(puts).hasSize(1);
      JsonNode body = MAPPER.readTree(puts.getFirst().getBodyAsString());
      assertThat(body.at("/uppgift/uppgiftStatus").asText()).isEqualTo(NEW_STATUS);
      assertThat(body.at("/uppgift/version").asInt()).isEqualTo(SEEDED_VERSION + 1);
   }

   @Test
   @DisplayName("FROUL-FR-01.15: updateUppgift kastar RegelCancelledException om RegelCommonData saknas")
   void updateUppgift_should_throw_when_no_common_data()
   {
      assertThatThrownBy(
            () -> oulUppgiftService.updateUppgift(UUID.randomUUID(), OulTestData.seedUppgift(), OulTestData.cloudEventData()))
            .isInstanceOf(RegelCancelledException.class);
   }
}
