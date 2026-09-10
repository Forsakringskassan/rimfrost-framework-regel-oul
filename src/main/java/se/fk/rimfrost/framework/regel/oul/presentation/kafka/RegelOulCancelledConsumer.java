package se.fk.rimfrost.framework.regel.oul.presentation.kafka;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import se.fk.rimfrost.framework.regel.oul.RegelOulCancelledMessagePayload;
import se.fk.rimfrost.framework.regel.oul.logic.RegelOulCancelledService;

/**
 * Kafka consumer for cancelled-events published to the topic configured by
 * {@code kafka.cancelled.topic} (FROUL-FR-04.1).
 *
 * <p>Each message signals that the associated regel flow will not complete normally (e.g. a BPMN
 * timeout). Delegates immediately to {@link RegelOulCancelledService} for best-effort cleanup.
 */
@SuppressWarnings("unused")
@ApplicationScoped
public class RegelOulCancelledConsumer
{

   @Inject
   RegelOulCancelledService cancelledService;

   /**
    * Processes a cancelled-event received on the {@code regel-oul-cancelled} channel.
    *
    * @param message the CloudEvent envelope carrying the {@code handlaggningId} of the
    *                cancelled regel flow
    */
   @Incoming("regel-oul-cancelled")
   @Blocking
   public void onCancelled(RegelOulCancelledMessagePayload message)
   {
      cancelledService.handleCancelled(UUID.fromString(message.getData().getHandlaggningId()));
   }

}
