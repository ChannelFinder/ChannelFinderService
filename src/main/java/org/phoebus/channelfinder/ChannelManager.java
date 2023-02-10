package org.phoebus.channelfinder;

import static org.phoebus.channelfinder.CFResourceDescriptors.CHANNEL_RESOURCE_URI;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import com.google.common.collect.Lists;
import org.phoebus.channelfinder.AuthorizationService.ROLES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
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

@CrossOrigin
@RestController
@RequestMapping(CHANNEL_RESOURCE_URI)
@EnableAutoConfiguration
public class ChannelManager {

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
     * @param allRequestParams query parameters
     * @return list of all channels
     */
    @GetMapping
    public List<XmlChannel> query(@RequestParam MultiValueMap<String, String> allRequestParams) {
        return channelRepository.search(allRequestParams);
    }

    @GetMapping("/count")
    public long queryCount(@RequestParam MultiValueMap<String, String> allRequestParams) {
        return channelRepository.count(allRequestParams);
    }

    /**
     * GET method for retrieving an instance of Channel identified by
     * <code>channelName</code>.
     *
     * @param channelName - channel name to search for
     * @return found channel
     */
    @GetMapping("/{channelName}")
    public XmlChannel read(@PathVariable("channelName") String channelName) {
        channelManagerAudit.info(MessageFormat.format(TextUtil.FIND_CHANNEL, channelName));

        Optional<XmlChannel> foundChannel = channelRepository.findById(channelName);
        if (foundChannel.isPresent())
            return foundChannel.get();
        else {
            String message = MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channelName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    /**
     * PUT method for creating/replacing a channel instance identified by the
     * payload. The <b>complete</b> set of properties for the channel must be
     * supplied, which will replace the existing set of properties.
     *
     * @param channelName - name of channel to be created 
     * @param channel - new data (properties/tags) for channel <code>chan</code>
     * @return the created channel
     */
    @PutMapping("/{channelName}")
    public XmlChannel create(@PathVariable("channelName") String channelName, @RequestBody XmlChannel channel) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
            channelManagerAudit.info(MessageFormat.format(TextUtil.CREATE_CHANNEL, channel.toLog()));
            // Validate request parameters
            validateChannelRequest(channel);

            // check if authorized owner
            if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), channel)) {
                String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channel.toLog());
                log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
            }
            Optional<XmlChannel> existingChannel = channelRepository.findById(channelName);
            boolean present = existingChannel.isPresent();
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingChannel.get())) {
                    String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, existingChannel.get().toLog());
                    log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                } 
                // delete existing channel
                channelRepository.deleteById(channelName);
            } 

            // reset owners of attached tags/props back to existing owners
            channel.getProperties().forEach(prop -> prop.setOwner(propertyRepository.findById(prop.getName()).get().getOwner()));
            channel.getTags().forEach(tag -> tag.setOwner(tagRepository.findById(tag.getName()).get().getOwner()));

            // create new channel
            return channelRepository.index(channel);
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channelName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
    }

    /**
     * PUT method for creating multiple channels.
     *
     * @param channels - XmlChannels to be created
     * @return the list of channels created
     */
    @PutMapping
    public Iterable<XmlChannel> create(@RequestBody Iterable<XmlChannel> channels) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {           
            // check if authorized owner
            for(XmlChannel channel: channels) {

                Optional<XmlChannel> existingChannel = channelRepository.findById(channel.getName());
                boolean present = existingChannel.isPresent();
                if(present) {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingChannel.get())) {
                        String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, existingChannel.get().toLog());
                        log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                    } 
                    channel.setOwner(existingChannel.get().getOwner());
                } else {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), channel)) {
                        String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channel.toLog());
                        log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                    }
                }
            }

            // Validate request parameters
            validateChannelRequest(channels);

            // delete existing channels
            for(XmlChannel channel: channels) {
                if(channelRepository.existsById(channel.getName())) {
                    // delete existing channel
                    channelRepository.deleteById(channel.getName());
                } 
            }

            // reset owners of attached tags/props back to existing owners
            for(XmlChannel channel: channels) {
                channel.getProperties().forEach(prop -> prop.setOwner(propertyRepository.findById(prop.getName()).get().getOwner()));            
                channel.getTags().forEach(tag -> tag.setOwner(tagRepository.findById(tag.getName()).get().getOwner()));
            }

            channels.forEach(log ->
                channelManagerAudit.info(MessageFormat.format(TextUtil.CREATE_CHANNEL, log.toLog()))
            );

            // create new channels
            return channelRepository.indexAll(Lists.newArrayList(channels));
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNELS, channels);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
    }

    /**
     * POST method for merging properties and tags of the channel identified by the
     * payload into an existing channel.
     *
     * @param channelName - name of channel to add
     * @param channel - new XmlChannel data (properties/tags) to be merged into channel <code>channelName</code>
     * @return the updated channel         
     */
    @PostMapping("/{channelName}")
    public XmlChannel update(@PathVariable("channelName") String channelName, @RequestBody XmlChannel channel) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
            long start = System.currentTimeMillis();
            channelManagerAudit.info(MessageFormat.format(TextUtil.PATH_POST_VALIDATION_TIME, servletContext.getContextPath(), (System.currentTimeMillis() - start)));
            // Validate request parameters
            validateChannelRequest(channel);

            // check if authorized owner
            if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), channel)) {
                String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channel.toLog());
                log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
            }
            Optional<XmlChannel> existingChannel = channelRepository.findById(channelName);
            boolean present = existingChannel.isPresent();

            XmlChannel newChannel;
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingChannel.get())) {
                    String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, existingChannel.get().toLog());
                    log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                }
                newChannel = existingChannel.get();
                newChannel.setOwner(channel.getOwner());
                newChannel.addProperties(channel.getProperties());
                newChannel.addTags(channel.getTags());
                // Is an existing channel being renamed
                if (!channel.getName().equalsIgnoreCase(existingChannel.get().getName())) {
                    // Since this is a rename operation we will need to remove the old channel.
                    channelRepository.deleteById(existingChannel.get().getName());
                    newChannel.setName(channel.getName());
                }
            } else {
                newChannel = channel;
            }

            // reset owners of attached tags/props back to existing owners
            channel.getProperties().forEach(prop -> prop.setOwner(propertyRepository.findById(prop.getName()).get().getOwner()));
            channel.getTags().forEach(tag -> tag.setOwner(tagRepository.findById(tag.getName()).get().getOwner()));

            // update channel
            return channelRepository.save(newChannel);
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channelName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
    }

    /**
     * POST method for merging properties and tags of the Channels identified by the
     * payload into existing channels.
     *
     * @param channels - XmlChannels to be updated
     * @return the updated channels
     */
    @PostMapping()
    public Iterable<XmlChannel> update(@RequestBody Iterable<XmlChannel> channels) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
            long start = System.currentTimeMillis();

            for(XmlChannel channel: channels) {                
                Optional<XmlChannel> existingChannel = channelRepository.findById(channel.getName());
                boolean present = existingChannel.isPresent();
                if(present) {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingChannel.get())) {
                        String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, existingChannel.get().toLog());
                        log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                    }
                    channel.setOwner(existingChannel.get().getOwner());
                } else {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), channel)) {
                        String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channel.toLog());
                        log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                    }
                }
            }

            // Validate request parameters
            validateChannelRequest(channels);   

            start = System.currentTimeMillis();
            channelManagerAudit.info(MessageFormat.format(TextUtil.PATH_POST_VALIDATION_TIME, servletContext.getContextPath(), (System.currentTimeMillis() - start)));

            // reset owners of attached tags/props back to existing owners
            for(XmlChannel channel: channels) {
                channel.getProperties().forEach(prop -> prop.setOwner(propertyRepository.findById(prop.getName()).get().getOwner()));            
                channel.getTags().forEach(tag -> tag.setOwner(tagRepository.findById(tag.getName()).get().getOwner()));
            }

            // update channels
            return channelRepository.saveAll(channels);
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNELS, channels);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
    }

    /**
     * DELETE method for deleting a channel instance identified by path parameter
     * <code>channelName</code>.
     *
     * @param channelName - name of channel to remove
     */
    @DeleteMapping("/{channelName}")
    public void remove(@PathVariable("channelName") String channelName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_CHANNEL)) {
            channelManagerAudit.info(MessageFormat.format(TextUtil.DELETE_CHANNEL, channelName));

            Optional<XmlChannel> existingChannel = channelRepository.findById(channelName);
            if(existingChannel.isPresent()) {
                // check if authorized owner
                if(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingChannel.get())) {
                    // delete channel
                    channelRepository.deleteById(channelName);
                } else {
                    String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channelName);
                    log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                }
            } else {
                String message = MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channelName);
                log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_CHANNEL, channelName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
    }

    /**
     * Check that the existing channel and the channel in the request body match
     * 
     * @param existing
     * @param request
     * @return
     */
    boolean validateChannel(XmlChannel existing, XmlChannel request) {
        return existing.getName().equals(request.getName());
    }

    /**
     * Checks if
     * 1. the channel name is not null and matches the name in the body
     * 2. the channel owner is not null or empty
     * 3. all the listed tags/props exist and prop value is not null or empty
     * 
     * @param channel channel to be validated
     */
    public void validateChannelRequest(XmlChannel channel) {
        // 1 
        if (channel.getName() == null || channel.getName().isEmpty()) {
            String message = MessageFormat.format(TextUtil.CHANNEL_NAME_CANNOT_BE_NULL_OR_EMPTY, channel.toLog());
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
        }
        // 2
        if (channel.getOwner() == null || channel.getOwner().isEmpty()) {
            String message = MessageFormat.format(TextUtil.CHANNEL_OWNER_CANNOT_BE_NULL_OR_EMPTY, channel.toLog());
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
        }
        // 3 
        List <String> tagNames = channel.getTags().stream().map(XmlTag::getName).collect(Collectors.toList());
        for(String tagName:tagNames) {
            if(!tagRepository.existsById(tagName)) {
                String message = MessageFormat.format(TextUtil.TAG_NAME_DOES_NOT_EXIST, tagName);
                log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
        }
        // 3 
        List <String> propertyNames = channel.getProperties().stream().map(XmlProperty::getName).collect(Collectors.toList());
        List <String> propertyValues = channel.getProperties().stream().map(XmlProperty::getValue).collect(Collectors.toList());
        for(String propertyName:propertyNames) {
            if(!propertyRepository.existsById(propertyName)) {
                String message = MessageFormat.format(TextUtil.PROPERTY_NAME_DOES_NOT_EXIST, propertyName);
                log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            } 
        }
        for(String propertyValue:propertyValues) {
            if(propertyValue == null || propertyValue.isEmpty()) {
                String message = MessageFormat.format(TextUtil.PROPERTY_VALUE_NULL_OR_EMPTY, propertyNames.get(propertyValues.indexOf(propertyValue)), propertyValue);
                log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
        }
    }

    /**
     * Checks if
     * 1. the tag names are not null
     * 2. the tag owners are not null or empty
     * 3. all the channels exist
     * 
     * @param channels list of channels to be validated
     */
    public void validateChannelRequest(Iterable<XmlChannel> channels) {
        for(XmlChannel channel: channels) {
            validateChannelRequest(channel);
        }
    }

}
