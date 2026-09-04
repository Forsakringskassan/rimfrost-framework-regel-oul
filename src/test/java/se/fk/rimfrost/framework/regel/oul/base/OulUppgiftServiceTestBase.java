package se.fk.rimfrost.framework.regel.oul.base;

import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import se.fk.rimfrost.framework.regel.RegelResponseMessagePayload;
import se.fk.rimfrost.framework.regel.oul.helpers.WireMockRegelOul;

/**
 * Base class for tests exercising {@code OulUppgiftService} and its
 * collaborators.
 *
 * <p>Provides:
 * <ul>
 *   <li>Access to the JPA {@link EntityManager} for direct DB assertions
 *       against the three correlation tables.
 *   <li>A shared {@link InMemoryConnector} for verifying regel-response
 *       messages produced by {@code sendErrorResponse}.
 *   <li>A per-test {@link #resetState()} hook that clears all correlation
 *       tables and drains previously buffered response messages.
 * </ul>
 *
 * <p>Concrete tests should annotate themselves with {@code @QuarkusTest} and
 * extend this class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class OulUppgiftServiceTestBase
{
   /** Channel name for the outbound regel-responses in-memory sink. */
   public static final String REGEL_RESPONSES_CHANNEL = "regel-responses";

   @Inject
   protected EntityManager entityManager;

   @Inject
   @Connector("smallrye-in-memory")
   protected InMemoryConnector inMemoryConnector;

   /**
    * Truncates the three correlation tables, clears the regel-response sink,
    * and drops any request log entries the shared WireMock server accumulated
    * during previous tests. Without the WireMock reset, count-based
    * assertions like "expected 1 POST" would see requests from earlier tests.
    */
   @BeforeEach
   @Transactional
   protected void resetState()
   {
      entityManager.createNativeQuery("TRUNCATE TABLE regel_oul_test.regel_oul_test_cloud_event_data").executeUpdate();
      entityManager.createNativeQuery("TRUNCATE TABLE regel_oul_test.regel_oul_test_common_data").executeUpdate();
      entityManager.createNativeQuery("TRUNCATE TABLE regel_oul_test.regel_oul_test_process_topic_info").executeUpdate();
      regelResponsesSink().clear();
      if (WireMockRegelOul.getWireMockServer() != null)
      {
         WireMockRegelOul.getWireMockServer().resetRequests();
      }
   }

   /**
    * @return the in-memory sink for the {@code regel-responses} channel
    */
   protected InMemorySink<RegelResponseMessagePayload> regelResponsesSink()
   {
      return inMemoryConnector.sink(REGEL_RESPONSES_CHANNEL);
   }

   /**
    * @return the buffered regel-response messages received since the last
    *         {@link #resetState()}
    */
   protected List<? extends Message<RegelResponseMessagePayload>> receivedRegelResponses()
   {
      return regelResponsesSink().received();
   }
}
