package se.fk.rimfrost.framework.regel.oul.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.exception.OulException;
import se.fk.rimfrost.framework.regel.oul.base.OulUppgiftServiceTestBase;
import se.fk.rimfrost.framework.regel.oul.helpers.OulTestData;
import se.fk.rimfrost.framework.regel.oul.helpers.WireMockRegelOul;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.oul.storage.CloudEventDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.ProcessTopicInfoStorage;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableProcessTopicInfo;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Tests for {@link OulUppgiftService#tryEndOulUppgift(UUID, String)} and
 * {@link OulUppgiftService#cleanupCorrelation(UUID)} covering the happy paths
 * of FROUL-FR-01.9 (end OUL uppgift + swallow OulException) and FROUL-FR-03.5
 * (delete all three correlation stores).
 *
 * <p>Uses {@code @InjectMock} on {@link OulAdapter} so the end-call assertions
 * can be made directly with Mockito rather than parsing WireMock request
 * bodies. Real storage beans are used for the cleanup test so pre-seeding and
 * post-cleanup assertions exercise the actual JPA layer.
 *
 * <p>The FROUL-FR-03.6 partial-failure scenario lives in
 * {@code OulUppgiftServiceCleanupResilienceTest} — it requires an
 * {@code @InjectMock} on a specific storage bean, which cannot coexist with
 * the seed-and-assert-empty pattern used here.
 */
@QuarkusTest
@QuarkusTestResource(WireMockRegelOul.class)
class OulUppgiftServiceEndAndCleanupTest extends OulUppgiftServiceTestBase
{
   private static final String END_REASON = "Test end reason";

   @Inject
   OulUppgiftService oulUppgiftService;

   @InjectMock
   OulAdapter oulAdapter;

   @Inject
   CloudEventDataStorage cloudEventDataStorage;

   @Inject
   ProcessTopicInfoStorage processTopicInfoStorage;

   @Inject
   RegelCommonDataStorage regelCommonDataStorage;

   @Test
   @DisplayName("FROUL-FR-01.9: tryEndOulUppgift anropar OUL endOperativUppgift med angiven orsak")
   void tryEndOulUppgift_should_call_oul_endOperativUppgift_with_reason() throws OulException
   {
      UUID uppgiftId = UUID.randomUUID();

      oulUppgiftService.tryEndOulUppgift(uppgiftId, END_REASON);

      verify(oulAdapter).endOperativUppgift(eq(uppgiftId), eq(END_REASON));
   }

   @Test
   @DisplayName("FROUL-FR-01.9: tryEndOulUppgift sväljer OulException och kastar inte vidare")
   void tryEndOulUppgift_should_swallow_OulException() throws OulException
   {
      UUID uppgiftId = UUID.randomUUID();
      doThrow(new OulException(OulException.ErrorType.UNEXPECTED_ERROR, "boom"))
            .when(oulAdapter).endOperativUppgift(any(), any());

      assertThatCode(() -> oulUppgiftService.tryEndOulUppgift(uppgiftId, END_REASON))
            .doesNotThrowAnyException();
      verify(oulAdapter).endOperativUppgift(eq(uppgiftId), eq(END_REASON));
   }

   @Test
   @DisplayName("FROUL-FR-01.10: endOulUppgift kastar OulException vidare vid fel")
   void endOulUppgift_should_propagate_OulException() throws OulException
   {
      UUID uppgiftId = UUID.randomUUID();
      OulException failure = new OulException(OulException.ErrorType.UNEXPECTED_ERROR, "boom");
      doThrow(failure).when(oulAdapter).endOperativUppgift(any(), any());

      assertThatThrownBy(() -> oulUppgiftService.endOulUppgift(uppgiftId, END_REASON))
            .isSameAs(failure);
      verify(oulAdapter).endOperativUppgift(eq(uppgiftId), eq(END_REASON));
   }

   @Test
   @DisplayName("FROUL-FR-03.5: cleanupCorrelation raderar samtliga tre korrelationslager")
   void cleanupCorrelation_should_delete_all_three_stores()
   {
      UUID handlaggningId = UUID.randomUUID();
      CloudEventData cloudEventData = OulTestData.cloudEventData();
      cloudEventDataStorage.setCloudEventData(handlaggningId, cloudEventData);
      processTopicInfoStorage.setProcessTopicInfo(handlaggningId,
            ImmutableProcessTopicInfo.builder().replyTopic(OulTestData.DEFAULT_REPLY_TOPIC).build());
      RegelCommonData regelCommonData = ImmutableRegelCommonData.builder()
            .uppgift(OulTestData.seedUppgift())
            .oulUppgiftId(UUID.fromString(WireMockRegelOul.DEFAULT_UPPGIFT_ID))
            .build();
      regelCommonDataStorage.setRegelCommonData(handlaggningId, regelCommonData);

      assertThat(cloudEventDataStorage.getCloudEventData(handlaggningId)).isNotNull();
      assertThat(processTopicInfoStorage.getProcessTopicInfo(handlaggningId)).isNotNull();
      assertThat(regelCommonDataStorage.getRegelCommonData(handlaggningId)).isNotNull();

      oulUppgiftService.cleanupCorrelation(handlaggningId);

      assertThat(cloudEventDataStorage.getCloudEventData(handlaggningId)).isNull();
      assertThat(processTopicInfoStorage.getProcessTopicInfo(handlaggningId)).isNull();
      assertThat(regelCommonDataStorage.getRegelCommonData(handlaggningId)).isNull();
   }
}
