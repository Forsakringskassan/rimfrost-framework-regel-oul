package se.fk.rimfrost.framework.regel.oul.helpers;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.HashMap;
import java.util.Map;
import se.fk.rimfrost.framework.regel.WireMockHandlaggning;

/**
 * WireMock test resource for regel-oul tests.
 *
 * <p>Extends {@link WireMockHandlaggning} with stubs for OUL and handläggning
 * endpoints. All stub responses are loaded from
 * {@code src/test/resources/mappings/}:
 * <ul>
 *   <li>{@code post-uppgifter.json} — {@code POST /uppgifter}
 *   <li>{@code post-uppgifter-end.json} — {@code POST /uppgifter/{id}/end}
 *   <li>{@code get-handlaggning.json} — {@code GET /handlaggning/{id}}
 *   <li>{@code put-handlaggning.json} — {@code PUT /handlaggning/{id}}
 * </ul>
 *
 * <p>Also injects {@code oul.api.base-url} into the Quarkus config so the
 * OUL adapter client points at the running WireMock server.
 *
 * <p>Apply to a test with {@code @QuarkusTestResource(WireMockRegelOul.class)}.
 */
public class WireMockRegelOul extends WireMockHandlaggning
{
   /** Uppgift id returned by the default {@code POST /uppgifter} stub. */
   public static final String DEFAULT_UPPGIFT_ID = "11e53b18-e9ac-4707-825b-a1cb80689c29";

   /** Handläggning id embedded in the default GET/PUT handläggning responses. */
   public static final String DEFAULT_HANDLAGGNING_ID = "5367f6b8-cc4a-11f0-8de9-199901011234";

   /**
    * Adds the {@code oul.api.base-url} mapping so the OUL adapter client
    * targets the same WireMock server as the handläggning client.
    *
    * @param server active WireMock server
    * @return combined property overrides
    */
   @Override
   protected Map<String, String> wiremockMapping(WireMockServer server)
   {
      Map<String, String> map = new HashMap<>(super.wiremockMapping(server));
      map.put("oul.api.base-url", server.baseUrl());
      return map;
   }
}
