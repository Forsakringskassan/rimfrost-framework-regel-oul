package se.fk.rimfrost.framework.regel.oul.logic;

import static org.assertj.core.api.Assertions.assertThat;
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
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import se.fk.rimfrost.framework.regel.oul.storage.CloudEventDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.ProcessTopicInfoStorage;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableProcessTopicInfo;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Tests for {@link OulUppgiftService#getCorrelationData(UUID)} covering the
 * happy path (all three rows present) and the three missing-row cases
 * (FROUL-FR-01.11).
 *
 * <p>Uses real storage beans backed by the Quarkus dev-services PostgreSQL
 * container. State is reset between tests by {@code resetState()} in the
 * base class.
 */
@QuarkusTest
@QuarkusTestResource(WireMockRegelOul.class)
class OulUppgiftServiceCorrelationReadTest extends OulUppgiftServiceTestBase
{
   @Inject
   OulUppgiftService oulUppgiftService;

   @Inject
   RegelCommonDataStorage regelCommonDataStorage;

   @Inject
   ProcessTopicInfoStorage processTopicInfoStorage;

   @Inject
   CloudEventDataStorage cloudEventDataStorage;

   @Test
   @DisplayName("FROUL-FR-01.11: getCorrelationData returnerar korrekt data när alla tre rader finns")
   void getCorrelationData_should_return_data_when_all_rows_present()
   {
      UUID handlaggningId = UUID.randomUUID();
      UUID oulUppgiftId = UUID.randomUUID();
      CloudEventData cloudEventData = OulTestData.cloudEventData();
      RegelCommonData commonData = ImmutableRegelCommonData.builder()
            .uppgift(OulTestData.seedUppgift())
            .oulUppgiftId(oulUppgiftId)
            .build();

      regelCommonDataStorage.setRegelCommonData(handlaggningId, commonData);
      processTopicInfoStorage.setProcessTopicInfo(handlaggningId,
            ImmutableProcessTopicInfo.builder().replyTopic(OulTestData.DEFAULT_REPLY_TOPIC).build());
      cloudEventDataStorage.setCloudEventData(handlaggningId, cloudEventData);

      OulCorrelationData result = oulUppgiftService.getCorrelationData(handlaggningId);

      assertThat(result).isNotNull();
      assertThat(result.oulUppgiftId()).isEqualTo(oulUppgiftId);
      assertThat(result.uppgift()).isEqualTo(commonData.uppgift());
      assertThat(result.replyTopic()).isEqualTo(OulTestData.DEFAULT_REPLY_TOPIC);
      assertThat(result.cloudEventData()).isEqualTo(cloudEventData);
   }

   @Test
   @DisplayName("FROUL-FR-01.11: getCorrelationData returnerar null när RegelCommonData saknas")
   void getCorrelationData_should_return_null_when_common_data_missing()
   {
      UUID handlaggningId = UUID.randomUUID();
      CloudEventData cloudEventData = OulTestData.cloudEventData();

      processTopicInfoStorage.setProcessTopicInfo(handlaggningId,
            ImmutableProcessTopicInfo.builder().replyTopic(OulTestData.DEFAULT_REPLY_TOPIC).build());
      cloudEventDataStorage.setCloudEventData(handlaggningId, cloudEventData);

      assertThat(oulUppgiftService.getCorrelationData(handlaggningId)).isNull();
   }

   @Test
   @DisplayName("FROUL-FR-01.11: getCorrelationData returnerar null när ProcessTopicInfo saknas")
   void getCorrelationData_should_return_null_when_process_topic_info_missing()
   {
      UUID handlaggningId = UUID.randomUUID();
      CloudEventData cloudEventData = OulTestData.cloudEventData();
      RegelCommonData commonData = ImmutableRegelCommonData.builder()
            .uppgift(OulTestData.seedUppgift())
            .oulUppgiftId(UUID.randomUUID())
            .build();

      regelCommonDataStorage.setRegelCommonData(handlaggningId, commonData);
      cloudEventDataStorage.setCloudEventData(handlaggningId, cloudEventData);

      assertThat(oulUppgiftService.getCorrelationData(handlaggningId)).isNull();
   }

   @Test
   @DisplayName("FROUL-FR-01.11: getCorrelationData returnerar null när CloudEventData saknas")
   void getCorrelationData_should_return_null_when_cloud_event_data_missing()
   {
      UUID handlaggningId = UUID.randomUUID();
      RegelCommonData commonData = ImmutableRegelCommonData.builder()
            .uppgift(OulTestData.seedUppgift())
            .oulUppgiftId(UUID.randomUUID())
            .build();

      regelCommonDataStorage.setRegelCommonData(handlaggningId, commonData);
      processTopicInfoStorage.setProcessTopicInfo(handlaggningId,
            ImmutableProcessTopicInfo.builder().replyTopic(OulTestData.DEFAULT_REPLY_TOPIC).build());

      assertThat(oulUppgiftService.getCorrelationData(handlaggningId)).isNull();
   }
}
