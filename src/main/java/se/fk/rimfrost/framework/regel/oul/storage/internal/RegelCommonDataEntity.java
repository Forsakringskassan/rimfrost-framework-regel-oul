package se.fk.rimfrost.framework.regel.oul.storage.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity backing the {@code common_data} table (prefixed by
 * {@code regel.persistence.table-prefix}).
 *
 * <p>Flattens the {@link se.fk.rimfrost.framework.handlaggning.model.Uppgift} value object
 * into individual columns. Fields are package-private; access is through
 * {@link RegelCommonDataMapper} and {@link PanacheRegelCommonDataStorage}.
 */
@Entity(name = "OulRegelCommonDataEntity")
@Table(name = "common_data")
public class RegelCommonDataEntity
{
   @Id
   @Column(name = "handlaggning_id")
   UUID handlaggningId;

   @Column(name = "uppgift_id")
   UUID uppgiftId;

   @Column(name = "uppgift_version")
   int uppgiftVersion;

   @Column(name = "uppgift_skapad_ts")
   Instant uppgiftSkapadTs;

   @Column(name = "uppgift_utford_ts")
   Instant uppgiftUtfordTs;

   @Column(name = "uppgift_planerad_ts")
   Instant uppgiftPlaneradTs;

   @Column(name = "uppgift_utforar_id_typ_id")
   String uppgiftUtforarIdTypId;

   @Column(name = "uppgift_utforar_id_varde")
   String uppgiftUtforarIdVarde;

   @Column(name = "uppgift_status")
   String uppgiftStatus;

   @Column(name = "uppgift_aktivitet_id")
   UUID uppgiftAktivitetId;

   @Column(name = "uppgift_fssa_information")
   String uppgiftFssaInformation;

   @Column(name = "uppgift_specifikation_id")
   UUID uppgiftSpecifikationId;

   @Column(name = "uppgift_specifikation_version")
   Integer uppgiftSpecifikationVersion;

   @Column(name = "oul_uppgift_id")
   UUID oulUppgiftId;

   @Version
   @Column(name = "version")
   long version;

   @Column(name = "created_at", nullable = false, updatable = false)
   Instant createdAt;

   @Column(name = "updated_at", nullable = false)
   Instant updatedAt;

   @PrePersist
   void onCreate()
   {
      createdAt = Instant.now();
      updatedAt = createdAt;
   }

   @PreUpdate
   void onUpdate()
   {
      updatedAt = Instant.now();
   }
}
