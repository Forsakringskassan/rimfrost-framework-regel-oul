package se.fk.rimfrost.framework.regel.oul.storage.internal;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.regel.oul.storage.RegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static se.fk.rimfrost.framework.regel.oul.storage.internal.StorageTestData.BY_INSTANT;
import static se.fk.rimfrost.framework.regel.oul.storage.internal.StorageTestData.createRegelCommonData;

@QuarkusTest
public class RegelCommonDataPersistenceTest
{
   @Inject
   RegelCommonDataStorage regelCommonDataStorage;

   @Test
   void insert_round_trips_every_property()
   {
      var handlaggningId = UUID.randomUUID();
      var expected = createRegelCommonData(1);

      regelCommonDataStorage.setRegelCommonData(handlaggningId, expected);

      assertThat(regelCommonDataStorage.getRegelCommonData(handlaggningId))
            .usingRecursiveComparison()
            .withComparatorForType(BY_INSTANT, OffsetDateTime.class)
            .isEqualTo(expected);
   }

   @Test
   void update_overwrites_every_property()
   {
      var handlaggningId = UUID.randomUUID();
      regelCommonDataStorage.setRegelCommonData(handlaggningId, createRegelCommonData(1));
      var second = createRegelCommonData(2);

      regelCommonDataStorage.setRegelCommonData(handlaggningId, second);

      assertThat(regelCommonDataStorage.getRegelCommonData(handlaggningId))
            .usingRecursiveComparison()
            .withComparatorForType(BY_INSTANT, OffsetDateTime.class)
            .isEqualTo(second);
   }

   @Test
   void update_clears_nullable_properties()
   {
      var handlaggningId = UUID.randomUUID();
      var full = createRegelCommonData(1);
      regelCommonDataStorage.setRegelCommonData(handlaggningId, full);
      var cleared = ImmutableRegelCommonData.copyOf(full)
            .withOulUppgiftId(null)
            .withUppgift(ImmutableUppgift.copyOf(full.uppgift())
                  .withUtfordTS(null)
                  .withPlaneradTillTS(null)
                  .withUtforare(null)
                  .withUppgiftStatus(null)
                  .withKommentar(null)
                  .withRegelutfall(null));

      regelCommonDataStorage.setRegelCommonData(handlaggningId, cleared);

      assertThat(regelCommonDataStorage.getRegelCommonData(handlaggningId))
            .usingRecursiveComparison()
            .withComparatorForType(BY_INSTANT, OffsetDateTime.class)
            .isEqualTo(cleared);
   }
}
