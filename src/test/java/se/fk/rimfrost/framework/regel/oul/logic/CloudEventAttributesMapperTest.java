package se.fk.rimfrost.framework.regel.oul.logic;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.regel.oul.helpers.OulTestData;

/**
 * Fails when an attribute of {@code CloudEventData} is dropped on the way to or
 * from the OUL CloudEvent attribute map.
 */
class CloudEventAttributesMapperTest
{
   @Test
   void attributes_round_trip_preserves_every_property()
   {
      var expected = OulTestData.cloudEventData();

      var actual = CloudEventAttributesMapper.toCloudEventData(CloudEventAttributesMapper.toAttributes(expected));

      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
   }
}
