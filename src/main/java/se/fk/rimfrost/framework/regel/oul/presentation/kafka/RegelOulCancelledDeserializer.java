package se.fk.rimfrost.framework.regel.oul.presentation.kafka;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import se.fk.rimfrost.framework.regel.oul.RegelOulCancelledMessagePayload;

/**
 * Kafka deserializer for {@link RegelOulCancelledMessagePayload}, the generated CloudEvent
 * envelope for cancelled-events published to {@code kafka.cancelled.topic}.
 */
@SuppressWarnings("unused")
public class RegelOulCancelledDeserializer extends ObjectMapperDeserializer<RegelOulCancelledMessagePayload>
{

   /** Creates a deserializer bound to {@link RegelOulCancelledMessagePayload}. */
   public RegelOulCancelledDeserializer()
   {
      super(RegelOulCancelledMessagePayload.class);
   }

}
