package se.fk.rimfrost.framework.regel.oul.storage;

import se.fk.rimfrost.framework.regel.oul.storage.entity.ProcessTopicInfo;
import java.util.UUID;

/**
 * Storage for persisting and retrieving {@link ProcessTopicInfo} keyed by handläggning id.
 *
 * <p>{@link ProcessTopicInfo} carries the Kafka reply topic so that OUL status callbacks
 * can be routed back to the correct consumer topic.
 */
public interface ProcessTopicInfoStorage
{
   /**
    * Returns the stored {@link ProcessTopicInfo} for {@code handlaggningId}, or {@code null} if absent.
    *
    * @param handlaggningId the handläggning whose process topic info to retrieve
    * @return the stored info, or {@code null}
    */
   ProcessTopicInfo getProcessTopicInfo(UUID handlaggningId);

   /**
    * Persists (insert or update) {@code processTopicInfo} for {@code handlaggningId}.
    *
    * @param handlaggningId  the handläggning to store info for
    * @param processTopicInfo the process topic info to store
    */
   void setProcessTopicInfo(UUID handlaggningId, ProcessTopicInfo processTopicInfo);

   /**
    * Deletes the stored process topic info for {@code handlaggningId}. No-op if absent.
    *
    * @param handlaggningId the handläggning whose info to delete
    */
   void deleteProcessTopicInfo(UUID handlaggningId);
}
