package se.fk.rimfrost.framework.regel.oul.storage.internal;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.regel.oul.storage.entity.ImmutableRegelCommonData;
import se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData;

/**
 * Maps between {@link se.fk.rimfrost.framework.regel.oul.storage.entity.RegelCommonData}
 * (including its embedded {@link se.fk.rimfrost.framework.handlaggning.model.Uppgift}) and
 * {@link RegelCommonDataEntity}.
 */
@ApplicationScoped
class RegelCommonDataMapper
{
   RegelCommonDataEntity toEntity(UUID handlaggningId, RegelCommonData data)
   {
      var entity = new RegelCommonDataEntity();
      entity.handlaggningId = handlaggningId;
      entity.oulUppgiftId = data.oulUppgiftId();
      mapUppgiftToEntity(data.uppgift(), entity);
      return entity;
   }

   RegelCommonData toDomain(RegelCommonDataEntity entity)
   {
      return ImmutableRegelCommonData.builder()
            .uppgift(mapUppgiftFromEntity(entity))
            .oulUppgiftId(entity.oulUppgiftId)
            .build();
   }

   private void mapUppgiftToEntity(Uppgift uppgift, RegelCommonDataEntity entity)
   {
      entity.uppgiftId = uppgift.id();
      entity.uppgiftVersion = uppgift.version();
      entity.uppgiftSkapadTs = uppgift.skapadTS().toInstant();
      entity.uppgiftUtfordTs = uppgift.utfordTS() != null ? uppgift.utfordTS().toInstant() : null;
      entity.uppgiftPlaneradTs = uppgift.planeradTillTS() != null ? uppgift.planeradTillTS().toInstant() : null;
      entity.uppgiftStatus = uppgift.uppgiftStatus();
      entity.uppgiftAktivitetId = uppgift.aktivitetId();
      entity.uppgiftFssaInformation = uppgift.fSSAinformation();
      entity.uppgiftSpecifikationId = uppgift.uppgiftSpecifikation().id();
      entity.uppgiftSpecifikationVersion = uppgift.uppgiftSpecifikation().version();

      if (uppgift.utforare() != null)
      {
         entity.uppgiftUtforarIdTypId = uppgift.utforare().typId();
         entity.uppgiftUtforarIdVarde = uppgift.utforare().varde();
      }
   }

   private Uppgift mapUppgiftFromEntity(RegelCommonDataEntity entity)
   {
      var builder = ImmutableUppgift.builder()
            .id(entity.uppgiftId)
            .version(entity.uppgiftVersion)
            .skapadTS(OffsetDateTime.ofInstant(entity.uppgiftSkapadTs, ZoneId.systemDefault()))
            .uppgiftStatus(entity.uppgiftStatus)
            .aktivitetId(entity.uppgiftAktivitetId)
            .fSSAinformation(entity.uppgiftFssaInformation)
            .uppgiftSpecifikation(ImmutableUppgiftSpecifikation.builder()
                  .id(entity.uppgiftSpecifikationId)
                  .version(entity.uppgiftSpecifikationVersion)
                  .build());

      if (entity.uppgiftUtfordTs != null)
      {
         builder.utfordTS(OffsetDateTime.ofInstant(entity.uppgiftUtfordTs, ZoneId.systemDefault()));
      }
      if (entity.uppgiftPlaneradTs != null)
      {
         builder.planeradTillTS(OffsetDateTime.ofInstant(entity.uppgiftPlaneradTs, ZoneId.systemDefault()));
      }
      if (entity.uppgiftUtforarIdTypId != null)
      {
         builder.utforare(ImmutableIdtyp.builder()
               .typId(entity.uppgiftUtforarIdTypId)
               .varde(entity.uppgiftUtforarIdVarde)
               .build());
      }

      return builder.build();
   }
}
