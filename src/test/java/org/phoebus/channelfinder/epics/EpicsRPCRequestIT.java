package org.phoebus.channelfinder.epics;

import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVAURI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.channelfinder.ChannelManager;
import org.phoebus.channelfinder.PropertyManager;
import org.phoebus.channelfinder.TagManager;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple example client to the channelfinder epics rpc service
 * @author Kunal Shroff
 *
 */

// TODO fix test in CI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(ChannelFinderEpicsService.class)
@WithMockUser(roles = "CF-ADMINS")
@TestPropertySource(value = "classpath:application_test.properties")
class EpicsRPCRequestIT {
    private static final Logger logger = Logger.getLogger(EpicsRPCRequestIT.class.getName());
    static PVAClient pvaClient;

    static {
        try {
            pvaClient = new PVAClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    public static void cleanup() {
        pvaChannel.close();
        pvaClient.close();
    }
    private static final PVAChannel pvaChannel = pvaClient.getChannel(ChannelFinderEpicsService.SERVICE_DESC);;


    @Autowired
    ChannelManager channelManager;

    @Autowired
    PropertyManager propertyManager;

    @Autowired
    TagManager tagManager;

    @Test
    void testRPCService() throws ExecutionException, InterruptedException, TimeoutException {
        pvaChannel.connect().get(5, TimeUnit.SECONDS);

        Channel testChannel0 = new Channel("testChannel0", "testOwner");

        // Create properties
        Property testProperty0 = new Property("testProperty0","testOwner", "testPropertyValue0");
        Property testProperty1 = new Property("testProperty1","testOwner", "testPropertyValue1");
        List<Property> properties = List.of(testProperty0, testProperty1);
        propertyManager.create(properties);
        testChannel0.setProperties(properties);

        // Create tag
        Tag tag = new Tag("testTag", "testOwner");
        tagManager.create(List.of(tag));
        testChannel0.setTags(List.of(tag));

        // Create a simple channel
        Channel createdChannel0 = channelManager.create(testChannel0.getName(), testChannel0);

        Channel expectedChannel = new Channel("testChannel0", "testOwner");

        // Create properties
        Property expProperty0 = new Property("testProperty0",null, "testPropertyValue0");
        Property expProperty1 = new Property("testProperty1",null, "testPropertyValue1");
        expectedChannel.setProperties(List.of(expProperty0, expProperty1));

        // Create tag
        Tag expTag = new Tag("testTag", null);
        expectedChannel.setTags(List.of(expTag));

        PVAURI uri = new PVAURI("uriname", "pva", "auth", ChannelFinderEpicsService.SERVICE_DESC, Map.of("_name", "*") );
        try {
            PVAStructure result = pvaChannel.invoke(uri.cloneData()).get(30,TimeUnit.SECONDS);
            List<Channel> channels = NTXmlUtil.parse(result);
            logger.log(Level.INFO, () -> "Result channel list " + channels);
            Assertions.assertEquals(expectedChannel, channels.get(0));
        } catch (Exception e) {

            logger.log(Level.WARNING, e.getMessage(), e);
            Assertions.fail(e);
        }
    }
}
