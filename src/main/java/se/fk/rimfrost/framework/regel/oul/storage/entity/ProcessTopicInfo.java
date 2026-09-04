package se.fk.rimfrost.framework.regel.oul.storage.entity;

import org.immutables.value.Value;

/**
 * Correlation record that carries the Kafka reply topic for a given handläggning.
 *
 * <p>Stored by {@code OulUppgiftService} when an OUL uppgift is created and used
 * during status callbacks to route the eventual regel response to the correct topic.
 */
@Value.Immutable
public interface ProcessTopicInfo
{
   String replyTopic();
}
