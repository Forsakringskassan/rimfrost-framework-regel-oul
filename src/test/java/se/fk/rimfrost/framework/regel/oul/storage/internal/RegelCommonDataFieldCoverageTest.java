package se.fk.rimfrost.framework.regel.oul.storage.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static se.fk.rimfrost.framework.regel.oul.storage.internal.StorageTestData.BY_INSTANT;
import static se.fk.rimfrost.framework.regel.oul.storage.internal.StorageTestData.createRegelCommonData;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Fails when a property is added to {@link RegelCommonData} (or its nested
 * {@code Uppgift} value types) or a field is added to {@link RegelCommonDataEntity}
 * without being carried through {@link RegelCommonDataMapper}.
 */
class RegelCommonDataFieldCoverageTest
{
   /** Domain properties deliberately not persisted. */
   static final Set<String> NOT_PERSISTED = Set.of(
         "uppgift.underlag",
         "uppgift.resultat");

   /** Entity columns managed by JPA, not by the mapper. */
   static final Set<String> TECHNICAL_COLUMNS = Set.of("version", "createdAt", "updatedAt");

   static final String[] VALUE_PACKAGES =
   {
         "se.fk.rimfrost.framework.regel.oul.storage.entity",
         "se.fk.rimfrost.framework.handlaggning.model"
   };

   final RegelCommonDataMapper mapper = new RegelCommonDataMapper();

   @Test
   void fixture_populates_every_domain_property()
   {
      StorageFieldCoverage.assertFullyPopulated(createRegelCommonData(1), NOT_PERSISTED, VALUE_PACKAGES);
   }

   @Test
   void to_entity_writes_every_entity_field()
   {
      var entity = mapper.toEntity(UUID.randomUUID(), createRegelCommonData(1));
      StorageFieldCoverage.assertAllEntityFieldsSet(entity, TECHNICAL_COLUMNS);
   }

   @Test
   void domain_round_trip_preserves_every_property()
   {
      var expected = createRegelCommonData(1);
      var actual = mapper.toDomain(mapper.toEntity(UUID.randomUUID(), expected));

      assertThat(actual)
            .usingRecursiveComparison()
            .withComparatorForType(BY_INSTANT, OffsetDateTime.class)
            .isEqualTo(expected);
   }
}
