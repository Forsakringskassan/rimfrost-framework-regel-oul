package se.fk.rimfrost.framework.regel.oul.logic.entity;

import java.util.UUID;
import org.immutables.value.Value;

/**
 * Typed representation of the CloudEvent metadata carried on an incoming regel request.
 *
 * <p>Persisted by {@code OulUppgiftService} for later correlation and forwarded to OUL
 * as a flat attribute map via {@link se.fk.rimfrost.framework.regel.oul.logic.CloudEventAttributesMapper}.
 */
@Value.Immutable
public interface CloudEventData
{

   UUID id();

   UUID kogitorootprociid();

   UUID kogitoparentprociid();

   UUID kogitoprocinstanceid();

   String kogitorootprocid();

   String kogitoprocid();

   String kogitoprocist();

   String kogitoprocversion();

   String type();

   String source();
}
