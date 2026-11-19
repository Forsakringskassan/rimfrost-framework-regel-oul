package se.fk.rimfrost.framework.regel.oul.storage.internal;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.regel.oul.storage.CloudEventDataStorage;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static se.fk.rimfrost.framework.regel.oul.storage.internal.StorageTestData.createCloudEventData;

@QuarkusTest
public class CloudEventDataPersistenceTest
{
   @Inject
   CloudEventDataStorage cloudEventDataStorage;

   @Test
   void insert_round_trips_every_property()
   {
      var handlaggningId = UUID.randomUUID();
      var expected = createCloudEventData(1);

      cloudEventDataStorage.setCloudEventData(handlaggningId, expected);

      assertThat(cloudEventDataStorage.getCloudEventData(handlaggningId))
            .usingRecursiveComparison()
            .isEqualTo(expected);
   }

   @Test
   void update_overwrites_every_property()
   {
      var handlaggningId = UUID.randomUUID();
      cloudEventDataStorage.setCloudEventData(handlaggningId, createCloudEventData(1));
      var second = createCloudEventData(2);

      cloudEventDataStorage.setCloudEventData(handlaggningId, second);

      assertThat(cloudEventDataStorage.getCloudEventData(handlaggningId))
            .usingRecursiveComparison()
            .isEqualTo(second);
   }
}
