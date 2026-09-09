package se.fk.rimfrost.framework.regel.oul.logic;

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
import se.fk.rimfrost.framework.regel.oul.helpers.WireMockRegelOul;

/**
 * Tests for {@link OulUppgiftService#tryUnassignOulUppgift(UUID)} and
 * {@link OulUppgiftService#unassignOulUppgift(UUID)} covering FROUL-FR-01.12
 * (best-effort unassign, swallows OulException) and FROUL-FR-01.13 (propagates
 * OulException to caller).
 */
@QuarkusTest
@QuarkusTestResource(WireMockRegelOul.class)
class OulUppgiftServiceUnassignTest extends OulUppgiftServiceTestBase
{
   @Inject
   OulUppgiftService oulUppgiftService;

   @InjectMock
   OulAdapter oulAdapter;

   @Test
   @DisplayName("FROUL-FR-01.12: tryUnassignOulUppgift anropar OUL unassignOperativUppgift")
   void tryUnassignOulUppgift_should_call_oul_unassignOperativUppgift() throws OulException
   {
      UUID uppgiftId = UUID.randomUUID();

      oulUppgiftService.tryUnassignOulUppgift(uppgiftId);

      verify(oulAdapter).unassignOperativUppgift(eq(uppgiftId));
   }

   @Test
   @DisplayName("FROUL-FR-01.12: tryUnassignOulUppgift sväljer OulException och kastar inte vidare")
   void tryUnassignOulUppgift_should_swallow_OulException() throws OulException
   {
      UUID uppgiftId = UUID.randomUUID();
      doThrow(new OulException(OulException.ErrorType.UNEXPECTED_ERROR, "boom"))
            .when(oulAdapter).unassignOperativUppgift(any());

      assertThatCode(() -> oulUppgiftService.tryUnassignOulUppgift(uppgiftId))
            .doesNotThrowAnyException();
      verify(oulAdapter).unassignOperativUppgift(eq(uppgiftId));
   }

   @Test
   @DisplayName("FROUL-FR-01.13: unassignOulUppgift anropar OUL unassignOperativUppgift")
   void unassignOulUppgift_should_call_oul_unassignOperativUppgift() throws OulException
   {
      UUID uppgiftId = UUID.randomUUID();

      oulUppgiftService.unassignOulUppgift(uppgiftId);

      verify(oulAdapter).unassignOperativUppgift(eq(uppgiftId));
   }

   @Test
   @DisplayName("FROUL-FR-01.13: unassignOulUppgift kastar OulException vidare vid fel")
   void unassignOulUppgift_should_propagate_OulException() throws OulException
   {
      UUID uppgiftId = UUID.randomUUID();
      OulException failure = new OulException(OulException.ErrorType.UNEXPECTED_ERROR, "boom");
      doThrow(failure).when(oulAdapter).unassignOperativUppgift(any());

      assertThatThrownBy(() -> oulUppgiftService.unassignOulUppgift(uppgiftId))
            .isSameAs(failure);
      verify(oulAdapter).unassignOperativUppgift(eq(uppgiftId));
   }
}
