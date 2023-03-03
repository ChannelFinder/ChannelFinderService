package org.phoebus.channelfinder.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.phoebus.channelfinder.XmlChannel;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for ChannelFinder and Elasticsearch that make use of existing dockerization
 * with docker-compose.yml / Dockerfile.
 *
 * <p>
 * Focus of this class is to have ChannelFinder and Elasticsearch up and running together with usage of
 * {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
 *
 * @author Lars Johansson
 *
 * @see ITTestFixturePopulateService
 */
@Testcontainers
class ChannelFinderChannelsPopulateServiceIT {

    @Container
    public static final DockerComposeContainer<?> ENVIRONMENT =
        new DockerComposeContainer<>(new File("docker-compose-integrationtest.yml"))
            .waitingFor(ITUtil.CHANNELFINDER, Wait.forLogMessage(".*Started Application.*", 1));

    /**
     * Test {@link org.phoebus.channelfinder.CFResourceDescriptors#CHANNEL_RESOURCE_URI}.
     *
     * @see ChannelFinderChannelsIT#handleChannels3QueryByPattern()
     * @see ITTestFixture
     * @see ITTestFixturePopulateService
     */
    @Test
    void handleChannelsQueryByPattern() {
        // what
        //     query by pattern
        //     --------------------------------------------------------------------------------
        //     set up test fixture
        //     test
        //         query by pattern
        //             combine search parameters and channels, properties, tags
        //     tear down test fixture

        // --------------------------------------------------------------------------------
        // set up test fixture
        // --------------------------------------------------------------------------------

        ITTestFixturePopulateService.setup();

        try {
            // note
            //     { } need to be encoded to %7B %7D in query string - otherwise it will be bad request

            XmlChannel[] result, result2 = null;

            ITUtilChannels.assertCountChannels(1500);
            ITUtilChannels.assertListChannels (1500);

            // channel (name)
            //     query for pattern
            //         non-existing
            //         exact
            //         pagination
            //         regex, regex pagination

            // BR:C001-BI:1{BLA}On-St --> BR:C001-BI:1%7BBLA%7DOn-St
            // SR:C001-BI:1{BHS}On-St --> SR:C001-BI:1%7BBHS%7DOn-St
            ITUtilChannels.assertCountChannels("?~name=asdf", 0);
            ITUtilChannels.assertListChannels ("?~name=asdf", 0);
            ITUtilChannels.assertCountChannels("?~name=BR:C001-BI:1%7BBLA%7DOn-St", 1);
            ITUtilChannels.assertListChannels ("?~name=BR:C001-BI:1%7BBLA%7DOn-St", 1);
            ITUtilChannels.assertCountChannels("?~name=SR:C001-BI:1%7BBHS%7DOn-St", 1);
            ITUtilChannels.assertListChannels ("?~name=SR:C001-BI:1%7BBHS%7DOn-St", 1);
            // ? appears to not work in query string
            // {} ----> CHANGE POPULATESERVICE
            ITUtilChannels.assertCountChannels("?~name=*St", 682);
            ITUtilChannels.assertListChannels ("?~name=*St", 682);

            ITUtilChannels.assertCountChannels("?~size=100", 1500);
            result = ITUtilChannels.assertListChannels ("?~size=100", 100);
            ITUtilChannels.assertCountChannels("?~size=100&~from=3", 1500);
            result2 = ITUtilChannels.assertListChannels ("?~size=100&~from=3", 100);
            assertEquals(result2.length, result.length);
            assertNotEquals(result2[0].getName(), result[0].getName());

            // property (name)
            //     query for pattern
            //         non-existing
            //         exact
            //         regex, regex pagination
            //         or, or regex, or regex pagination
            //         not, not with regex, not with regex and pagination

            ITUtilChannels.assertCountChannels("?asdf=asdf", 0);
            ITUtilChannels.assertListChannels ("?asdf=asdf", 0);
            ITUtilChannels.assertCountChannels("?location=asdf", 0);
            ITUtilChannels.assertListChannels ("?location=asdf", 0);
            ITUtilChannels.assertCountChannels("?location=booster", 500);
            ITUtilChannels.assertListChannels ("?location=booster", 500);
            ITUtilChannels.assertCountChannels("?group1=200", 200);
            ITUtilChannels.assertListChannels ("?group1=200", 200);
            ITUtilChannels.assertCountChannels("?location=b*ster", 500);
            ITUtilChannels.assertListChannels ("?location=b*ster", 500);
            ITUtilChannels.assertCountChannels("?group1=2*", 222);
            ITUtilChannels.assertListChannels ("?group1=2*", 222);
            ITUtilChannels.assertCountChannels("?prop39=4*", 111);
            ITUtilChannels.assertListChannels ("?prop39=4*", 111);

            ITUtilChannels.assertCountChannels("?group1=2*&~size=50", 222);
            result = ITUtilChannels.assertListChannels ("?group1=2*&~size=50", 50);
            ITUtilChannels.assertCountChannels("?group1=2*&~size=50&~from=3", 222);
            result2 = ITUtilChannels.assertListChannels ("?group1=2*&~size=50&~from=3", 50);
            assertEquals(result2.length, result.length);
            assertNotEquals(result2[0].getName(), result[0].getName());

            // tag (name)
            //     query for pattern
            //         non-existing
            //         exact - archived, handle_this, noteworthy, not_used
            //         regex, regex pagination
            //         not, not regex, not regex pagination

            ITUtilChannels.assertCountChannels("?~tag=asdf", 0);
            ITUtilChannels.assertListChannels ("?~tag=asdf", 0);
            ITUtilChannels.assertCountChannels("?~tag=group4_500", 500);
            ITUtilChannels.assertListChannels ("?~tag=group4_500", 500);
            ITUtilChannels.assertCountChannels("?~tag=group7_20", 20);
            ITUtilChannels.assertListChannels ("?~tag=group7_20", 20);
            ITUtilChannels.assertCountChannels("?~tag=*_2*", 766);
            ITUtilChannels.assertListChannels ("?~tag=*_2*", 766);
            ITUtilChannels.assertCountChannels("?~tag!=*_2*", 734);
            ITUtilChannels.assertListChannels ("?~tag!=*_2*", 734);

            ITUtilChannels.assertCountChannels("?~tag=*_2*&~size=50", 766);
            result = ITUtilChannels.assertListChannels ("?~tag=*_2*&~size=20", 20);
            ITUtilChannels.assertCountChannels("?~tag=*_2*&~size=50&~from=3", 766);
            result2 = ITUtilChannels.assertListChannels ("?~tag=*_2*&~size=20&~from=3", 20);
            assertEquals(result2.length, result.length);
            assertNotEquals(result2[0].getName(), result[0].getName());

            // combinations
            //     query for pattern
            //         complex
            //             properties
            //             tags
            //             properties, tags
            //             properties, tags, pagination

            //     assigment of properties and tags to channels vary between test runs
            //     difficult to test combinations of property names and tag names in query string
        } catch (Exception e) {
            fail();
        }

        // --------------------------------------------------------------------------------
        // tear down test fixture
        // --------------------------------------------------------------------------------

        ITTestFixturePopulateService.tearDown();
    }

}
