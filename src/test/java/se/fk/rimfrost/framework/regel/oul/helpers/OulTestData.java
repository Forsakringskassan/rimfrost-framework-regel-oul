package se.fk.rimfrost.framework.regel.oul.helpers;

import static org.mockito.Mockito.mock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableOulStatus;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableProcessInfo;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.model.Erbjudande;
import se.fk.rimfrost.framework.oul.model.ImmutableErbjudande;
import se.fk.rimfrost.framework.regel.oul.logic.CloudEventAttributesMapper;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.oul.logic.entity.ImmutableOulUppgiftSpec;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulUppgiftSpec;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableCloudEventData;

/**
 * Factory methods for building test fixtures used by regel-oul unit and
 * integration tests. Values are minimal and consistent across fixtures so
 * tests can build small object graphs without wiring the full domain.
 */
public final class OulTestData
{
   public static final String DEFAULT_REPLY_TOPIC = "regel-response-test";
   public static final String DEFAULT_UTFORAR_TYP = "PID";
   public static final String DEFAULT_UTFORAR_VARDE = "197001010000";

   private OulTestData()
   {
   }

   /**
    * Builds a minimal {@link Handlaggning} suitable for OUL flows. The
    * {@link Yrkande} is a Mockito mock — populate it in-line if the specific
    * test needs erbjudande / yrkande fields.
    *
    * @param id      handläggning id
    * @param version handläggning version
    * @return a fully populated {@link Handlaggning}
    */
   public static Handlaggning handlaggning(UUID id, int version)
   {
      return ImmutableHandlaggning.builder()
            .id(id)
            .version(version)
            .yrkande(mock(Yrkande.class))
            .processInstansId(UUID.randomUUID())
            .skapadTS(OffsetDateTime.now())
            .avslutadTS(OffsetDateTime.now())
            .handlaggningspecifikationId(UUID.randomUUID())
            .build();
   }

   /**
    * @return a {@link CloudEventData} with random UUIDs and stable string
    *         fields — safe to use across tests without collision.
    */
   public static CloudEventData cloudEventData()
   {
      return ImmutableCloudEventData.builder()
            .id(UUID.randomUUID())
            .kogitorootprociid(UUID.randomUUID())
            .kogitoparentprociid(UUID.randomUUID())
            .kogitoprocinstanceid(UUID.randomUUID())
            .kogitorootprocid("root-proc-id")
            .kogitoprocid("proc-id")
            .kogitoprocist("proc-ist")
            .kogitoprocversion("1.0")
            .type("regel-response")
            .source("RegelOulTest")
            .build();
   }

   /**
    * @return an {@link Erbjudande} test fixture
    */
   public static Erbjudande erbjudande()
   {
      return ImmutableErbjudande.builder()
            .id("erb-1")
            .namn("Test erbjudande")
            .build();
   }

   /**
    * Builds an {@link OulUppgiftSpec} bound to the given handläggning. The
    * caller owns the {@link Handlaggning} fixture and can mutate before
    * passing it in.
    *
    * @param handlaggning    the handläggning the uppgift belongs to
    * @param cloudEventData  typed CloudEvent metadata (persisted for correlation)
    * @return a fully populated {@link OulUppgiftSpec}
    */
   public static OulUppgiftSpec oulUppgiftSpec(Handlaggning handlaggning, CloudEventData cloudEventData)
   {
      return ImmutableOulUppgiftSpec.builder()
            .handlaggningId(handlaggning.id())
            .handlaggning(handlaggning)
            .replyTo(DEFAULT_REPLY_TOPIC)
            .cloudEventData(cloudEventData)
            .cloudEventAttributes(CloudEventAttributesMapper.toAttributes(cloudEventData))
            .regel("test-regel")
            .beskrivning("Test uppgift")
            .verksamhetslogik("test-verksamhet")
            .roll("handlaggare")
            .url("/test/uppgift")
            .erbjudande(erbjudande())
            .aktivitetId(UUID.randomUUID())
            .uppgiftSpecifikationId(UUID.randomUUID())
            .uppgiftSpecifikationVersion(1)
            .build();
   }

   /**
    * Builds a minimal {@link Uppgift} at version 1 with status {@code NY},
    * suitable for seeding correlation storage in cleanup and resilience tests.
    *
    * @return a minimal {@link Uppgift}
    */
   public static Uppgift seedUppgift()
   {
      return ImmutableUppgift.builder()
            .id(UUID.randomUUID())
            .version(1)
            .aktivitetId(UUID.randomUUID())
            .skapadTs(OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS))
            .uppgiftStatus("NY")
            .fSSAinformation("FSSAinformation.HANDLAGGNING_PAGAR")
            .uppgiftSpecifikation(ImmutableUppgiftSpecifikation.builder()
                  .id(UUID.randomUUID())
                  .version(1)
                  .build())
            .build();
   }

   /**
    * Builds an {@link OulStatus} callback matching a previously created OUL
    * uppgift.
    *
    * @param handlaggningId    the handläggning id the callback refers to
    * @param uppgiftId         the OUL uppgift id the callback refers to
    * @param uppgiftStatus     the new uppgift status ({@code STARTED},
    *                          {@code COMPLETED}, etc.)
    * @param cloudEventData    typed CloudEvent metadata — the attribute map
    *                          derived from this is placed on the callback's
    *                          {@code processInfo.cloudeventAttributes}
    * @return a populated {@link OulStatus}
    */
   public static OulStatus oulStatus(UUID handlaggningId, UUID uppgiftId, String uppgiftStatus,
         CloudEventData cloudEventData)
   {
      Map<String, String> attributes = CloudEventAttributesMapper.toAttributes(cloudEventData);
      return ImmutableOulStatus.builder()
            .handlaggningId(handlaggningId)
            .uppgiftId(uppgiftId)
            .utforarId(ImmutableIdtyp.builder()
                  .typId(DEFAULT_UTFORAR_TYP)
                  .varde(DEFAULT_UTFORAR_VARDE)
                  .build())
            .uppgiftStatus(uppgiftStatus)
            .processInfo(ImmutableProcessInfo.builder()
                  .replyTopic(DEFAULT_REPLY_TOPIC)
                  .cloudeventAttributes(attributes)
                  .build())
            .planeradTill(OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MICROS))
            .build();
   }
}
