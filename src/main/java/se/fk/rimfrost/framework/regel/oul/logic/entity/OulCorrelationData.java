package se.fk.rimfrost.framework.regel.oul.logic.entity;

import java.util.UUID;
import org.immutables.value.Value;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;

/**
 * Bundles the three persistent correlation rows written by
 * {@code OulUppgiftService#createOulUppgift} into a single value type.
 *
 * <p>Consumers that need correlation data during their done-flow can read this
 * via {@code OulUppgiftService#getCorrelationData} instead of injecting the
 * three storage interfaces directly.
 */
@Value.Immutable
public interface OulCorrelationData
{
   /** OUL's own uppgift identifier, from {@code RegelCommonData}. */
   UUID oulUppgiftId();

   /** The handläggning uppgift snapshot, from {@code RegelCommonData}. */
   Uppgift uppgift();

   /** The Kafka reply topic, from {@code ProcessTopicInfo}. */
   String replyTopic();

   /** The CloudEvent envelope from the originating regel request. */
   CloudEventData cloudEventData();
}
