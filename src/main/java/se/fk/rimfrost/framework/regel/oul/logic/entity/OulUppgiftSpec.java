package se.fk.rimfrost.framework.regel.oul.logic.entity;

import java.util.Map;
import java.util.UUID;
import org.immutables.value.Value;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.oul.model.Erbjudande;

/**
 * Consumer-facing input for creating an OUL uppgift via
 * {@code OulUppgiftService.createOulUppgift}.
 *
 * <p>The OUL reply subtopic is not carried on the spec; the framework
 * resolves it internally from the {@code kafka.subtopic} configuration
 * property.
 *
 * <p>{@link #cloudEventData()} (typed, used for correlation persistence)
 * and {@link #cloudEventAttributes()} (raw map, forwarded to OUL) are both
 * supplied by the caller; the framework does not derive one from the other.
 */
@Value.Immutable
public interface OulUppgiftSpec
{
   /** Handläggning the uppgift belongs to. */
   UUID handlaggningId();

   /**
    * The current {@link Handlaggning} the uppgift is being added to. Supplied
    * by the consumer (which typically has already read it for its own logic —
    * e.g. yrkande/erbjudande resolution) to avoid a redundant round-trip to
    * the handläggning-adapter.
    */
   Handlaggning handlaggning();

   /** Kafka topic the eventual regel-response will be sent back on. */
   String replyTo();

   /** Typed CloudEvent metadata persisted for later correlation. */
   CloudEventData cloudEventData();

   /** Raw CloudEvent attributes forwarded to OUL as-is. */
   Map<String, String> cloudEventAttributes();

   /** Short label describing which regel produced the uppgift. */
   String regel();

   /** Human-readable description shown in the handläggare's inbox. */
   String beskrivning();

   /** Business-logic identifier used by OUL routing / filtering. */
   String verksamhetslogik();

   /** Role required to work with the uppgift. */
   String roll();

   /** URL the handläggare navigates to in order to work with the uppgift. */
   String url();

   /** Erbjudande the uppgift is associated with. */
   Erbjudande erbjudande();

   /** Aktivitet the uppgift belongs to (identifier from the handläggning model). */
   UUID aktivitetId();

   /** ID of the {@code UppgiftSpecifikation} the uppgift references. */
   UUID uppgiftSpecifikationId();

   /** Version of the {@code UppgiftSpecifikation} the uppgift references. */
   Integer uppgiftSpecifikationVersion();

   /** OUL request business version. Defaults to {@code "1"}. */
   @Value.Default
   default String version()
   {
      return "1";
   }
}
