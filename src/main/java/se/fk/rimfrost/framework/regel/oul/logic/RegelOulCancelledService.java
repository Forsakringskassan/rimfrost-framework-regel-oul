package se.fk.rimfrost.framework.regel.oul.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;

/**
 * Handles cancelled-events received from the {@code kafka.cancelled.topic} Kafka topic.
 *
 * <p>On receipt, the service performs best-effort cleanup in this order (FROUL-FR-04):
 * <ol>
 *   <li>Calls {@link RegelOulCancelledHandler#handleCancelled(UUID)} if a rule-specific
 *       handler is present on the classpath (FROUL-FR-04.5). Errors are caught and logged
 *       without aborting the remaining cleanup (FROUL-FR-04.6).</li>
 *   <li>Best-effort ends the OUL uppgift if one exists (FROUL-FR-04.3).</li>
 *   <li>Clears all stored correlation data (FROUL-FR-04.4).</li>
 * </ol>
 *
 * <p>If no correlation data is found for the given handläggning, the event is silently
 * ignored (FROUL-FR-04.7).
 */
@ApplicationScoped
public class RegelOulCancelledService
{

   private static final Logger LOGGER = LoggerFactory.getLogger(RegelOulCancelledService.class);

   @Inject
   OulUppgiftService oulUppgiftService;

   @Any
   @Inject
   Instance<RegelOulCancelledHandler> cancelledHandler;

   /**
    * Processes a cancelled-event for the given handläggning.
    *
    * @param handlaggningId the handläggning whose regel data is to be cleaned up
    */
   public void handleCancelled(UUID handlaggningId)
   {
      OulCorrelationData correlationData = oulUppgiftService.getCorrelationData(handlaggningId);
      if (correlationData == null)
      {
         // FROUL-FR-04.7 — no correlation state, ignore
         LOGGER.info("Cancelled-event received for handlaggningId {} but no correlation data found — ignoring",
               handlaggningId);
         return;
      }

      // FROUL-FR-04.5 — rule-specific cleanup first (optional)
      if (!cancelledHandler.isUnsatisfied())
      {
         try
         {
            cancelledHandler.get().handleCancelled(handlaggningId);
         }
         catch (Exception e)
         {
            // FROUL-FR-04.6 — best-effort, log and continue
            LOGGER.error("RegelOulCancelledHandler failed for handlaggningId {} — continuing with framework cleanup",
                  handlaggningId, e);
         }
      }

      // FROUL-FR-04.3 — best-effort end OUL task
      if (correlationData.oulUppgiftId() != null)
      {
         try
         {
            oulUppgiftService.tryEndOulUppgift(correlationData.oulUppgiftId(), "BPMN_CANCELLED");
         }
         catch (Exception e)
         {
            LOGGER.error("tryEndOulUppgift failed for handlaggningId {} — continuing with cleanup",
                  handlaggningId, e);
         }
      }

      // FROUL-FR-04.4 — clean up all correlation data
      oulUppgiftService.cleanupCorrelation(handlaggningId);
   }

}
