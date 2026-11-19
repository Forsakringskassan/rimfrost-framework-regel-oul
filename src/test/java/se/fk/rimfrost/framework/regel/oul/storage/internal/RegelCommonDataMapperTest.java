package se.fk.rimfrost.framework.regel.oul.storage.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableRegelutfall;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegelCommonDataMapperTest
{
   RegelCommonDataMapper regelCommonDataMapper;

   @BeforeEach
   void setup()
   {
      regelCommonDataMapper = new RegelCommonDataMapper();
   }

   @Test
   void should_map_regel_common_data_to_entity_and_back_with_null_utford_ts()
   {
      var uppgift = ImmutableUppgift.builder().from(createUppgift()).utfordTS(null).build();
      var expectedRegelCommonData = ImmutableRegelCommonData.builder().from(createRegelCommonData()).uppgift(uppgift).build();
      var handlaggningId = UUID.randomUUID();

      var entity = regelCommonDataMapper.toEntity(handlaggningId, expectedRegelCommonData);
      var regelCommonData = regelCommonDataMapper.toDomain(entity);

      assertEquals(expectedRegelCommonData, regelCommonData);
   }

   @Test
   void should_map_regel_common_data_to_entity_and_back_with_null_planerad_till_ts()
   {
      var uppgift = ImmutableUppgift.builder().from(createUppgift()).planeradTillTS(null).build();
      var expectedRegelCommonData = ImmutableRegelCommonData.builder().from(createRegelCommonData()).uppgift(uppgift).build();
      var handlaggningId = UUID.randomUUID();

      var entity = regelCommonDataMapper.toEntity(handlaggningId, expectedRegelCommonData);
      var regelCommonData = regelCommonDataMapper.toDomain(entity);

      assertEquals(expectedRegelCommonData, regelCommonData);
   }

   @Test
   void should_map_regel_common_data_to_entity_and_back_with_null_utforare()
   {
      var uppgift = ImmutableUppgift.builder().from(createUppgift()).utforare(null).build();
      var expectedRegelCommonData = ImmutableRegelCommonData.builder().from(createRegelCommonData()).uppgift(uppgift).build();
      var handlaggningId = UUID.randomUUID();

      var entity = regelCommonDataMapper.toEntity(handlaggningId, expectedRegelCommonData);
      var regelCommonData = regelCommonDataMapper.toDomain(entity);

      assertEquals(expectedRegelCommonData, regelCommonData);
   }

   @Test
   void should_map_regel_common_data_to_entity_and_back_with_null_uppgift_status()
   {
      var uppgift = ImmutableUppgift.builder().from(createUppgift()).uppgiftStatus(null).build();
      var expectedRegelCommonData = ImmutableRegelCommonData.builder().from(createRegelCommonData()).uppgift(uppgift).build();
      var handlaggningId = UUID.randomUUID();

      var entity = regelCommonDataMapper.toEntity(handlaggningId, expectedRegelCommonData);
      var regelCommonData = regelCommonDataMapper.toDomain(entity);

      assertEquals(expectedRegelCommonData, regelCommonData);
   }

   @Test
   void should_map_regel_common_data_to_entity_and_back_with_null_kommentar()
   {
      var uppgift = ImmutableUppgift.builder().from(createUppgift()).kommentar(null).build();
      var expectedRegelCommonData = ImmutableRegelCommonData.builder().from(createRegelCommonData()).uppgift(uppgift).build();
      var handlaggningId = UUID.randomUUID();

      var entity = regelCommonDataMapper.toEntity(handlaggningId, expectedRegelCommonData);
      var regelCommonData = regelCommonDataMapper.toDomain(entity);

      assertEquals(expectedRegelCommonData, regelCommonData);
   }

   @Test
   void should_map_regel_common_data_to_entity_and_back_with_null_regelutfall()
   {
      var uppgift = ImmutableUppgift.builder().from(createUppgift()).regelutfall(null).build();
      var expectedRegelCommonData = ImmutableRegelCommonData.builder().from(createRegelCommonData()).uppgift(uppgift).build();
      var handlaggningId = UUID.randomUUID();

      var entity = regelCommonDataMapper.toEntity(handlaggningId, expectedRegelCommonData);
      var regelCommonData = regelCommonDataMapper.toDomain(entity);

      assertEquals(expectedRegelCommonData, regelCommonData);
   }

   private RegelCommonData createRegelCommonData()
   {
      return ImmutableRegelCommonData.builder().oulUppgiftId(UUID.randomUUID()).uppgift(createUppgift()).build();
   }

   private Uppgift createUppgift()
   {
      return ImmutableUppgift.builder()
            .id(UUID.randomUUID())
            .version(1)
            .skapadTS(OffsetDateTime.now())
            .utfordTS(OffsetDateTime.now())
            .planeradTillTS(OffsetDateTime.now())
            .utforare(ImmutableIdtyp.builder().typId(UUID.randomUUID().toString()).varde(UUID.randomUUID().toString()).build())
            .uppgiftStatus("Test status")
            .kommentar("Test comment")
            .aktivitetId(UUID.randomUUID())
            .fSSAinformation(UUID.randomUUID().toString())
            .uppgiftSpecifikation(ImmutableUppgiftSpecifikation.builder().id(UUID.randomUUID()).version(1).build())
            .regelutfall(ImmutableRegelutfall.builder().varde(UUID.randomUUID().toString()).build())
            // .underlag is not mapped, so left empty
            // .resultat is not mapped, so left empty
            .build();
   }
}
