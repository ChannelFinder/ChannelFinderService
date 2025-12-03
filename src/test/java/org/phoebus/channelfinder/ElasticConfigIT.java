package org.phoebus.channelfinder;

import java.io.IOException;
import org.phoebus.channelfinder.configuration.ElasticConfig;

public class ElasticConfigIT {

  /**
   * Removes indexes created for the test
   *
   * @param elasticConfig Bean with configuration
   * @throws IOException when request fails
   */
  static void teardown(ElasticConfig elasticConfig) throws IOException {

    String[] indexes =
        new String[] {
          elasticConfig.getES_CHANNEL_INDEX(),
          elasticConfig.getES_PROPERTY_INDEX(),
          elasticConfig.getES_TAG_INDEX()
        };
    for (String index : indexes) {
      if (elasticConfig.getSearchClient().indices().exists(b -> b.index(index)).value()) {
        elasticConfig.getSearchClient().indices().delete(b -> b.index(index));
      }
    }
  }

  /**
   * Makes sure the indexes are recreated before running a test This can happen if two tests share
   * the same ElasticConfig bean and teardown is run at the end. For an example see
   * PropertyManagerIT and PropertyValidationIT use the same @WebMvcTest annotation and then share
   * the ElasticConfig Bean during test runs.
   *
   * @param elasticConfig Bean with configuration
   */
  static void setUp(ElasticConfig elasticConfig) {
    elasticConfig.elasticIndexValidation(elasticConfig.getSearchClient());
  }
}
