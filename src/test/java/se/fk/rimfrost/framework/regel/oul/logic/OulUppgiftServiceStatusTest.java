package se.fk.rimfrost.framework.regel.oul.logic;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
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
import se.fk.rimfrost.framework.oul.logic.OulHandlerInterface;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.regel.oul.base.OulUppgiftServiceTestBase;
import se.fk.rimfrost.framework.regel.oul.helpers.OulTestData;
import se.fk.rimfrost.framework.regel.oul.helpers.WireMockRegelOul;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Tests for {@link OulUppgiftService#handleOulStatus(OulStatus)} covering the
 * OUL status callback flow (FROUL-FR-02.2..02.5) plus the CDI contract that
 * partially covers FROUL-FR-02.1 (Kafka subscription is framework-oul's
 * responsibility; here we only verify that {@code OulUppgiftService} is
 * resolvable as an {@link OulHandlerInterface} implementation).
 *
 * <p>Pre-seeds {@code RegelCommonData} directly via
 * {@link RegelCommonDataStorage} before invoking {@code handleOulStatus};
 * asserts on the stored row and on the outbound PUT captured by WireMock.
 */
@QuarkusTest
@QuarkusTestResource(WireMockRegelOul.class)
class OulUppgiftServiceStatusTest extends OulUppgiftServiceTestBase
{
   private static final ObjectMapper MAPPER = new ObjectMapper();
   private static final String NEW_STATUS = "AVSLUTAD";
   private static final int SEEDED_UPPGIFT_VERSION = 3;
   /** Version of the handläggning returned by the WireMock GET stub. */
   private static final int STUBBED_HANDLAGGNING_VERSION = 1;

   @Inject
   OulUppgiftService oulUppgiftService;

   @Inject
   OulHandlerInterface oulHandler;

   @Inject
   RegelCommonDataStorage regelCommonDataStorage;

   /**
    * Seeds a {@link RegelCommonData} row for {@code handlaggningId} with an
    * {@link Uppgift} at version {@link #SEEDED_UPPGIFT_VERSION} and status
    * {@code NY}. Used by the FROUL-FR-02.2..02.4 tests to have prior state
    * against which the status update can be measured.
    *
    * @param handlaggningId handläggning id to seed under
    */
   private void seedRegelCommonData(UUID handlaggningId)
   {
      Uppgift uppgift = ImmutableUppgift.builder()
            .id(UUID.randomUUID())
            .version(SEEDED_UPPGIFT_VERSION)
            .aktivitetId(UUID.randomUUID())
            .skapadTs(OffsetDateTime.now())
            .uppgiftStatus("NY")
            .fSSAinformation("FSSAinformation.HANDLAGGNING_PAGAR")
            .uppgiftSpecifikation(ImmutableUppgiftSpecifikation.builder()
                  .id(UUID.randomUUID())
                  .version(1)
                  .build())
            .build();
      RegelCommonData data = ImmutableRegelCommonData.builder()
            .uppgift(uppgift)
            .oulUppgiftId(UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID))
            .build();
      regelCommonDataStorage.setRegelCommonData(handlaggningId, data);
   }

   @Test
   @DisplayName("FROUL-FR-02.1: OulUppgiftService är registrerad som OulHandlerInterface via CDI")
   void oulUppgiftService_should_be_registered_as_oulHandlerInterface_cdi_bean()
   {
      assertThat(oulHandler).isNotNull();
      assertThat(oulHandler).isInstanceOf(OulUppgiftService.class);
      assertThat(oulHandler).isSameAs(oulUppgiftService);
   }

   @Test
   @DisplayName("FROUL-FR-02.2: handleOulStatus uppdaterar version, status, utförar-ID och planerad tidsstämpel")
   void status_should_update_stored_uppgift_status_utforar_planerad()
   {
      UUID handlaggningId = UUID.fromString(WireMockRegelOul.DEFAULT_HANDLAGGNING_ID);
      seedRegelCommonData(handlaggningId);
      CloudEventData ced = OulTestData.cloudEventData();
      OulStatus status = OulTestData.oulStatus(handlaggningId,
            UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID), NEW_STATUS, ced);

      oulUppgiftService.handleOulStatus(status);

      RegelCommonData stored = regelCommonDataStorage.getRegelCommonData(handlaggningId);
      assertThat(stored).isNotNull();
      assertThat(stored.uppgift().version()).isEqualTo(SEEDED_UPPGIFT_VERSION + 1);
      assertThat(stored.uppgift().uppgiftStatus()).isEqualTo(NEW_STATUS);
      assertThat(stored.uppgift().utforarId()).isNotNull();
      assertThat(stored.uppgift().utforarId().typId()).isEqualTo(OulTestData.DEFAULT_UTFORAR_TYP);
      assertThat(stored.uppgift().utforarId().varde()).isEqualTo(OulTestData.DEFAULT_UTFORAR_VARDE);
      assertThat(stored.uppgift().planeradTs()).isEqualTo(status.planeradTill());
   }

   @Test
   @DisplayName("FROUL-FR-02.3: handleOulStatus synkroniserar uppdaterad uppgift till handläggning via PUT")
   void status_should_sync_to_handlaggning_via_put() throws Exception
   {
      UUID handlaggningId = UUID.fromString(WireMockRegelOul.DEFAULT_HANDLAGGNING_ID);
      seedRegelCommonData(handlaggningId);
      OulStatus status = OulTestData.oulStatus(handlaggningId,
            UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID), NEW_STATUS,
            OulTestData.cloudEventData());

      oulUppgiftService.handleOulStatus(status);

      List<LoggedRequest> puts = WireMockRegelOul.waitForHandlaggningRequests(
            handlaggningId.toString(), RequestMethod.PUT, 1);
      assertThat(puts).hasSize(1);
      JsonNode body = MAPPER.readTree(puts.getFirst().getBodyAsString());
      assertThat(body.at("/handlaggning/uppgift/uppgiftStatus").asText()).isEqualTo(NEW_STATUS);
      assertThat(body.at("/handlaggning/uppgift/version").asInt()).isEqualTo(SEEDED_UPPGIFT_VERSION + 1);
   }

   @Test
   @DisplayName("FROUL-FR-02.4: handleOulStatus inkrementerar inte handläggningens egen version")
   void status_should_not_increment_handlaggning_version() throws Exception
   {
      UUID handlaggningId = UUID.fromString(WireMockRegelOul.DEFAULT_HANDLAGGNING_ID);
      seedRegelCommonData(handlaggningId);
      OulStatus status = OulTestData.oulStatus(handlaggningId,
            UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID), NEW_STATUS,
            OulTestData.cloudEventData());

      oulUppgiftService.handleOulStatus(status);

      List<LoggedRequest> puts = WireMockRegelOul.waitForHandlaggningRequests(
            handlaggningId.toString(), RequestMethod.PUT, 1);
      assertThat(puts).isNotEmpty();
      JsonNode body = MAPPER.readTree(puts.getLast().getBodyAsString());
      assertThat(body.at("/handlaggning/version").asInt()).isEqualTo(STUBBED_HANDLAGGNING_VERSION);
   }

   @Test
   @DisplayName("FROUL-FR-02.5: handleOulStatus ignorerar notifiering utan lagrad RegelCommonData")
   void status_should_be_ignored_when_no_regel_common_data()
   {
      UUID handlaggningId = UUID.randomUUID();
      OulStatus status = OulTestData.oulStatus(handlaggningId,
            UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID), NEW_STATUS,
            OulTestData.cloudEventData());

      oulUppgiftService.handleOulStatus(status);

      List<LoggedRequest> puts = WireMockRegelOul.getWireMockServer()
            .findAll(putRequestedFor(urlPathMatching("/handlaggning/.+")));
      assertThat(puts).isEmpty();
      List<LoggedRequest> ends = WireMockRegelOul.getWireMockServer()
            .findAll(postRequestedFor(urlPathMatching("/uppgifter/.+/end")));
      assertThat(ends).isEmpty();
      assertThat(receivedRegelResponses()).isEmpty();
   }
}
