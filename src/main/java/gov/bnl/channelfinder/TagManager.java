package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.TAG_RESOURCE_URI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
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

import com.google.common.collect.Lists;

@RestController
@RequestMapping(TAG_RESOURCE_URI)
@EnableAutoConfiguration
public class TagManager {

	//	private SecurityContext securityContext;
    static Logger tagManagerAudit = Logger.getLogger(TagManager.class.getName() + ".audit");
    static Logger log = Logger.getLogger(TagManager.class.getName());

    @Autowired
    TagRepository tagRepository;

    @Autowired
    ChannelRepository channelRepository;

    /**
     * PUT method to create and <b>exclusively</b> update the tag identified by the
     * path parameter <tt>name</tt> to all channels identified in the payload
     * structure <tt>data</tt>. Setting the owner attribute in the XML root element
     * is mandatory.
     * 
     * @param tag  URI path parameter: tag name
     * @param data XmlTag structure containing the list of channels to be tagged
     * @return HTTP Response
     */
    @PutMapping("/{tag}")
    public XmlTag create(@PathVariable("tag") String tag, @RequestBody XmlTag data) {
        long start = System.currentTimeMillis();
        tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
        if (tag.equals(data.getName())) {
            validateTagRequest(data);
            return tagRepository.index(data);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The tag name in the body " + data.toString() + " Does not match the URI " + tag, null);
        }
    }

    /**
     * Checks if
     * 1. the tag owner is not null or empty
     * 2. all the listed channels exist
     * 
     * @param data
     */
    private void validateTagRequest(@RequestBody XmlTag tag) {
        if (tag.getOwner() == null || tag.getOwner().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The tag owner cannot be null or empty " + tag.toString(), null);
        }
//        tag.getChannels().stream().map(XmlChannel::getName).collect(Collectors.toList())
    }

    /**
     * GET method for retrieving the list of tags in the database.
     *
     * @return list of tags
     */
    @GetMapping
    public List<XmlTag> listTags(@RequestParam Map<String, String> allRequestParams) {
        // TODO this is an extra copy because the CF API contract was a List and the
        // CRUDrepository contract is an Iterable. In the future one of the two should be
        // changed.
        return Lists.newArrayList(tagRepository.findAll());
    }

    /**
     * GET method for retrieving the tag with the path parameter <tt>tagName</tt>
     * 
     * To get all its channels use the parameter "withChannels"
     *
     * @param tag URI path parameter: tag name to search for
     * @return list of channels with their properties and tags that match
     */
    @GetMapping("/{tag}")
    public XmlTag read(@PathVariable("tag") String tag, @RequestParam("withChannels") boolean withChannels) {
        Optional<XmlTag> foundTag = tagRepository.findById(tag);
        if(foundTag.isPresent()) {
            return foundTag.get();
        }
        return null;
    }

    /**
     * PUT method to create and <b>exclusively</b> update the tag identified by the
     * path parameter <tt>name</tt> to all channels identified in the payload
     * structure <tt>data</tt>. Setting the owner attribute in the XML root element
     * is mandatory.
     * 
     * @param data XmlTag structure containing the list of channels to be tagged
     * @return HTTP Response
     */
    @PutMapping()
    public List<XmlTag> createTags(@RequestBody List<XmlTag> data) {
        long start = System.currentTimeMillis();
        tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
        return (List<XmlTag>) tagRepository.indexAll(data);
    }

    /**
     * POST method to update the the tag identified by the path parameter
     * <tt>name</tt>, adding it to all channels identified by the channels inside
     * the payload structure <tt>data</tt>. Setting the owner attribute in the XML
     * root element is mandatory.
     * 
     * TODO: Optimize the bulk channel update
     *
     * @param tagName  URI path parameter: tag name
     * @param tag with list of channels to addSingle the tag <tt>name</tt> to
     * @return HTTP Response
     */
    @PostMapping("/{tag}")
    public XmlTag update(@PathVariable("tag") String tagName, @RequestBody XmlTag tag) {
        long start = System.currentTimeMillis();
        tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
        XmlTag updatedTag = tagRepository.save(tag);
        // Updated the listed channels in tags with the associated tag
        tag.getChannels().forEach(channel -> {
            channel.addTag(updatedTag);
        });
        channelRepository.saveAll(tag.getChannels());
        return updatedTag;
    }

    /**
     * POST method for creating multiple tags and updating all the appropriate
     * channels.
     * 
     * If the channels don't exist it will fail
     *
     * @param tags XmlTags data (from payload)
     * @return List of all the updated tags
     * @throws IOException when audit or log fail
     */
    @PostMapping()
    public List<XmlTag> updateTags(@RequestBody List<XmlTag> tags) throws IOException {
        Iterable<XmlTag> createdTags = tagRepository.saveAll(tags);
        // Updated the listed channels in tags with the associated tags
        List<XmlChannel> channels = new ArrayList<>();
        tags.forEach(tag -> {
            channels.addAll(tag.getChannels());
        });
        channelRepository.saveAll(channels);
        return Lists.newArrayList(createdTags);
    }

    /**
     * PUT method for adding the tag identified by <tt>tag</tt> to the single
     * channel <tt>chan</tt> (both path parameters).
     * 
     * TODO: could be simplified with multi index update and script which can use
     * wildcards thus removing the need to explicitly define the entire tag
     * 
     * @param tag  URI path parameter: tag name
     * @param chan URI path parameter: channel to update <tt>tag</tt> to
     * @param data tag data (ignored)
     * @return HTTP Response
     */
    @PutMapping("/{tagName}/{chName}")
    public String addSingle(@PathVariable("tagName") String tag, @PathVariable("chName") String chan, @RequestBody XmlTag data) {
        return null;
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
     * DELETE method for deleting the tag identified by the path parameter
     * <tt>name</tt> from all channels.
     *
     * @param tag URI path parameter: tag name to remove
     */
    @DeleteMapping("/{tagName}")
    public void remove(@PathVariable("tagName") String tag) {
        tagRepository.deleteById(tag);
    }

    /**
     * DELETE method for deleting the tag identified by <tt>tag</tt> from the
     * channel <tt>channelName</tt> (both path parameters).
     *
     * @param tag  URI path parameter: tag name to remove
     * @param channelName URI path parameter: channel to remove <tt>tag</tt> from
     */
    @DeleteMapping("/{tagName}/{channelName}")
    public void removeSingle(@PathVariable("tagName") final String tagName, @PathVariable("channelName") String channelName) {
        Optional<XmlChannel> ch = channelRepository.findById(channelName);
        if(ch.isPresent()) {
            XmlChannel channel = ch.get();
            channel.removeTag(new XmlTag(tagName, ""));
            channelRepository.index(channel);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The channel with the name " + channelName + " does not exist");
        }
    }

}
