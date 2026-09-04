package se.fk.rimfrost.framework.regel.oul.storage.internal;

import static org.assertj.core.api.Assertions.assertThat;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that Flyway migrations produce the three framework-owned correlation
 * tables under the configured schema and prefix (FROUL-PR-01.1/1.2).
 *
 * <p>The prefix used here is the test-config value
 * ({@code regel.persistence.table-prefix=regel_oul_test}), demonstrating that
 * the prefix is driven by configuration.
 *
 * <p>Deliberately does not extend {@code OulUppgiftServiceTestBase} — it
 * needs no WireMock setup or per-test truncation; a bare {@code @QuarkusTest}
 * is enough to trigger boot-time Flyway execution.
 */
@QuarkusTest
class PersistenceStartupTest
{
   private static final String SCHEMA = "regel_oul_test";
   private static final String TABLE_PREFIX = "regel_oul_test";

   @Inject
   EntityManager entityManager;

   @Test
   @DisplayName("FROUL-PR-01.1/1.2: Flyway skapar de tre framework-tabellerna med konfigurerat prefix")
   void flyway_should_create_three_prefixed_tables()
   {
      @SuppressWarnings("unchecked")
      List<String> tableNames = entityManager.createNativeQuery(
            "SELECT table_name FROM information_schema.tables "
                  + "WHERE table_schema = :schema AND table_name LIKE :prefix")
            .setParameter("schema", SCHEMA)
            .setParameter("prefix", TABLE_PREFIX + "_%")
            .getResultList();

      assertThat(tableNames).contains(
            TABLE_PREFIX + "_common_data",
            TABLE_PREFIX + "_cloud_event_data",
            TABLE_PREFIX + "_process_topic_info");
   }
}
