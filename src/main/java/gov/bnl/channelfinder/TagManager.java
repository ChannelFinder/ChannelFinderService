package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_TAG_TYPE;
import static gov.bnl.channelfinder.CFResourceDescriptors.TAG_RESOURCE_URI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.RequestOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping(TAG_RESOURCE_URI)
@EnableAutoConfiguration
public class TagManager {

    // private SecurityContext securityContext;
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
     * @param map 
     *
     * @return list of all tags
     */
    @GetMapping
    public Iterable<XmlTag> list() {
        return tagRepository.findAll();
    }

    /**
     * GET method for retrieving the tag with the path parameter <tt>tagName</tt>
     * 
     * To get all its channels use the parameter "withChannels"
     *
     * @param tagName - tag name to search for
     * @return found tag
     */
    @GetMapping("/{tagName}")
    public XmlTag read(@PathVariable("tagName") String tagName,
            @RequestParam(value = "withChannels", defaultValue = "true") boolean withChannels) {
        tagManagerAudit.info("getting tag: " + tagName);
        Optional<XmlTag> foundTag = tagRepository.findById(tagName);
        if(foundTag.isPresent()) {
            return foundTag.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The tag with the name " + tagName + " does not exist");
        }
    }

    /**
     * PUT method to create and <b>exclusively</b> update the tag identified by the
     * path parameter <tt>name</tt> to all channels identified in the payload
     * structure <tt>data</tt>. Setting the owner attribute in the XML root element
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
            Optional<XmlTag> existingTag = tagRepository.findById(tagName);
            boolean present = existingTag.isPresent();
            if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
            }
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + existingTag, null);
                } 
                // delete existing tag
                tagRepository.deleteById(tagName);
            } 

            // create new tag
            XmlTag createdTag = tagRepository.index(tag);

            if(!tag.getChannels().isEmpty()) {
                // update the listed channels in the tag's payloads with the new tag
                channelRepository.saveAll(tag.getChannels());
            }
            return createdTag;
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
    }

    /**
     * PUT method for creating multiple tags.
     * 
     * @param testTags - XmlTags to be created
     * @return the list of tags created
     */
    @PutMapping()
    public Iterable<XmlTag> create(@RequestBody Iterable<XmlTag> testTags) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validateTagRequest(testTags);

