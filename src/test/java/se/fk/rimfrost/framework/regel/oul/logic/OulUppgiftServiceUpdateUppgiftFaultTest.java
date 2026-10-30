package se.fk.rimfrost.framework.regel.oul.logic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.regel.oul.helpers.OulTestData;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Fault tests for {@link OulUppgiftService#updateUppgift(UUID, Uppgift)} verifying
 * that exceptions from the handläggning adapter and correlation storage propagate
 * to the caller (FROUL-FR-01.15).
 */
@QuarkusTest
class OulUppgiftServiceUpdateUppgiftFaultTest
{
   @Inject
   OulUppgiftService oulUppgiftService;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   RegelCommonDataStorage regelCommonDataStorage;

   @Test
   @DisplayName("FROUL-FR-01.15: updateUppgift propagerar HandlaggningException vid fel vid läsning av handläggning")
   void updateUppgift_should_propagate_exception_when_read_handlaggning_fails() throws HandlaggningException
   {
      UUID handlaggningId = UUID.randomUUID();
      HandlaggningException failure = new HandlaggningException(
            HandlaggningException.ErrorType.SERVICE_UNAVAILABLE, "service unavailable");
      doThrow(failure).when(handlaggningAdapter).readHandlaggning(handlaggningId);

      assertThatThrownBy(() -> oulUppgiftService.updateUppgift(handlaggningId, OulTestData.seedUppgift()))
            .isInstanceOf(HandlaggningException.class)
            .isSameAs(failure);
   }

   @Test
   @DisplayName("FROUL-FR-01.15: updateUppgift propagerar HandlaggningException vid fel vid uppdatering av handläggning")
   void updateUppgift_should_propagate_exception_when_update_handlaggning_fails() throws HandlaggningException
   {
      UUID handlaggningId = UUID.randomUUID();
      when(handlaggningAdapter.readHandlaggning(handlaggningId))
            .thenReturn(OulTestData.handlaggning(handlaggningId, 1));
      when(regelCommonDataStorage.getRegelCommonData(handlaggningId))
            .thenReturn(seedCommonData());
      HandlaggningException failure = new HandlaggningException(
            HandlaggningException.ErrorType.CONFLICT, "conflict");
      doThrow(failure).when(handlaggningAdapter).updateHandlaggning(any());

      assertThatThrownBy(() -> oulUppgiftService.updateUppgift(handlaggningId, OulTestData.seedUppgift()))
            .isInstanceOf(HandlaggningException.class)
            .isSameAs(failure);
   }

   @Test
   @DisplayName("FROUL-FR-01.15: updateUppgift propagerar RuntimeException vid fel vid läsning av RegelCommonData")
   void updateUppgift_should_propagate_exception_when_read_common_data_fails() throws HandlaggningException
   {
      UUID handlaggningId = UUID.randomUUID();
      when(handlaggningAdapter.readHandlaggning(handlaggningId))
            .thenReturn(OulTestData.handlaggning(handlaggningId, 1));
      RuntimeException failure = new RuntimeException("DB read failure");
      doThrow(failure).when(regelCommonDataStorage).getRegelCommonData(handlaggningId);

      assertThatThrownBy(() -> oulUppgiftService.updateUppgift(handlaggningId, OulTestData.seedUppgift()))
            .isInstanceOf(RuntimeException.class)
            .isSameAs(failure);
   }

   @Test
   @DisplayName("FROUL-FR-01.15: updateUppgift propagerar RuntimeException vid fel vid skrivning av RegelCommonData")
   void updateUppgift_should_propagate_exception_when_write_common_data_fails() throws HandlaggningException
   {
      UUID handlaggningId = UUID.randomUUID();
      when(handlaggningAdapter.readHandlaggning(handlaggningId))
            .thenReturn(OulTestData.handlaggning(handlaggningId, 1));
      when(regelCommonDataStorage.getRegelCommonData(handlaggningId))
            .thenReturn(seedCommonData());
      RuntimeException failure = new RuntimeException("DB write failure");
      doThrow(failure).when(regelCommonDataStorage).setRegelCommonData(any(), any());

      assertThatThrownBy(() -> oulUppgiftService.updateUppgift(handlaggningId, OulTestData.seedUppgift()))
            .isInstanceOf(RuntimeException.class)
            .isSameAs(failure);
   }

   private static RegelCommonData seedCommonData()
   {
      return ImmutableRegelCommonData.builder()
            .uppgift(OulTestData.seedUppgift())
            .oulUppgiftId(UUID.randomUUID())
            .build();
   }
}
