package se.fk.rimfrost.framework.regel.oul.storage.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that JPA optimistic locking is active on
 * {@link RegelCommonDataEntity} (FROUL-NFR-01.1): two concurrent writers that
 * both start from the same version cannot both commit — the later writer must
 * be rejected with an {@link OptimisticLockException} (or Hibernate's
 * equivalent {@link StaleObjectStateException} wrapped in the same causal
 * chain).
 *
 * <p>Placed in the {@code storage.internal} package so the test can construct
 * and manipulate {@link RegelCommonDataEntity} directly — its fields are
 * package-private and the storage's public API refreshes the version on every
 * write, which would mask the optimistic-locking behaviour we are trying to
 * verify.
 *
 * <p>The concurrent scenario is simulated single-threaded using three
 * successive {@code REQUIRES_NEW} transactions: (1) persist an initial row,
 * (2) load a detached snapshot representing writer A, (3) commit writer B
 * which bumps the DB version, (4) try to merge writer A's now-stale snapshot
 * and expect the failure.
 */
@QuarkusTest
class OptimisticLockingTest
{
   @Inject
   EntityManager entityManager;

   /**
    * Verifies JPA optimistic locking on RegelCommonDataEntity (FROUL-NFR-01.1).
    * A row is persisted, then a detached snapshot is taken to represent Writer A at version 0.
    * Writer B commits a change, bumping the DB version to 1.
    * When Writer A attempts to merge its stale snapshot, Hibernate's UPDATE WHERE version = 0
    * matches no rows and throws OptimisticLockException — rejecting the conflicting write.
    */
   @Test
   @DisplayName("FROUL-NFR-01.1: Samtidiga skrivningar mot samma RegelCommonData ska avvisa den föråldrade skrivningen")
   void concurrent_writes_to_same_regel_common_data_should_reject_stale_write()
   {
      UUID handlaggningId = UUID.randomUUID();

      QuarkusTransaction.requiringNew().run(() -> entityManager.persist(seedEntity(handlaggningId)));

      RegelCommonDataEntity staleSnapshot = QuarkusTransaction.requiringNew().call(() -> {
         RegelCommonDataEntity loaded = entityManager.find(RegelCommonDataEntity.class, handlaggningId);
         entityManager.detach(loaded);
         return loaded;
      });
      long initialVersion = staleSnapshot.version;

      QuarkusTransaction.requiringNew().run(() -> {
         RegelCommonDataEntity managed = entityManager.find(RegelCommonDataEntity.class, handlaggningId);
         managed.uppgiftStatus = "WRITER_B_COMMITTED";
      });

      long dbVersionAfterWriterB = QuarkusTransaction.requiringNew()
            .call(() -> entityManager.find(RegelCommonDataEntity.class, handlaggningId).version);
      assertThat(dbVersionAfterWriterB).isGreaterThan(initialVersion);

      assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> {
         staleSnapshot.uppgiftStatus = "WRITER_A_STALE";
         entityManager.merge(staleSnapshot);
         entityManager.flush();
      })).satisfiesAnyOf(
            e -> assertThat(e).isInstanceOf(OptimisticLockException.class),
            e -> assertThat(e).hasRootCauseInstanceOf(OptimisticLockException.class),
            e -> assertThat(e).hasRootCauseInstanceOf(StaleObjectStateException.class));
   }

   private static RegelCommonDataEntity seedEntity(UUID handlaggningId)
   {
      RegelCommonDataEntity e = new RegelCommonDataEntity();
      e.handlaggningId = handlaggningId;
      e.uppgiftId = UUID.randomUUID();
      e.uppgiftVersion = 1;
      e.uppgiftSkapadTs = Instant.now();
      e.uppgiftStatus = "NY";
      e.uppgiftAktivitetId = UUID.randomUUID();
      e.uppgiftFssaInformation = "FSSAinformation.HANDLAGGNING_PAGAR";
      e.uppgiftSpecifikationId = UUID.randomUUID();
      e.uppgiftSpecifikationVersion = 1;
      e.oulUppgiftId = UUID.randomUUID();
      return e;
   }
}