            // check if authorized owner
            for(XmlTag tag:testTags) {
                Optional<XmlTag> existingTag = tagRepository.findById(tag.getName());
                boolean present = existingTag.isPresent();
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
                } 
                if(present) {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "User does not have the proper authorization to perform an operation on this tag: " + existingTag, null);
                    } 
                    // delete existing tag
                    tagRepository.deleteById(tag.getName());                
                } 
            }

            // create new tags
            Iterable<XmlTag> createdTags = tagRepository.indexAll(testTags);

            // update the listed channels in the tags' payloads with new tags
            List<XmlChannel> channels = new ArrayList<>();
            testTags.forEach(tag -> {
                channels.addAll(tag.getChannels());
            });
            if(!channels.isEmpty()) {
                channelRepository.saveAll(channels);
            }
            return (List)createdTags;    
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on these tags: " + testTags, null);      
    }

    /**
     * PUT method for adding the tag identified by <tt>tag</tt> to the single
     * channel <tt>chan</tt> (both path parameters).
     * 
     * TODO: could be simplified with multi index update and script which can use
     * wildcards thus removing the need to explicitly define the entire tag
     * 
     * @param tagName - name of tag to be added to channel
     * @param channelName - channel to update <tt>tag</tt> to
     * @return added tag
     */
    @PutMapping("/{tagName}/{chName}")
    public XmlTag addSingle(@PathVariable("tagName") String tagName, @PathVariable("channelName") String channelName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validateTagRequest(channelName);

            // check if authorized owner
            Optional<XmlTag> existingTag = tagRepository.findById(tagName);
            boolean present = existingTag.isPresent();
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + existingTag, null);
                } 
                // add tag to channel
                XmlChannel channel = channelRepository.findById(channelName).get();
                channel.addTag(existingTag.get());
                channelRepository.save(channel);
                return existingTag.get();
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The tag with the name " + tagName + " does not exist");
            }
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + tagName, null);
    }

    /**
     * POST method to update the tag identified by the path parameter
     * <tt>name</tt>, adding it to all channels identified by the channels inside
     * the payload structure <tt>data</tt>. Setting the owner attribute in the XML
     * root element is mandatory.
     * 
     * TODO: Optimize the bulk channel update
     *
     * @param tagName - name of tag to be updated
     * @param tag - XmlTag with list of channels to addSingle the tag <tt>name</tt> to
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
            Optional<XmlTag> existingTag = tagRepository.findById(tagName);
            boolean present = existingTag.isPresent();
            if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
            }
            List<XmlChannel> chans = null;
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + existingTag, null);
                } 
                chans = existingTag.get().getChannels();
            } 
            
            // update tag
            XmlTag updatedTag = tagRepository.save(tagName,tag);

            // update the listed channels in the tag's payload with the updated tag
            if(!tag.getChannels().isEmpty()) {
                channelRepository.saveAll(tag.getChannels());
            }
            if(!chans.isEmpty()) {
                channelRepository.saveAll(chans);
            }
            return updatedTag;        
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + tagName, null);
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
            // Validate request parameters
            validateTagRequest(tags);

            // check if authorized owner
            for(XmlTag tag:tags) {
                Optional<XmlTag> existingTag = tagRepository.findById(tag.getName());
                boolean present = existingTag.isPresent();
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
                } 
                if(present) {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "User does not have the proper authorization to perform an operation on this tag: " + existingTag, null);
                    }               
                }          
            }

            // update tags
            Iterable<XmlTag> createdTags = tagRepository.saveAll(tags);

            // update the listed channels in the tags' payloads with the updated tags
            List<XmlChannel> channels = new ArrayList<>();
            tags.forEach(tag -> {
                channels.addAll(tag.getChannels());
            });
            if(!channels.isEmpty()) {
                channelRepository.saveAll(channels);
            }
            return createdTags;
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on these tag: " + tags, null);
    }

    /**
     * DELETE method for deleting the tag identified by the path parameter
     * <tt>tagName</tt> from all channels.
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
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + tagName, null);
                }
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The tag with the name " + tagName + " does not exist");
            }
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + tagName, null);
    }

    /**
     * DELETE method for deleting the tag identified by <tt>tagName</tt> from the
     * channel <tt>channelName</tt> (both path parameters).
     *
     * @param tagName - name of tag to remove
     * @param channelName - channel to remove <tt>tagName</tt> from
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
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "The channel with the name " + channelName + " does not exist");
                    }
                } else
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this tag: " + tagName, null); 
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The tag with the name " + tagName + " does not exist");
            }
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + tagName, null);       
    }

    /**
     * Check that the existing tag and the tag in the request body match
     * 
     * @param existing
     * @param request
     * @return
     */
    boolean validateTag(XmlTag existing, XmlTag request) {
        return existing.getName().equals(request.getName());
    }

    /**
     * Checks if
     * 1. the tag name is not null and matches the name in the body
     * 2. the tag owner is not null or empty
     * 3. all the listed channels exist
     * 
     * @param data
     */
    private void validateTagRequest(XmlTag testTag) {
        // 1 
        if (testTag.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The tag name cannot be null " + testTag.toString(), null);
        }
        // 2
        if (testTag.getOwner() == null || testTag.getOwner().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The tag owner cannot be null or empty " + testTag.toString(), null);
        }
        // 3
        List <String> channelNames = testTag.getChannels().stream().map(XmlChannel::getName).collect(Collectors.toList());
        for(String channelName:channelNames) {
            Optional<XmlChannel> ch = channelRepository.findById(channelName);
            if(!ch.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The channel with the name " + channelName + " does not exist");
            }
        }

    }

    /**
     * Checks if
     * 1. the tag names are not null
     * 2. the tag owners are not null or empty
     * 3. all the channels exist
     * 
     * @param data
     */
    private void validateTagRequest(Iterable<XmlTag> tags) {
        for(XmlTag tag: tags) {
            validateTagRequest(tag);
        }
    }

    /**
     * Checks if
     * 1. the tag name is not null
     * 2. the tag owner is not null or empty
     * 3. all the channel exist
     * 
     * @param data
     */
    private void validateTagRequest(String channelName) {
        Optional<XmlChannel> ch = channelRepository.findById(channelName);
        if(!ch.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The channel with the name " + channelName + " does not exist");
        }
    }
}
