package se.fk.rimfrost.framework.regel.oul.logic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import jakarta.enterprise.inject.Instance;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;

/**
 * Unit tests for {@link RegelOulCancelledService} covering all FROUL-FR-04 cleanup scenarios.
 *
 * <p><b>Why plain Mockito rather than {@code @QuarkusTest} or {@code @QuarkusComponentTest}:</b>
 * {@code RegelOulCancelledService} injects an optional {@code Instance<RegelOulCancelledHandler>}
 * and branches on {@link Instance#isUnsatisfied()} at runtime. Both {@code @QuarkusTest} and
 * {@code @QuarkusComponentTest} share a single CDI container across test classes in the same run;
 * registering a mock {@code RegelOulCancelledHandler} bean in one class leaks into all others,
 * making it impossible to test the "handler absent" path in a sibling class. Using two classes with
 * separate Quarkus profiles would work but adds significant boilerplate for no gain. Plain Mockito
 * lets {@link #cancelledHandler} stub {@code isUnsatisfied()} independently per scenario, which is
 * the right tool when the object under test is a pure-logic bean with no direct DB or HTTP calls.
 */
@ExtendWith(MockitoExtension.class)
class RegelOulCancelledServiceTest
{

   private static final UUID HANDLAGGNING_ID = UUID.randomUUID();
   private static final UUID OUL_UPPGIFT_ID = UUID.randomUUID();

   @Mock
   OulUppgiftService oulUppgiftService;

   @Mock
   Instance<RegelOulCancelledHandler> cancelledHandler;

   @Mock
   RegelOulCancelledHandler handler;

   @InjectMocks
   RegelOulCancelledService service;

   @Test
   @DisplayName("FROUL-FR-04.7: No correlation data — event ignored, OUL not called, cleanup not called")
   void handleCancelled_noCorrelationData_ignoresEvent()
   {
      when(oulUppgiftService.getCorrelationData(HANDLAGGNING_ID)).thenReturn(null);

      service.handleCancelled(HANDLAGGNING_ID);

      verify(oulUppgiftService).getCorrelationData(HANDLAGGNING_ID);
      verifyNoMoreInteractions(oulUppgiftService);
   }

   @Test
   @DisplayName("FROUL-FR-04.3, FROUL-FR-04.4: Correlation data present, no handler — OUL uppgift ended, correlation cleaned up")
   void handleCancelled_correlationDataPresent_noHandler_endsOulAndCleansUp()
   {
      when(cancelledHandler.isUnsatisfied()).thenReturn(true);
      OulCorrelationData correlationData = correlationDataWith(OUL_UPPGIFT_ID);
      when(oulUppgiftService.getCorrelationData(HANDLAGGNING_ID)).thenReturn(correlationData);

      service.handleCancelled(HANDLAGGNING_ID);

      verify(oulUppgiftService).tryEndOulUppgift(OUL_UPPGIFT_ID, "BPMN_CANCELLED");
      verify(oulUppgiftService).cleanupCorrelation(HANDLAGGNING_ID);
   }

   @Test
   @DisplayName("FROUL-FR-04.5: Handler present — handler called first, then OUL end, then cleanup")
   void handleCancelled_handlerPresent_handlerCalledBeforeFrameworkCleanup()
   {
      when(cancelledHandler.isUnsatisfied()).thenReturn(false);
      when(cancelledHandler.get()).thenReturn(handler);
      OulCorrelationData correlationData = correlationDataWith(OUL_UPPGIFT_ID);
      when(oulUppgiftService.getCorrelationData(HANDLAGGNING_ID)).thenReturn(correlationData);

      service.handleCancelled(HANDLAGGNING_ID);

      InOrder order = inOrder(handler, oulUppgiftService);
      order.verify(handler).handleCancelled(HANDLAGGNING_ID);
      order.verify(oulUppgiftService).tryEndOulUppgift(OUL_UPPGIFT_ID, "BPMN_CANCELLED");
      order.verify(oulUppgiftService).cleanupCorrelation(HANDLAGGNING_ID);
   }

   @Test
   @DisplayName("FROUL-FR-04.6: Handler throws — OUL end and cleanup still run")
   void handleCancelled_handlerThrows_frameworkCleanupStillRuns()
   {
      when(cancelledHandler.isUnsatisfied()).thenReturn(false);
      when(cancelledHandler.get()).thenReturn(handler);
      doThrow(new RuntimeException("handler boom")).when(handler).handleCancelled(any());
      OulCorrelationData correlationData = correlationDataWith(OUL_UPPGIFT_ID);
      when(oulUppgiftService.getCorrelationData(HANDLAGGNING_ID)).thenReturn(correlationData);

      service.handleCancelled(HANDLAGGNING_ID);

      verify(oulUppgiftService).tryEndOulUppgift(OUL_UPPGIFT_ID, "BPMN_CANCELLED");
      verify(oulUppgiftService).cleanupCorrelation(HANDLAGGNING_ID);
   }

   @Test
   @DisplayName("FROUL-FR-04.3: tryEndOulUppgift throws — cleanup still runs")
   void handleCancelled_tryEndThrows_cleanupStillRuns()
   {
      when(cancelledHandler.isUnsatisfied()).thenReturn(true);
      OulCorrelationData correlationData = correlationDataWith(OUL_UPPGIFT_ID);
      when(oulUppgiftService.getCorrelationData(HANDLAGGNING_ID)).thenReturn(correlationData);
      doThrow(new RuntimeException("end boom")).when(oulUppgiftService).tryEndOulUppgift(any(), any());

      service.handleCancelled(HANDLAGGNING_ID);

      verify(oulUppgiftService).cleanupCorrelation(HANDLAGGNING_ID);
   }

   @Test
   @DisplayName("FROUL-FR-04.3: No OUL uppgift ID on correlation data — OUL end skipped, cleanup still runs")
   void handleCancelled_noOulUppgiftId_skipsEndButCleansUp()
   {
      when(cancelledHandler.isUnsatisfied()).thenReturn(true);
      OulCorrelationData correlationData = correlationDataWith(null);
      when(oulUppgiftService.getCorrelationData(HANDLAGGNING_ID)).thenReturn(correlationData);

      service.handleCancelled(HANDLAGGNING_ID);

      verify(oulUppgiftService, never()).tryEndOulUppgift(any(), any());
      verify(oulUppgiftService).cleanupCorrelation(HANDLAGGNING_ID);
   }

   private static OulCorrelationData correlationDataWith(UUID oulUppgiftId)
   {
      OulCorrelationData data = mock(OulCorrelationData.class);
      when(data.oulUppgiftId()).thenReturn(oulUppgiftId);
      return data;
   }

}
