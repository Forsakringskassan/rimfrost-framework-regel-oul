package se.fk.rimfrost.framework.regel.oul.logic;

import java.util.Map;
import java.util.UUID;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableCloudEventData;

/**
 * Maps between the typed {@link CloudEventData} used for correlation storage
 * and the raw {@code Map<String, String>} attribute representation carried on
 * OUL CloudEvents.
 */
public final class CloudEventAttributesMapper
{
   private CloudEventAttributesMapper()
   {
   }

   /**
    * Serialises a {@link CloudEventData} to the flat attribute map form
    * expected by OUL CloudEvents.
    *
    * @param cloudEventData typed CloudEvent data
    * @return the attribute map form
    */
   public static Map<String, String> toAttributes(CloudEventData cloudEventData)
   {
      return Map.of(
            "id", cloudEventData.id().toString(),
            "kogitorootprociid", cloudEventData.kogitorootprociid().toString(),
            "kogitoparentprociid", cloudEventData.kogitoparentprociid().toString(),
            "kogitoprocinstanceid", cloudEventData.kogitoprocinstanceid().toString(),
            "kogitorootprocid", cloudEventData.kogitorootprocid(),
            "kogitoprocid", cloudEventData.kogitoprocid(),
            "kogitoprocist", cloudEventData.kogitoprocist(),
            "kogitoprocversion", cloudEventData.kogitoprocversion(),
            "type", cloudEventData.type(),
            "source", cloudEventData.source());
   }

   /**
    * Reconstructs a {@link CloudEventData} from the flat attribute map form
    * carried on OUL CloudEvents.
    *
    * @param attributes the attribute map form
    * @return the typed CloudEvent data
    * @throws IllegalArgumentException if {@code attributes} is {@code null}
    */
   public static CloudEventData toCloudEventData(Map<String, String> attributes)
   {
      if (attributes == null)
      {
         throw new IllegalArgumentException("attributes must not be null");
      }
      return ImmutableCloudEventData.builder()
            .id(UUID.fromString(attributes.get("id")))
            .kogitorootprociid(UUID.fromString(attributes.get("kogitorootprociid")))
            .kogitoparentprociid(UUID.fromString(attributes.get("kogitoparentprociid")))
            .kogitoprocinstanceid(UUID.fromString(attributes.get("kogitoprocinstanceid")))
            .kogitorootprocid(attributes.get("kogitorootprocid"))
            .kogitoprocid(attributes.get("kogitoprocid"))
            .kogitoprocist(attributes.get("kogitoprocist"))
            .kogitoprocversion(attributes.get("kogitoprocversion"))
            .type(attributes.get("type"))
            .source(attributes.get("source"))
            .build();
   }
}
