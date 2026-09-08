package se.fk.rimfrost.framework.regel.oul.storage;

import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import java.util.UUID;

/**
 * Storage for persisting and retrieving {@link CloudEventData} keyed by handläggning id.
 */
public interface CloudEventDataStorage
{
   /**
    * Returns the stored {@link CloudEventData} for {@code handlaggningId}, or {@code null} if absent.
    *
    * @param handlaggningId the handläggning whose CloudEvent data to retrieve
    * @return the stored data, or {@code null}
    */
   CloudEventData getCloudEventData(UUID handlaggningId);

   /**
    * Persists (insert or update) {@code cloudEventData} for {@code handlaggningId}.
    *
    * @param handlaggningId the handläggning to store data for
    * @param cloudEventData the CloudEvent data to store
    */
   void setCloudEventData(UUID handlaggningId, CloudEventData cloudEventData);

   /**
    * Deletes the stored CloudEvent data for {@code handlaggningId}. No-op if absent.
    *
    * @param handlaggningId the handläggning whose data to delete
    */
   void deleteCloudEventData(UUID handlaggningId);
}
