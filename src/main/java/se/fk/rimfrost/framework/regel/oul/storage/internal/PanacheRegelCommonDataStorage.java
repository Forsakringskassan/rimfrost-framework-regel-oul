package se.fk.rimfrost.framework.regel.oul.storage.internal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;
import java.util.UUID;

/**
 * JPA/Panache implementation of {@link RegelCommonDataStorage}.
 *
 * <p>All operations run within the caller's transaction via {@code @Transactional}.
 * Update operations preserve the original {@code version} and {@code createdAt} to
 * satisfy the JPA optimistic-locking invariant.
 */
@ApplicationScoped
@Transactional
public class PanacheRegelCommonDataStorage implements RegelCommonDataStorage
{
   @Inject
   RegelCommonDataRepository repository;

   @Inject
   RegelCommonDataMapper mapper;

   @Override
   public RegelCommonData getRegelCommonData(UUID handlaggningId)
   {
      return repository.findByIdOptional(handlaggningId)
            .map(mapper::toDomain)
            .orElse(null);
   }

   @Override
   public void setRegelCommonData(UUID handlaggningId, RegelCommonData regelCommonData)
   {
      var existing = repository.findByIdOptional(handlaggningId);
      if (existing.isPresent())
      {
         var entity = existing.get();
         var updated = mapper.toEntity(handlaggningId, regelCommonData);
         updated.version = entity.version;
         updated.createdAt = entity.createdAt;
         repository.getEntityManager().merge(updated);
      }
      else
      {
         repository.persist(mapper.toEntity(handlaggningId, regelCommonData));
      }
   }

   @Override
   public void deleteRegelCommonData(UUID handlaggningId)
   {
      repository.deleteById(handlaggningId);
   }
}
