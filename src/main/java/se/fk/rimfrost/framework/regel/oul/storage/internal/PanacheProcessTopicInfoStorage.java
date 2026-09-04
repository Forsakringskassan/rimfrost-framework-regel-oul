package se.fk.rimfrost.framework.regel.oul.storage.internal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import se.fk.rimfrost.framework.regel.oul.storage.ProcessTopicInfoStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ProcessTopicInfo;
import java.util.UUID;

/**
 * JPA/Panache implementation of {@link ProcessTopicInfoStorage}.
 *
 * <p>All operations run within the caller's transaction via {@code @Transactional}.
 * Update operations preserve the original {@code version} and {@code createdAt} to
 * satisfy the JPA optimistic-locking invariant.
 */
@ApplicationScoped
@Transactional
public class PanacheProcessTopicInfoStorage implements ProcessTopicInfoStorage
{
   @Inject
   ProcessTopicInfoRepository repository;

   @Inject
   ProcessTopicInfoMapper mapper;

   @Override
   public ProcessTopicInfo getProcessTopicInfo(UUID handlaggningId)
   {
      var result = repository.findById(handlaggningId);

      if (result == null)
      {
         return null;
      }

      return mapper.toDomain(result);
   }

   @Override
   public void setProcessTopicInfo(UUID handlaggningId, ProcessTopicInfo processTopicInfo)
   {
      var existing = repository.findByIdOptional(handlaggningId);

      if (existing.isPresent())
      {
         var entity = existing.get();
         var updated = mapper.toEntity(handlaggningId, processTopicInfo);
         updated.version = entity.version;
         updated.createdAt = entity.createdAt;
         repository.getEntityManager().merge(updated);
      }
      else
      {
         repository.persist(mapper.toEntity(handlaggningId, processTopicInfo));
      }
   }

   @Override
   public void deleteProcessTopicInfo(UUID handlaggningId)
   {
      repository.deleteById(handlaggningId);
   }
}
