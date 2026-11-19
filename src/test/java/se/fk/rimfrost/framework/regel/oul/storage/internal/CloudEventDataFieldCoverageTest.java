package se.fk.rimfrost.framework.regel.oul.storage.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static se.fk.rimfrost.framework.regel.oul.storage.internal.StorageTestData.createCloudEventData;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;

/**
 * Fails when a property is added to {@link CloudEventData} or a field is added to
 * {@link CloudEventDataEntity} without being carried through {@link CloudEventDataMapper}.
 */
class CloudEventDataFieldCoverageTest
{
   /** Entity columns managed by JPA, not by the mapper. */
   static final Set<String> TECHNICAL_COLUMNS = Set.of("version", "createdAt", "updatedAt");

   static final String VALUE_PACKAGE = "se.fk.rimfrost.framework.regel.logic.entity";

   final CloudEventDataMapper mapper = new CloudEventDataMapper();

   @Test
   void fixture_populates_every_domain_property()
   {
      StorageFieldCoverage.assertFullyPopulated(createCloudEventData(1), Set.of(), VALUE_PACKAGE);
   }

   @Test
   void to_entity_writes_every_entity_field()
   {
      var entity = mapper.toEntity(UUID.randomUUID(), createCloudEventData(1));
      StorageFieldCoverage.assertAllEntityFieldsSet(entity, TECHNICAL_COLUMNS);
   }

   @Test
   void domain_round_trip_preserves_every_property()
   {
      var expected = createCloudEventData(1);
      var actual = mapper.toDomain(mapper.toEntity(UUID.randomUUID(), expected));

      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
   }
}
