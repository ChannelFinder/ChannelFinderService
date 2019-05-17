package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.CHANNEL_RESOURCE_URI;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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

import gov.bnl.channelfinder.AuthorizationService.ROLES;

@RestController
@RequestMapping(CHANNEL_RESOURCE_URI)
@EnableAutoConfiguration
public class ChannelManager {

    // private SecurityContext securityContext;
    static Logger channelManagerAudit = Logger.getLogger(ChannelManager.class.getName() + ".audit");
    static Logger log = Logger.getLogger(ChannelManager.class.getName());

    @Autowired
    private ServletContext servletContext;

    @Autowired
    TagRepository tagRepository;
    
    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    AuthorizationService authorizationService;

    /**
     * GET method for retrieving a collection of Channel instances, based on a
     * multi-parameter query specifying patterns for tags, property values, and
     * channel names to match against.
     *
     * @return list of channels
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
        channelManagerAudit.info("getting channel: " + channelName);
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
     * @param channelName - name of channel to be created 
     * @param channel - new data (properties/tags) for channel <tt>chan</tt>
     * @return the created channel
     */
    @PutMapping("/{channelName}")
    public XmlChannel create(@PathVariable("channelName") String channelName, @RequestBody XmlChannel channel) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
            channelManagerAudit.info("PUT:" + channel.toLog());
            // TODO Validate the channel
            return channelRepository.index(channel);
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this channel: " + channelName, null);
    }

    /**
     * PUT method for creating multiple channels.
     *
     * @param channels - XmlChannels to be created
     * @return the list of channels created
     */
    @PutMapping
    public Iterable<XmlChannel> create(@RequestBody List<XmlChannel> channels) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
            channels.stream().forEach(log -> {
                channelManagerAudit.info("PUT" + log.toLog());
            });
            // TODO validate each channel
            return channelRepository.indexAll(channels);
            // return Lists.newArrayList(channelRepository.indexAll(data));
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on these channels: " + channels, null);
    }

    /**
     * POST method for merging properties and tags of the channel identified by the
     * payload into an existing channel.
     *
     * @param channelName - name of channel to add
     * @param channel - new XmlChannel data (properties/tags) to be merged into channel <tt>channelName</tt>
     * @return the updated channel         
     */
    @PostMapping("/{channelName}")
    public XmlChannel update(@PathVariable("channelName") String channelName, @RequestBody XmlChannel channel) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
            long start = System.currentTimeMillis();
            if (channel.getName() == null || channel.getName().isEmpty()) {
            }
            //        if (!validateChannelName(channelName, data)) {
            //            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified channel name '" + channelName
            //                    + "' and payload channel name '" + data.getName() + "' do not match");
            //        }
            try {
                start = System.currentTimeMillis();
                // TODO validate channel
                channelManagerAudit.info("|" + servletContext.getContextPath() + "|POST|validation : "
                        + (System.currentTimeMillis() - start));
                start = System.currentTimeMillis();
                Optional<XmlChannel> foundChannel = channelRepository.findById(channelName);
                if (foundChannel.isPresent()) {
                    return channelRepository.save(channel);
                } else {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "The channel with the name " + channelName + " does not exist");
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The channel with the name " + channelName + " does not exist");
            } 
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this channel: " + channelName, null);
    }

    /**
     * POST method for merging properties and tags of the Channels identified by the
     * payload into existing channels.
     *
     * @param channels - XmlChannels to be updated
     * @result the updated channels
     */
    @PostMapping()
    public Iterable<XmlChannel> update(@RequestBody List<XmlChannel> channels) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
            long start = System.currentTimeMillis();
            // TODO check user authorization
            // TODO validate the channels
            try {
                // check if all the channels exist
                for (XmlChannel xmlChannel : channels) {
                    Optional<XmlChannel> foundChannel = channelRepository.findById(xmlChannel.getName());
                    if (!foundChannel.isPresent()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "The channel with the name " + xmlChannel.getName() + " does not exist");
                    }
                }
                start = System.currentTimeMillis();
                channelManagerAudit.info("|" + servletContext.getContextPath() + "|POST|validation : "
                        + (System.currentTimeMillis() - start));
                start = System.currentTimeMillis();
                return channelRepository.saveAll(channels);
                //return Lists.newArrayList(channelRepository.saveAll(data));
            } catch (Exception e) {

            } finally {
            }
            return null;
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on these channels: " + channels, null);
    }

    /**
     * DELETE method for deleting a channel instance identified by path parameter
     * <tt>channelName</tt>.
     *
     * @param channelName - name of channel to remove
     */
    @DeleteMapping("/{channelName}")
    public void remove(@PathVariable("channelName") String channelName) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
            channelManagerAudit.info("deleting ch:" + channelName);
            // TODO validate the channel
            channelRepository.deleteById(channelName);
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this channel: " + channelName, null);
    }

}
