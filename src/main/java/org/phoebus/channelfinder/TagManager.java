package org.phoebus.channelfinder;

import static org.phoebus.channelfinder.CFResourceDescriptors.TAG_RESOURCE_URI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.Lists;
import org.phoebus.channelfinder.AuthorizationService.ROLES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping(TAG_RESOURCE_URI)
@EnableAutoConfiguration
public class TagManager {

    static Logger tagManagerAudit = Logger.getLogger(TagManager.class.getName() + ".audit");
    static Logger log = Logger.getLogger(TagManager.class.getName());

    @Autowired
    TagRepository tagRepository;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    AuthorizationService authorizationService;

    /**
     * GET method for retrieving the list of tags in the database.
     *
     * @return list of all tags
     */
    @GetMapping
    public Iterable<XmlTag> list() {
        return tagRepository.findAll();
    }

    /**
     * GET method for retrieving the tag with the path parameter <code>tagName</code>
     * 
     * To get all its channels use the parameter "withChannels"
     *
     * @param tagName - tag name to search for
     * @param withChannels - channels with the tag tagName
     * @return found tag
     */
    @GetMapping("/{tagName}")
    public XmlTag read(@PathVariable("tagName") String tagName,
            @RequestParam(value = "withChannels", defaultValue = "true") boolean withChannels) {
        tagManagerAudit.info("getting tag: " + tagName);

        if(withChannels) {
            Optional<XmlTag> foundTag = tagRepository.findById(tagName,true);
            if(foundTag.isPresent()) {
                return foundTag.get();
            } else {
                log.log(Level.SEVERE, "The tag with the name " + tagName + " does not exist", new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The tag with the name " + tagName + " does not exist");
            }
        } else {
            Optional<XmlTag> foundTag = tagRepository.findById(tagName);
            if(foundTag.isPresent()) {
                return foundTag.get();
            } else {
                log.log(Level.SEVERE, "The tag with the name " + tagName + " does not exist", new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The tag with the name " + tagName + " does not exist");
            }
        }
    }

    /**
     * PUT method to create and <b>exclusively</b> update the tag identified by the
     * path parameter <code>name</code> to all channels identified in the payload
     * structure <code>data</code>. Setting the owner attribute in the XML root element
     * is mandatory.
     * 
     * @param tagName - name of tag to be created
     * @param tag - XmlTag structure containing the list of channels to be tagged
     * @return the created tag
     */
    @PutMapping("/{tagName}")
    public XmlTag create(@PathVariable("tagName") String tagName, @RequestBody XmlTag tag) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validateTagRequest(tag);

            // check if authorized owner
            if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tag.toLog(), new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
            }
            Optional<XmlTag> existingTag = tagRepository.findById(tagName);
            boolean present = existingTag.isPresent();
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + existingTag.get().toLog(), new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + existingTag.get(), null);
                } 
                // delete existing tag
                tagRepository.deleteById(tagName);
            } 

            // create new tag
            XmlTag createdTag = tagRepository.index(tag);

            if (!tag.getChannels().isEmpty()) {
                tag.getChannels().forEach(chan -> chan.addTag(createdTag));
                // update the listed channels in the tag's payloads with the new tag
                Iterable<XmlChannel> chans = channelRepository.saveAll(tag.getChannels());
                List<XmlChannel> chanList = new ArrayList<XmlChannel>();
                for (XmlChannel chan : chans) {
                    chanList.add(chan);
                }
                createdTag.setChannels(chanList);
            }
            return createdTag;
        } else
            log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tag.toLog(), new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
    }

    /**
     * PUT method for creating multiple tags.
     * 
     * @param tags - XmlTags to be created
     * @return the list of tags created
     */
    @PutMapping()
    public Iterable<XmlTag> create(@RequestBody Iterable<XmlTag> tags) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));            

            // check if authorized owner
            for(XmlTag tag: tags) {       
                Optional<XmlTag> existingTag = tagRepository.findById(tag.getName());
                boolean present = existingTag.isPresent();
                if(present) {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                        log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + existingTag.get().toLog(), new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "User does not have the proper authorization to perform an operation on this tag: " + existingTag, null);
                    }
                    tag.setOwner(existingTag.get().getOwner());
                } else {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                        log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tag.toLog(), new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
                    }
                }
            }

            // Validate request parameters
            validateTagRequest(tags);

            // delete existing tags
            for(XmlTag tag: tags) {
                if(tagRepository.existsById(tag.getName())) {
                    // delete existing tag
                    tagRepository.deleteById(tag.getName());
                } 
            }

            // create new tags
            Iterable<XmlTag> createdTags = tagRepository.indexAll(Lists.newArrayList(tags));

            // update the listed channels in the tags' payloads with new tags
            Map<String, XmlChannel> channels = new HashMap<String, XmlChannel>();
            for (XmlTag tag : tags) {
                for (XmlChannel channel : tag.getChannels()) {
                    if (channels.get(channel.getName()) != null) {
                        channels.get(channel.getName()).addTag(new XmlTag(tag.getName(), tag.getOwner()));
                    } else {
                        channel.addTag(new XmlTag(tag.getName(), tag.getOwner()));
                        channels.put(channel.getName(), channel);
                    }
                }
            }

            if(!channels.isEmpty()) {
                Iterable<XmlChannel> chans = channelRepository.saveAll(channels.values());
            }
            // TODO should return created tags with properly organized saved channels, but it would be very complicated...
            return tags;
        } else {
            log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on these tags: " + tags, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on these tags: " + tags, null);
        }
    }

    /**
     * PUT method for adding the tag identified by <code>tag</code> to the single
     * channel <code>chan</code> (both path parameters).
     * 
     * TODO: could be simplified with multi index update and script which can use
     * wildcards thus removing the need to explicitly define the entire tag
     * 
     * @param tagName - name of tag to be added to channel
     * @param channelName - channel to update <code>tag</code> to
     * @return added tag
     */
    @PutMapping("/{tagName}/{channelName}")
    public XmlTag addSingle(@PathVariable("tagName") String tagName, @PathVariable("channelName") String channelName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validateTagWithChannelRequest(channelName);

            // check if authorized owner
            Optional<XmlTag> existingTag = tagRepository.findById(tagName);
            boolean present = existingTag.isPresent();
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + existingTag.get().toLog(), new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + existingTag.get(), null);
                } 
                // add tag to channel
                XmlChannel channel = channelRepository.findById(channelName).get();
                channel.addTag(existingTag.get());
                XmlChannel taggedChannel = channelRepository.save(channel);
                XmlTag addedTag = existingTag.get();
                addedTag.setChannels(Arrays.asList(taggedChannel));
                return addedTag;
            } else {
                log.log(Level.SEVERE, "The tag with the name " + tagName + " does not exist", new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The tag with the name " + tagName + " does not exist");
            }
        } else {
            log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tagName, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + tagName, null);
        }
    }

    /**
     * POST method to update the tag identified by the path parameter
     * <code>name</code>, adding it to all channels identified by the channels inside
     * the payload structure <code>data</code>. Setting the owner attribute in the XML
     * root element is mandatory.
     * 
     * TODO: Optimize the bulk channel update
     *
     * @param tagName - name of tag to be updated
     * @param tag - XmlTag with list of channels to addSingle the tag <code>name</code> to
     * @return the updated tag
     */
    @PostMapping("/{tagName}")
    public XmlTag update(@PathVariable("tagName") String tagName, @RequestBody XmlTag tag) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validateTagRequest(tag);

            // check if authorized owner
            if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tag.toLog(), new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
            }
            List<XmlChannel> channels = new ArrayList<XmlChannel>();
            Optional<XmlTag> existingTag = tagRepository.findById(tagName,true);

            XmlTag newTag;
            if(existingTag.isPresent()) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + existingTag.get().toLog(), new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + existingTag.get(), null);
                } 
                channels = existingTag.get().getChannels();
                newTag = existingTag.get();
                newTag.setOwner(tag.getOwner());
                // Is an existing tag being renamed
                if (!tag.getName().equalsIgnoreCase(existingTag.get().getName())) {
                    // Since this is a rename operation we will need to remove the old channel.
                    tagRepository.deleteById(existingTag.get().getName());
                    newTag.setName(tag.getName());
                }
            } else {
                newTag = tag;
            }

            // update tag
            XmlTag updatedTag = tagRepository.save(newTag);

            // update channels of existing tag
            if(!channels.isEmpty()) {
                channels.forEach(chan -> chan.addTag(updatedTag));
            }

            // update the listed channels in the tag's payload with the updated tag
            if(!tag.getChannels().isEmpty()) {
                tag.getChannels().forEach(c -> c.addTag(updatedTag));
                // update the listed channels in the tag's payloads with the new tag
                channels.addAll(tag.getChannels());
            }

            if(!channels.isEmpty()) {
                Iterable<XmlChannel> updatedChannels = channelRepository.saveAll(channels);
                updatedTag.setChannels(StreamSupport.stream(updatedChannels.spliterator(), false)
                        .collect(Collectors.toList()));
            }

            return updatedTag;
        } else {
            log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tagName, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + tagName, null);
        }
    }

    /**
     * POST method for updating multiple tags and updating all the appropriate
     * channels.
     * 
     * If the channels don't exist it will fail
     *
     * @param tags - XmlTags to be updated
     * @return the updated tags
     */
    @PostMapping()
    public Iterable<XmlTag> update(@RequestBody Iterable<XmlTag> tags) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));

            // check if authorized owner
            for(XmlTag tag:tags) {
                Optional<XmlTag> existingTag = tagRepository.findById(tag.getName());
                boolean present = existingTag.isPresent();
                if(present) {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                        log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + existingTag.get().toLog(), new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "User does not have the proper authorization to perform an operation on this tag: " + existingTag.get(), null);
                    }
                    tag.setOwner(existingTag.get().getOwner());
                } else {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                        log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tag.toLog(), new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
                    }
                }
            }

            // Validate request parameters
            validateTagRequest(tags);

            // update the listed channels in the tags' payloads with new tags
            Map<String, XmlChannel> channels = new HashMap<String, XmlChannel>();
            for (XmlTag tag : tags) {
                for (XmlChannel channel : tag.getChannels()) {
                    if (channels.get(channel.getName()) != null) {
                        channels.get(channel.getName()).addTag(new XmlTag(tag.getName(), tag.getOwner()));
                    } else {
                        channel.addTag(new XmlTag(tag.getName(), tag.getOwner()));
                        channels.put(channel.getName(), channel);
                    }
                }
            }

            // update tags
            Iterable<XmlTag> updatedTags = tagRepository.saveAll(tags);

            // update channels
            if(!channels.isEmpty()) {
                channelRepository.saveAll(channels.values());
            }
            // TODO should return updated tags with properly organized saved channels, but it would be very complicated...
            return tags;
        } else {
            log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on these tags: " + tags, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on these tag: " + tags, null);
        }
    }

    /**
     * DELETE method for deleting the tag identified by the path parameter
     * <code>tagName</code> from all channels.
     *
     * @param tagName - name of tag to remove
     */
    @DeleteMapping("/{tagName}")
    public void remove(@PathVariable("tagName") String tagName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            Optional<XmlTag> existingTag = tagRepository.findById(tagName);
            if(existingTag.isPresent()) {
                // check if authorized owner
                if(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    // delete tag
                    tagRepository.deleteById(tagName);
                } else {
                    log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tagName, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + tagName, null);
                }
            } else {
                log.log(Level.SEVERE, "The tag with the name " + tagName + " does not exist", new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The tag with the name " + tagName + " does not exist");
            }
        } else {
            log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tagName, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + tagName, null);
        }
    }

    /**
     * DELETE method for deleting the tag identified by <code>tagName</code> from the
     * channel <code>channelName</code> (both path parameters).
     *
     * @param tagName - name of tag to remove
     * @param channelName - channel to remove <code>tagName</code> from
     */
    @DeleteMapping("/{tagName}/{channelName}")
    public void removeSingle(@PathVariable("tagName") final String tagName, @PathVariable("channelName") String channelName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            Optional<XmlTag> existingTag = tagRepository.findById(tagName);
            if(existingTag.isPresent()) {
                // check if authorized owner
                if(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    Optional<XmlChannel> ch = channelRepository.findById(channelName);
                    if(ch.isPresent()) {
                        // remove tag from channel
                        XmlChannel channel = ch.get();
                        channel.removeTag(new XmlTag(tagName, ""));
                        channelRepository.index(channel);
                    } else {
                        log.log(Level.SEVERE, "The channel with the name " + channelName + " does not exist", new ResponseStatusException(HttpStatus.NOT_FOUND));
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "The channel with the name " + channelName + " does not exist");
                    }
                } else {
                    log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tagName, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + tagName, null); 
                }
            } else {
                log.log(Level.SEVERE, "The tag with the name " + tagName + " does not exist", new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The tag with the name " + tagName + " does not exist");
            }
        } else {
            log.log(Level.SEVERE, "User does not have the proper authorization to perform an operation on this tag: " + tagName, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + tagName, null);       
        }
    }

    /**
     * Checks if all the tags included satisfy the following conditions
     * 1. the tag names are not null
     * 2. the tag owners are not null or empty
     * 3. all the channels exist
     * 
     * @param tags the list of tags to be validated
     */
    public void validateTagRequest(Iterable<XmlTag> tags) {
        for(XmlTag tag: tags) {
            validateTagRequest(tag);
        }
    }

    /**
     * Checks if tag satisfies the following conditions
     * 1. the tag name is not null and matches the name in the body
     * 2. the tag owner is not null or empty
     * 3. all the listed channels exist
     * 
     * @param tag the tag to be validates
     */
    public void validateTagRequest(XmlTag tag) {
        // 1 
        if (tag.getName() == null || tag.getName().isEmpty()) {
            log.log(Level.SEVERE, "The tag name cannot be null or empty " + tag.toLog(), new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The tag name cannot be null or empty " + tag.toString(), null);
        }
        // 2
        if (tag.getOwner() == null || tag.getOwner().isEmpty()) {
            log.log(Level.SEVERE, "The tag owner cannot be null or empty " + tag.toLog(), new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The tag owner cannot be null or empty " + tag.toString(), null);
        }
        // 3
        List <String> channelNames = tag.getChannels().stream().map(XmlChannel::getName).collect(Collectors.toList());
        for(String channelName:channelNames) {
            if(!channelRepository.existsById(channelName)) {
                log.log(Level.SEVERE, "The channel with the name " + channelName + " does not exist", new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The channel with the name " + channelName + " does not exist");
            }
        }

    }

    /**
     * Checks if channel with name "channelName" exists
     * @param channelName check channel exists
     */
    public void validateTagWithChannelRequest(String channelName) {
        if(!channelRepository.existsById(channelName)) {
            log.log(Level.SEVERE, "The channel with the name " + channelName + " does not exist", new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The channel with the name " + channelName + " does not exist");
        }
    }
}
