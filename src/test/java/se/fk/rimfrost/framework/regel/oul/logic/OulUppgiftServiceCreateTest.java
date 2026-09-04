package se.fk.rimfrost.framework.regel.oul.logic;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.oul.model.OperativUppgift;
import se.fk.rimfrost.framework.regel.oul.base.OulUppgiftServiceTestBase;
import se.fk.rimfrost.framework.regel.oul.helpers.OulTestData;
import se.fk.rimfrost.framework.regel.oul.helpers.WireMockRegelOul;
import se.fk.rimfrost.framework.regel.oul.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulUppgiftSpec;
import se.fk.rimfrost.framework.regel.oul.storage.CloudEventDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.ProcessTopicInfoStorage;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ProcessTopicInfo;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Tests for {@link OulUppgiftService#createOulUppgift} covering the OUL
 * request shape (FROUL-FR-01.1..1.8) and the correlation persistence side
 * effects (FROUL-FR-03.1..3.4).
 *
 * <p>Uses WireMock (via {@link WireMockRegelOul}) to capture and inspect the
 * outbound OUL and handläggning requests, and reads the three correlation
 * stores directly to assert persistence.
 */
@QuarkusTest
@QuarkusTestResource(WireMockRegelOul.class)
class OulUppgiftServiceCreateTest extends OulUppgiftServiceTestBase
{
   private static final ObjectMapper MAPPER = new ObjectMapper();

   @Inject
   OulUppgiftService oulUppgiftService;

   @Inject
   CloudEventDataStorage cloudEventDataStorage;

   @Inject
   ProcessTopicInfoStorage processTopicInfoStorage;

   @Inject
   RegelCommonDataStorage regelCommonDataStorage;

   /**
    * Builds a default handläggning + cloud event + spec, then calls
    * {@code createOulUppgift}. Individual tests use this to trigger the flow
    * and then assert on captured requests or storage state.
    *
    * @param handlaggningId the id to use for the handläggning and OUL uppgift
    * @param cloudEventData the cloud event metadata to attach to the spec
    * @return the {@link OperativUppgift} returned by the service
    * @throws Exception if the OUL adapter throws
    */
   private OperativUppgift createDefault(UUID handlaggningId, CloudEventData cloudEventData) throws Exception
   {
      Handlaggning handlaggning = OulTestData.handlaggning(handlaggningId, 1);
      OulUppgiftSpec spec = OulTestData.oulUppgiftSpec(handlaggning, cloudEventData);
      return oulUppgiftService.createOulUppgift(spec);
   }

   /**
    * @return the JSON body of the most recent POST to {@code /uppgifter}
    * @throws Exception if the body cannot be parsed
    */
   private JsonNode lastOulCreateRequestBody() throws Exception
   {
      List<LoggedRequest> requests = WireMockRegelOul.getWireMockServer()
            .findAll(postRequestedFor(urlPathEqualTo("/uppgifter")));
      assertThat(requests).isNotEmpty();
      return MAPPER.readTree(requests.getLast().getBodyAsString());
   }

   @Test
   @DisplayName("FROUL-FR-01.1: createOulUppgift anropar OUL createOperativUppgift")
   void createOulUppgift_should_post_to_oul_uppgifter() throws Exception
   {
      UUID handlaggningId = UUID.randomUUID();
      OperativUppgift result = createDefault(handlaggningId, OulTestData.cloudEventData());

      assertThat(result.getUppgiftId()).isEqualTo(UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID));
      List<LoggedRequest> requests = WireMockRegelOul.getWireMockServer()
            .findAll(postRequestedFor(urlPathEqualTo("/uppgifter")));
      assertThat(requests).hasSize(1);
   }

   @Test
   @DisplayName("FROUL-FR-01.2: OUL-uppgift innehåller regel, beskrivning, verksamhetslogik och roll")
   void createOulUppgift_should_include_regel_beskrivning_verksamhet_roll() throws Exception
   {
      createDefault(UUID.randomUUID(), OulTestData.cloudEventData());

      JsonNode body = lastOulCreateRequestBody();
      assertThat(body.get("regel").asText()).isEqualTo("test-regel");
      assertThat(body.get("beskrivning").asText()).isEqualTo("Test uppgift");
      assertThat(body.get("verksamhetslogik").asText()).isEqualTo("test-verksamhet");
      assertThat(body.get("roll").asText()).isEqualTo("handlaggare");
   }

   @Test
   @DisplayName("FROUL-FR-01.3: OUL-uppgift innehåller URL till regelns REST-gränssnitt")
   void createOulUppgift_should_include_url() throws Exception
   {
      createDefault(UUID.randomUUID(), OulTestData.cloudEventData());

      JsonNode body = lastOulCreateRequestBody();
      assertThat(body.get("url").asText()).isEqualTo("/test/uppgift");
   }

   @Test
   @DisplayName("FROUL-FR-01.4: OUL-uppgift innehåller CloudEvent-attributen från regelförfrågan (processInfo)")
   void createOulUppgift_should_include_cloudevent_attributes_in_processInfo() throws Exception
   {
      CloudEventData ced = OulTestData.cloudEventData();
      createDefault(UUID.randomUUID(), ced);

      JsonNode processInfo = lastOulCreateRequestBody().get("process_info");
      assertThat(processInfo).isNotNull();
      JsonNode attrs = processInfo.get("cloudevent_attributes");
      assertThat(attrs).isNotNull();
      assertThat(attrs.get("id").asText()).isEqualTo(ced.id().toString());
      assertThat(attrs.get("type").asText()).isEqualTo(ced.type());
      assertThat(attrs.get("source").asText()).isEqualTo(ced.source());
   }

   @Test
   @DisplayName("FROUL-FR-01.5: OUL-uppgift anger reply-subtopic från kafka.subtopic-konfiguration")
   void createOulUppgift_should_include_configured_reply_subtopic() throws Exception
   {
      createDefault(UUID.randomUUID(), OulTestData.cloudEventData());

      JsonNode body = lastOulCreateRequestBody();
      assertThat(body.get("sub_topic").asText()).isEqualTo("oul-test");
   }

   @Test
   @DisplayName("FROUL-FR-01.6: Efter skapande lagras uppgift-ID och OUL-uppgift-ID i RegelCommonData")
   void createOulUppgift_should_persist_regel_common_data_with_uppgift_ids() throws Exception
   {
      UUID handlaggningId = UUID.randomUUID();
      createDefault(handlaggningId, OulTestData.cloudEventData());

      RegelCommonData stored = regelCommonDataStorage.getRegelCommonData(handlaggningId);
      assertThat(stored).isNotNull();
      assertThat(stored.oulUppgiftId()).isEqualTo(UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID));
      assertThat(stored.uppgift()).isNotNull();
      assertThat(stored.uppgift().id()).isNotNull();
   }

   @Test
   @DisplayName("FROUL-FR-01.7: Efter skapande uppdateras handläggning med uppgiftsreferens och specifikation")
   void createOulUppgift_should_put_handlaggning_with_uppgift_reference() throws Exception
   {
      UUID handlaggningId = UUID.randomUUID();
      createDefault(handlaggningId, OulTestData.cloudEventData());

      List<LoggedRequest> puts = WireMockRegelOul.waitForHandlaggningRequests(
            handlaggningId.toString(), RequestMethod.PUT, 1);
      assertThat(puts).hasSize(1);
      JsonNode body = MAPPER.readTree(puts.get(0).getBodyAsString());
      JsonNode uppgift = body.at("/handlaggning/uppgift");
      assertThat(uppgift.isMissingNode()).isFalse();
      assertThat(uppgift.get("id").asText()).isNotBlank();
      assertThat(uppgift.at("/uppgiftspecifikation/id").asText()).isNotBlank();
      assertThat(uppgift.at("/uppgiftspecifikation/version").asInt()).isEqualTo(1);
   }

   @Test
   @DisplayName("FROUL-FR-01.8: OUL-skapandeförfrågan innehåller inte individer")
   void createOulUppgift_should_not_include_individer_in_oul_request() throws Exception
   {
      createDefault(UUID.randomUUID(), OulTestData.cloudEventData());

      JsonNode body = lastOulCreateRequestBody();
      assertThat(body.has("individer")).isFalse();
   }

   @Test
   @DisplayName("FROUL-FR-03.1: CloudEvent-attributen lagras persistent för korrelation")
   void createOulUppgift_should_persist_cloud_event_data() throws Exception
   {
      UUID handlaggningId = UUID.randomUUID();
      CloudEventData ced = OulTestData.cloudEventData();
      createDefault(handlaggningId, ced);

      CloudEventData stored = cloudEventDataStorage.getCloudEventData(handlaggningId);
      assertThat(stored).isNotNull();
      assertThat(stored.id()).isEqualTo(ced.id());
      assertThat(stored.type()).isEqualTo(ced.type());
      assertThat(stored.source()).isEqualTo(ced.source());
      assertThat(stored.kogitoprocinstanceid()).isEqualTo(ced.kogitoprocinstanceid());
   }

   @Test
   @DisplayName("FROUL-FR-03.2 & FROUL-FR-03.3: replyTo lagras i ProcessTopicInfo per handläggning")
   void createOulUppgift_should_persist_reply_topic_in_process_topic_info() throws Exception
   {
      UUID handlaggningId = UUID.randomUUID();
      createDefault(handlaggningId, OulTestData.cloudEventData());

      ProcessTopicInfo stored = processTopicInfoStorage.getProcessTopicInfo(handlaggningId);
      assertThat(stored).isNotNull();
      assertThat(stored.replyTopic()).isEqualTo(OulTestData.DEFAULT_REPLY_TOPIC);
   }

   @Test
   @DisplayName("FROUL-FR-03.4: uppgifts-ID och OUL:s uppgifts-ID lagras persistent per handläggning")
   void createOulUppgift_should_persist_uppgift_and_oul_uppgift_id_in_common_data() throws Exception
   {
      UUID handlaggningId = UUID.randomUUID();
      createDefault(handlaggningId, OulTestData.cloudEventData());

      RegelCommonData stored = regelCommonDataStorage.getRegelCommonData(handlaggningId);
      assertThat(stored).isNotNull();
      assertThat(stored.uppgift().id()).isNotNull();
      assertThat(stored.oulUppgiftId()).isEqualTo(UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID));
   }
}
