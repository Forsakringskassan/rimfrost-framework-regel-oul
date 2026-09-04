package se.fk.rimfrost.framework.regel.oul.storage.internal;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/**
 * Panache repository for {@link RegelCommonDataEntity}, keyed by handläggning id.
 */
@ApplicationScoped
class RegelCommonDataRepository implements PanacheRepositoryBase<RegelCommonDataEntity, UUID>
{
}
