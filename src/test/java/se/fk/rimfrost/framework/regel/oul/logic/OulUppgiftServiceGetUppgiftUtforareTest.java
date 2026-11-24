package se.fk.rimfrost.framework.regel.oul.logic;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.exception.OulException;
import se.fk.rimfrost.framework.oul.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.regel.oul.base.OulUppgiftServiceTestBase;
import se.fk.rimfrost.framework.regel.oul.logic.exception.OulServiceException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link OulUppgiftService#getOulUppgiftUtforare(UUID)} covering FROUL-FR-01.16.
 */
@QuarkusTest
class OulUppgiftServiceGetUppgiftUtforareTest extends OulUppgiftServiceTestBase
{
   @Inject
   OulUppgiftService oulUppgiftService;

   @InjectMock
   OulAdapter oulAdapter;

   @Test
   void getUppgiftUtforare_should_throw_OulServiceException_on_adapter_exception() throws OulException
   {
      var uppgiftId = UUID.randomUUID();
      var adapterException = new OulException(OulException.ErrorType.UNEXPECTED_ERROR, "error message");

      Mockito.when(oulAdapter.getOperativUppgiftUtforare(uppgiftId)).thenThrow(adapterException);

      var exception = assertThrows(OulServiceException.class, () -> oulUppgiftService.getOulUppgiftUtforare(uppgiftId));
      assertEquals(OulServiceException.ErrorType.UNEXPECTED_ERROR, exception.getErrorType());
      assertEquals("error message", exception.getMessage());
      assertEquals(adapterException, exception.getCause());
   }

   @Test
   void getUppgiftUtforare_should_return_optional_with_Idtyp_when_utforare_is_set() throws OulException, OulServiceException
   {
      var uppgiftId = UUID.randomUUID();
      var idtyp = ImmutableIdtyp.builder().typId(UUID.randomUUID().toString()).varde(UUID.randomUUID().toString()).build();

      Mockito.when(oulAdapter.getOperativUppgiftUtforare(uppgiftId)).thenReturn(Optional.of(idtyp));

      var response = oulUppgiftService.getOulUppgiftUtforare(uppgiftId);

      assertTrue(response.isPresent());
      assertEquals(idtyp.typId(), response.get().typId());
      assertEquals(idtyp.varde(), response.get().varde());
   }

   @Test
   void getUppgiftUtforare_should_return_empty_optional_when_utforare_not_set() throws OulException, OulServiceException
   {
      var uppgiftId = UUID.randomUUID();

      Mockito.when(oulAdapter.getOperativUppgiftUtforare(uppgiftId)).thenReturn(Optional.empty());

      var response = oulUppgiftService.getOulUppgiftUtforare(uppgiftId);

      assertTrue(response.isEmpty());
   }
}
