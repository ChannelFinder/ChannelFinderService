package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.TAG_RESOURCE_URI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    public Iterable<XmlTag> list(Map<String, String> map) {
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
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validateTagRequest(tagName,tag);
            XmlTag createdTag = tagRepository.index(tag);
            // Updated the listed channels in the properties payload with new tag
            channelRepository.saveAll(tag.getChannels());
            return createdTag;
        } else
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
    public List<XmlTag> create(@RequestBody List<XmlTag> tags) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            validateTagRequest(tags);
            Iterable<XmlTag> createdTags = tagRepository.indexAll(tags);
            List<XmlChannel> channels = new ArrayList<>();
            tags.forEach(tag -> {
                channels.addAll(tag.getChannels());
            });
            channelRepository.saveAll(channels);
            return (List)createdTags;    
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on these tags: " + tags, null);      
    }

    /**
     * PUT method for adding the tag identified by <tt>tag</tt> to the single
     * channel <tt>chan</tt> (both path parameters).
     * 
     * TODO: could be simplified with multi index update and script which can use
     * wildcards thus removing the need to explicitly define the entire tag
     * 
     * @param tagName - name of tag to be created
     * @param channelName - channel to update <tt>tag</tt> to
     * @param tag - tag data
     * @return added tag
     */
    @PutMapping("/{tagName}/{chName}")
    public XmlTag addSingle(@PathVariable("tagName") String tagName, @PathVariable("channelName") String channelName, @RequestBody XmlTag tag) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            validateTagRequest(tagName, tag, channelName);
            XmlTag createdTag = tagRepository.index(tag);
            XmlChannel channel = channelRepository.findById(channelName).get();
            channel.addTag(tag);
            channelRepository.save(channel);
            return createdTag;        
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + tag, null);
    }

    /**
     * POST method to update the the tag identified by the path parameter
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
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            validateTagRequest(tagName,tag);
            XmlTag updatedTag = tagRepository.save(tag);
            // Updated the listed channels in tags with the associated tag
            channelRepository.saveAll(tag.getChannels());
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
    public Iterable<XmlTag> update(@RequestBody List<XmlTag> tags) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            validateTagRequest(tags);
            Iterable<XmlTag> createdTags = tagRepository.saveAll(tags);
            // Updated the listed channels in tags with the associated tags
            List<XmlChannel> channels = new ArrayList<>();
            tags.forEach(tag -> {
                channels.addAll(tag.getChannels());
            });
            channelRepository.saveAll(channels);
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
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            tagRepository.deleteById(tagName);
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
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            Optional<XmlChannel> ch = channelRepository.findById(channelName);
            if(ch.isPresent()) {
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
    private void validateTagRequest(String tagName, XmlTag tag) {
        // 1 
        if (tag.getName() == null || !tagName.equals(tag.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The tag name cannot be null and must match the tag in the URI " + tag.toString(), null);
        }
        // 2
        if (tag.getOwner() == null || tag.getOwner().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The tag owner cannot be null or empty " + tag.toString(), null);
        }
        // 3
        List <String> channelNames = tag.getChannels().stream().map(XmlChannel::getName).collect(Collectors.toList());
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
    private void validateTagRequest(List<XmlTag> tags) {
        for(XmlTag tag: tags) {
            validateTagRequest(tag.getName(),tag);
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
    private void validateTagRequest(String tagName, XmlTag tag, String channelName) {
        validateTagRequest(tagName, tag); 
        Optional<XmlChannel> ch = channelRepository.findById(channelName);
        if(!ch.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The channel with the name " + channelName + " does not exist");
        }
    }
}
