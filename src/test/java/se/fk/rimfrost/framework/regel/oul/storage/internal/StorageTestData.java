package se.fk.rimfrost.framework.regel.oul.storage.internal;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.UUID;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableRegelutfall;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableCloudEventData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Fully populated fixtures for storage tests.
 *
 * <p>The seed only matters for update tests: every property differs between two
 * seeds, so an update from {@code createXYZ(1)} to {@code createXYZ(2)} touches
 * every column.
 */
final class StorageTestData
{
   /** timestamptz keeps the instant, not the offset. */
   static final Comparator<OffsetDateTime> BY_INSTANT = Comparator.comparing(OffsetDateTime::toInstant);

   private StorageTestData()
   {
   }

   /**
    * Every persisted property is set to a non-default value. Timestamps have
    * microsecond precision to match PostgreSQL {@code timestamptz}.
    */
   static RegelCommonData createRegelCommonData(int seed)
   {
      var ts = OffsetDateTime.of(2026, 11, 19, 9, 24, 13, 392_798_000, ZoneOffset.ofHours(1)).plusMinutes(seed);
      return ImmutableRegelCommonData.builder()
            .oulUppgiftId(UUID.randomUUID())
            .uppgift(ImmutableUppgift.builder()
                  .id(UUID.randomUUID())
                  .version(7 + seed)
                  .skapadTS(ts)
                  .utfordTS(ts.plusHours(1))
                  .planeradTillTS(ts.plusDays(1))
                  .utforare(ImmutableIdtyp.builder().typId("PID-" + seed).varde("19700101000" + seed).build())
                  .uppgiftStatus("PAGAR-" + seed)
                  .kommentar("kommentar-" + seed)
                  .aktivitetId(UUID.randomUUID())
                  .fSSAinformation("FSSAinformation.HANDLAGGNING_PAGAR-" + seed)
                  .uppgiftSpecifikation(ImmutableUppgiftSpecifikation.builder()
                        .id(UUID.randomUUID())
                        .version(3 + seed)
                        .build())
                  .regelutfall(ImmutableRegelutfall.builder().varde("JA-" + seed).build())
                  .build())
            .build();
   }

   /** Every attribute is set. */
   static CloudEventData createCloudEventData(int seed)
   {
      return ImmutableCloudEventData.builder()
            .id(UUID.randomUUID())
            .kogitorootprociid(UUID.randomUUID())
            .kogitoparentprociid(UUID.randomUUID())
            .kogitoprocinstanceid(UUID.randomUUID())
            .kogitorootprocid("root-proc-id-" + seed)
            .kogitoprocid("proc-id-" + seed)
            .kogitoprocist("proc-ist-" + seed)
            .kogitoprocversion("1." + seed)
            .type("regel-response-" + seed)
            .source("RegelOulTest-" + seed)
            .build();
   }
}
