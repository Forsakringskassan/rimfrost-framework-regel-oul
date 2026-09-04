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
 * JPA entity backing the {@code cloud_event_data} table (prefixed by
 * {@code regel.persistence.table-prefix}).
 *
 * <p>Fields are package-private; access is through
 * {@link CloudEventDataMapper} and {@link PanacheCloudEventDataStorage}.
 */
@Entity(name = "OulCloudEventDataEntity")
@Table(name = "cloud_event_data")
public class CloudEventDataEntity
{
   @Id
   @Column(name = "handlaggning_id")
   UUID handlaggningId;

   @Column(name = "event_id", nullable = false)
   UUID eventId;

   UUID kogitorootprociid;
   UUID kogitoparentprociid;
   UUID kogitoprocinstanceid;
   String kogitorootprocid;
   String kogitoprocid;
   String kogitoprocist;
   String kogitoprocversion;
   String type;
   String source;

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
