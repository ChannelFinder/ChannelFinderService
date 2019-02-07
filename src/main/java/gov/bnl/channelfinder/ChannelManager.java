package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.CHANNEL_RESOURCE_URI;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Lists;

@RestController
@RequestMapping(CHANNEL_RESOURCE_URI)
@EnableAutoConfiguration
public class ChannelManager {

    // private SecurityContext securityContext;
    static Logger logManagerAudit = Logger.getLogger(ChannelManager.class.getName() + ".audit");
    static Logger log = Logger.getLogger(ChannelManager.class.getName());

    @Autowired
    private ServletContext servletContext;

    @Autowired
    TagRepository tagRepository;
    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ChannelRepository channelRepository;

    /**
     * GET method for retrieving a collection of Channel instances, based on a
     * multi-parameter query specifying patterns for tags, property values, and
     * channel names to match against.
     *
     * @return HTTP Response
     */
    @GetMapping
    public List<XmlChannel> query(@RequestParam MultiValueMap<String, String> allRequestParams) {
        return channelRepository.search(allRequestParams);
    }

    /**
     * GET method for retrieving an instance of Channel identified by
     * <tt>channelName</tt>.
     *
     * @param channelName channel name
     * @return XmlChannel identified with the name channelName
     */
    @GetMapping("/{channelName}")
    public XmlChannel read(@PathVariable("channelName") String channelName) {
        logManagerAudit.info("getting channel :" + channelName);
        if (channelRepository.findById(channelName).isPresent())
            return channelRepository.findById(channelName).get();
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The channel with the name " + channelName + " does not exist");
    }

    /**
     * PUT method for creating/replacing a channel instance identified by the
     * payload. The <b>complete</b> set of properties for the channel must be
     * supplied, which will replace the existing set of properties.
     *
     * @param chan name of channel to create or add
     * @param data new data (properties/tags) for channel <tt>chan</tt>
     * @return HTTP The created XmlChannel
     */
    @PutMapping("/{channelName}")
    public XmlChannel create(@PathVariable("channelName") String channelName, @RequestBody XmlChannel data) {
        logManagerAudit.info("PUT:" + data.toLog());
        // TODO Validate the authorization of the user
        // TODO Validate the channel
        return channelRepository.index(data);
    }

    /**
     * PUT method for creating multiple channel instances.
     *
     * @param data XmlChannels data (from payload)
     * @return A list of the created channels
     */
    @PutMapping
    public List<XmlChannel> create(@RequestBody List<XmlChannel> data) {

        data.stream().forEach(log -> {
            logManagerAudit.info("PUT" + log.toLog());
        });
        // TODO validate user authorization
        // TODO validate each channel
        // TODO this is an extra copy because the CF API contract was a List and the
        // CRUDrepository contract is an Iterable. In the future one of the two should
        // be changed.
        return Lists.newArrayList(channelRepository.indexAll(data));
    }

    /**
     * POST method for merging properties and tags of the Channel identified by the
     * payload into an existing channel.
     *
     * @param channelName name of channel to add
     * @param data        new XmlChannel data (properties/tags) to be merged into
     *                    channel <tt>channelName</tt>
     */
    @PostMapping("/{channelName}")
    public XmlChannel update(@PathVariable("channelName") String channelName, @RequestBody XmlChannel data) {
        long start = System.currentTimeMillis();
        if (data.getName() == null || data.getName().isEmpty()) {
        }
//        if (!validateChannelName(channelName, data)) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified channel name '" + channelName
//                    + "' and payload channel name '" + data.getName() + "' do not match");
//        }
        try {
            start = System.currentTimeMillis();
            // TODO validate channel
            logManagerAudit.info("|" + servletContext.getContextPath() + "|POST|validation : "
                    + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            Optional<XmlChannel> foundChannel = channelRepository.findById(channelName);
            if (foundChannel.isPresent()) {
                return channelRepository.save(data);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The channel with the name " + channelName + " does not exist");
            }
        } catch (Exception e) {

        } finally {
        }
        return null;
    }

    /**
     * POST method for merging properties and tags of the Channels identified by the
     * payload into existing channels.
     *
     * @param data        new XmlChannel data (properties/tags) to be merged into
     *                    channel <tt>channelName</tt>
     */
    @PostMapping()
    public List<XmlChannel> update(@RequestBody List<XmlChannel> data) {
        long start = System.currentTimeMillis();
        // TODO check user authorization
        // TODO validate the channels
        try {
            // check if all the channels exist
            for (XmlChannel xmlChannel : data) {
                Optional<XmlChannel> foundChannel = channelRepository.findById(xmlChannel.getName());
                if (!foundChannel.isPresent()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "The channel with the name " + xmlChannel.getName() + " does not exist");
                }
            }
            start = System.currentTimeMillis();
            logManagerAudit.info("|" + servletContext.getContextPath() + "|POST|validation : "
                    + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            return Lists.newArrayList(channelRepository.saveAll(data));
        } catch (Exception e) {

        } finally {
        }
        return null;
    }

    /**
     * DELETE method for deleting a channel instance identified by path parameter
     * <tt>channelName</tt>.
     *
     * @param channelName channel to remove
     */
    @DeleteMapping("/{channelName}")
    public void remove(@PathVariable("channelName") String channelName) {
        logManagerAudit.info("deleting ch:" + channelName);
        // TODO check authorization
        // TODO validate the channel
        channelRepository.deleteById(channelName);
    }
}
