package se.fk.rimfrost.framework.regel.oul.logic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.regel.oul.helpers.OulTestData;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;

/**
 * Fault tests for {@link OulUppgiftService#updateUppgift(UUID, Uppgift)} verifying
 * that {@link HandlaggningException} propagates to the caller (FROUL-FR-01.15).
 */
@QuarkusTest
class OulUppgiftServiceUpdateUppgiftFaultTest
{
   @Inject
   OulUppgiftService oulUppgiftService;

   @Inject
   RegelCommonDataStorage regelCommonDataStorage;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

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
      Uppgift uppgift = ImmutableUppgift.builder()
            .id(UUID.randomUUID())
            .version(1)
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
            .oulUppgiftId(UUID.randomUUID())
            .build());

      when(handlaggningAdapter.readHandlaggning(handlaggningId))
            .thenReturn(OulTestData.handlaggning(handlaggningId, 1));
      HandlaggningException failure = new HandlaggningException(
            HandlaggningException.ErrorType.CONFLICT, "conflict");
      doThrow(failure).when(handlaggningAdapter).updateHandlaggning(any());

      assertThatThrownBy(() -> oulUppgiftService.updateUppgift(handlaggningId, OulTestData.seedUppgift()))
            .isInstanceOf(HandlaggningException.class)
            .isSameAs(failure);
   }
}
