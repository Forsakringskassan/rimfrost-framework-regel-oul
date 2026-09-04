package se.fk.rimfrost.framework.regel.oul.storage;

import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;
import java.util.UUID;

/**
 * Storage port for persisting and retrieving {@link RegelCommonData} keyed by handläggning id.
 *
 * <p>{@link RegelCommonData} holds the uppgift reference and OUL uppgift id needed to
 * process status callbacks and synchronise state back to the handläggning.
 */
public interface RegelCommonDataStorage
{
   /**
    * Returns the stored {@link RegelCommonData} for {@code handlaggningId}, or {@code null} if absent.
    *
    * @param handlaggningId the handläggning whose common data to retrieve
    * @return the stored data, or {@code null}
    */
   RegelCommonData getRegelCommonData(UUID handlaggningId);

   /**
    * Persists (insert or update) {@code regelCommonData} for {@code handlaggningId}.
    *
    * @param handlaggningId  the handläggning to store data for
    * @param regelCommonData the common data to store
    */
   void setRegelCommonData(UUID handlaggningId, RegelCommonData regelCommonData);

   /**
    * Deletes the stored common data for {@code handlaggningId}. No-op if absent.
    *
    * @param handlaggningId the handläggning whose data to delete
    */
   void deleteRegelCommonData(UUID handlaggningId);
}
