package se.fk.rimfrost.framework.regel.oul.storage.internal;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableProcessTopicInfo;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ProcessTopicInfo;
import java.util.UUID;

/**
 * Maps between {@link se.fk.rimfrost.framework.regel.oul.storage.entity.ProcessTopicInfo}
 * and {@link ProcessTopicInfoEntity}.
 */
@ApplicationScoped
public class ProcessTopicInfoMapper
{
   public ProcessTopicInfoEntity toEntity(UUID handlaggningId, ProcessTopicInfo processTopicInfo)
   {
      ProcessTopicInfoEntity processTopicInfoEntity = new ProcessTopicInfoEntity();
      processTopicInfoEntity.handlaggningId = handlaggningId;
      processTopicInfoEntity.replyTopic = processTopicInfo.replyTopic();
      return processTopicInfoEntity;
   }

   public ProcessTopicInfo toDomain(ProcessTopicInfoEntity processTopicInfoEntity)
   {
      return ImmutableProcessTopicInfo.builder().replyTopic(processTopicInfoEntity.replyTopic).build();
   }
}
