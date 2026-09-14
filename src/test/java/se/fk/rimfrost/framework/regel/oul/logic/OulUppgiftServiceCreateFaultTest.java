package se.fk.rimfrost.framework.regel.oul.logic;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.exception.OulException;
import se.fk.rimfrost.framework.regel.oul.helpers.OulTestData;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulUppgiftSpec;
import se.fk.rimfrost.framework.regel.oul.logic.exception.OulServiceException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class OulUppgiftServiceCreateFaultTest
{
   @Inject
   OulUppgiftService oulUppgiftService;

   @InjectMock
   OulAdapter oulAdapter;

   @Test
   @DisplayName("FROUL-FR-01.14: createOulUppgift kastar OulServiceException vid fel")
   void createOulUppgift_should_throw_OulServiceException() throws OulException
   {
      UUID handlaggningId = UUID.randomUUID();
      OulException failure = new OulException(OulException.ErrorType.UNEXPECTED_ERROR, "boom");
      doThrow(failure).when(oulAdapter).createOperativUppgift(any());

      Handlaggning handlaggning = OulTestData.handlaggning(handlaggningId, 1);
      OulUppgiftSpec spec = OulTestData.oulUppgiftSpec(handlaggning, OulTestData.cloudEventData());

      var exception = assertThrows(OulServiceException.class, () -> oulUppgiftService.createOulUppgift(spec));
      assertEquals(OulServiceException.ErrorType.UNEXPECTED_ERROR, exception.getErrorType());
      assertEquals(failure.getMessage(), exception.getMessage());
      assertEquals(failure, exception.getCause());

      verify(oulAdapter).createOperativUppgift(any());
   }
}
