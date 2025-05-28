package org.phoebus.channelfinder;

import static org.phoebus.channelfinder.CFResourceDescriptors.TAG_RESOURCE_URI;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.text.MessageFormat;
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
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Tag;
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

    private static final Logger tagManagerAudit = Logger.getLogger(TagManager.class.getName() + ".audit");
    private static final Logger logger = Logger.getLogger(TagManager.class.getName());

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
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List all Tags",
                            content = @Content(
                                    array = @ArraySchema(schema = @Schema(implementation = Tag.class)))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error while trying to list all Tags",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
            })
    @GetMapping
    public Iterable<Tag> list() {
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
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Finding Tag by tagName",
                            content = @Content(schema = @Schema(implementation = Tag.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tag not found",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
            })
    @GetMapping("/{tagName}")
    public Tag read(@PathVariable("tagName") String tagName,
                    @RequestParam(value = "withChannels", defaultValue = "true") boolean withChannels) {
        tagManagerAudit.log(Level.INFO, () -> MessageFormat.format(TextUtil.FIND_TAG, tagName));

        if(withChannels) {
            Optional<Tag> foundTag = tagRepository.findById(tagName,true);
            if(foundTag.isPresent()) {
                return foundTag.get();
            } else {
                String message = MessageFormat.format(TextUtil.TAG_NAME_DOES_NOT_EXIST, tagName);
                logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
        } else {
            Optional<Tag> foundTag = tagRepository.findById(tagName);
            if(foundTag.isPresent()) {
                return foundTag.get();
            } else {
                String message = MessageFormat.format(TextUtil.TAG_NAME_DOES_NOT_EXIST, tagName);
                logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
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
     * @param tag - Tag structure containing the list of channels to be tagged
     * @return the created tag
     */
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tag created and updated",
                            content = @Content(schema = @Schema(implementation = Tag.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tag-, or Channel-name does not exist",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error while trying to create/update Tag",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
            })
    @PutMapping("/{tagName}")
    public Tag create(@PathVariable("tagName") String tagName, @RequestBody Tag tag) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.log(Level.INFO, () -> MessageFormat.format(TextUtil.CLIENT_INITIALIZATION, (System.currentTimeMillis() - start)));
            // Validate request parameters
            validateTagRequest(tag);

            // check if authorized owner
            if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tag.toLog());
                logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
            }
            Optional<Tag> existingTag = tagRepository.findById(tagName);
            boolean present = existingTag.isPresent();
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, existingTag.get().toLog());
                    logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                } 
                // delete existing tag
                tagRepository.deleteById(tagName);
            } 

            // create new tag
            Tag createdTag = tagRepository.index(tag);

            if (!tag.getChannels().isEmpty()) {
                tag.getChannels().forEach(chan -> chan.addTag(createdTag));
                // update the listed channels in the tag's payloads with the new tag
                Iterable<Channel> chans = channelRepository.saveAll(tag.getChannels());
                List<Channel> chanList = new ArrayList<>();
                for (Channel chan : chans) {
                    chanList.add(chan);
                }
                createdTag.setChannels(chanList);
            }
            return createdTag;
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tagName);
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
    }

    /**
     * PUT method for creating multiple tags.
     * 
     * @param tags - XmlTags to be created
     * @return the list of tags created
     */
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tags created",
                            content = @Content(
                                    array =
                                    @ArraySchema(schema = @Schema(implementation = Tag.class)))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tag-, or Channel-name does not exist",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error while trying to create Tags",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
            })
    @PutMapping()
    public Iterable<Tag> create(@RequestBody Iterable<Tag> tags) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.log(Level.INFO, () -> MessageFormat.format(TextUtil.CLIENT_INITIALIZATION, (System.currentTimeMillis() - start)));

            // check if authorized owner
            for(Tag tag: tags) {
                Optional<Tag> existingTag = tagRepository.findById(tag.getName());
                boolean present = existingTag.isPresent();
                if(present) {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                        String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, existingTag.get().toLog());
                        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                    }
                    tag.setOwner(existingTag.get().getOwner());
                } else {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                        String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tag.toLog());
                        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                    }
                }
            }

            // Validate request parameters
            validateTagRequest(tags);

            // delete existing tags
            for(Tag tag: tags) {
                if(tagRepository.existsById(tag.getName())) {
                    // delete existing tag
                    tagRepository.deleteById(tag.getName());
                } 
            }

            // create new tags
            Iterable<Tag> createdTags = tagRepository.indexAll(Lists.newArrayList(tags));

            // update the listed channels in the tags' payloads with new tags
            Map<String, Channel> channels = new HashMap<>();
            for (Tag tag : tags) {
                for (Channel channel : tag.getChannels()) {
                    if (channels.get(channel.getName()) != null) {
                        channels.get(channel.getName()).addTag(new Tag(tag.getName(), tag.getOwner()));
                    } else {
                        channel.addTag(new Tag(tag.getName(), tag.getOwner()));
                        channels.put(channel.getName(), channel);
                    }
                }
            }

            if(!channels.isEmpty()) {
                Iterable<Channel> chans = channelRepository.saveAll(channels.values());
            }
            // TODO should return created tags with properly organized saved channels, but it would be very complicated...
            return tags;
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAGS, tags);
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
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
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tags added to a single channel",
                            content = @Content(schema = @Schema(implementation = Tag.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tag-, or Channel-name does not exist",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Tag creational error",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
            })
    @PutMapping("/{tagName}/{channelName}")
    public Tag addSingle(@PathVariable("tagName") String tagName, @PathVariable("channelName") String channelName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.log(Level.INFO, () -> MessageFormat.format(TextUtil.CLIENT_INITIALIZATION, (System.currentTimeMillis() - start)));
            // Validate request parameters
            validateTagWithChannelRequest(channelName);

            // check if authorized owner
            Optional<Tag> existingTag = tagRepository.findById(tagName);
            boolean present = existingTag.isPresent();
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, existingTag.get().toLog());
                    logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                } 
                // add tag to channel
                Channel channel = channelRepository.findById(channelName).get();
                channel.addTag(existingTag.get());
                Channel taggedChannel = channelRepository.save(channel);
                Tag addedTag = existingTag.get();
                addedTag.setChannels(Arrays.asList(taggedChannel));
                return addedTag;
            } else {
                String message = MessageFormat.format(TextUtil.TAG_NAME_DOES_NOT_EXIST, tagName);
                logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tagName);
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
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
     * @param tag - Tag with list of channels to addSingle the tag <code>name</code> to
     * @return the updated tag
     */
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tag updated",
                            content = @Content(schema = @Schema(implementation = Tag.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tag name does not exist",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Tag update error",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
            })
    @PostMapping("/{tagName}")
    public Tag update(@PathVariable("tagName") String tagName, @RequestBody Tag tag) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.log(Level.INFO, () -> MessageFormat.format(TextUtil.CLIENT_INITIALIZATION, (System.currentTimeMillis() - start)));
            // Validate request parameters
            validateTagRequest(tag);

            // check if authorized owner
            if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tag.toLog());
                logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
            }
            List<Channel> channels = new ArrayList<>();
            Optional<Tag> existingTag = tagRepository.findById(tagName,true);

            Tag newTag;
            if(existingTag.isPresent()) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, existingTag.get().toLog());
                    logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
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
            Tag updatedTag = tagRepository.save(newTag);

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
                Iterable<Channel> updatedChannels = channelRepository.saveAll(channels);
                updatedTag.setChannels(StreamSupport.stream(updatedChannels.spliterator(), false)
                        .collect(Collectors.toList()));
            }

            return updatedTag;
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tagName);
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
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
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tags updated",
                            content = @Content(array =
                            @ArraySchema(schema = @Schema(implementation = Tag.class)))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tag-, or Channel-name does not exist",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error while updating tags",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
            })
    @PostMapping()
    public Iterable<Tag> update(@RequestBody Iterable<Tag> tags) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            tagManagerAudit.log(Level.INFO, () -> MessageFormat.format(TextUtil.CLIENT_INITIALIZATION, (System.currentTimeMillis() - start)));

            // check if authorized owner
            for(Tag tag:tags) {
                Optional<Tag> existingTag = tagRepository.findById(tag.getName());
                boolean present = existingTag.isPresent();
                if(present) {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                        String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, existingTag.get().toLog());
                        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                    }
                    tag.setOwner(existingTag.get().getOwner());
                } else {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), tag)) {
                        String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tag.toLog());
                        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                    }
                }
            }

            // Validate request parameters
            validateTagRequest(tags);

            // update the listed channels in the tags' payloads with new tags
            Map<String, Channel> channels = new HashMap<>();
            for (Tag tag : tags) {
                for (Channel channel : tag.getChannels()) {
                    if (channels.get(channel.getName()) != null) {
                        channels.get(channel.getName()).addTag(new Tag(tag.getName(), tag.getOwner()));
                    } else {
                        channel.addTag(new Tag(tag.getName(), tag.getOwner()));
                        channels.put(channel.getName(), channel);
                    }
                }
            }

            // update tags
            Iterable<Tag> updatedTags = tagRepository.saveAll(tags);

            // update channels
            if(!channels.isEmpty()) {
                channelRepository.saveAll(channels.values());
            }
            // TODO should return updated tags with properly organized saved channels, but it would be very complicated...
            return tags;
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAGS, tags);
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
    }

    /**
     * DELETE method for deleting the tag identified by the path parameter
     * <code>tagName</code> from all channels.
     *
     * @param tagName - name of tag to remove
     */
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tag deleted"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tag does not exist",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Tag creational error",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
            })
    @DeleteMapping("/{tagName}")
    public void remove(@PathVariable("tagName") String tagName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            Optional<Tag> existingTag = tagRepository.findById(tagName);
            if(existingTag.isPresent()) {
                // check if authorized owner
                if(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    // delete tag
                    tagRepository.deleteById(tagName);
                } else {
                    String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tagName);
                    logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                }
            } else {
                String message = MessageFormat.format(TextUtil.TAG_NAME_DOES_NOT_EXIST, tagName);
                logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tagName);
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
    }

    /**
     * DELETE method for deleting the tag identified by <code>tagName</code> from the
     * channel <code>channelName</code> (both path parameters).
     *
     * @param tagName - name of tag to remove
     * @param channelName - channel to remove <code>tagName</code> from
     */
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tag deleted from the desired channel"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tag does not exist",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Tag creational error",
                            content = @Content(schema = @Schema(implementation = ResponseStatusException.class)))
            })
    @DeleteMapping("/{tagName}/{channelName}")
    public void removeSingle(@PathVariable("tagName") final String tagName, @PathVariable("channelName") String channelName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            Optional<Tag> existingTag = tagRepository.findById(tagName);
            if(existingTag.isPresent()) {
                // check if authorized owner
                if(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingTag.get())) {
                    Optional<Channel> ch = channelRepository.findById(channelName);
                    if(ch.isPresent()) {
                        // remove tag from channel
                        Channel channel = ch.get();
                        channel.removeTag(new Tag(tagName, ""));
                        channelRepository.index(channel);
                    } else {
                        String message = MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channelName);
                        logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
                    }
                } else {
                    String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tagName);
                    logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
                }
            } else {
                String message = MessageFormat.format(TextUtil.TAG_NAME_DOES_NOT_EXIST, tagName);
                logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
        } else {
            String message = MessageFormat.format(TextUtil.USER_NOT_AUTHORIZED_ON_TAG, tagName);
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message, null);
        }
    }

    /**
     * Checks if all the tags included satisfy the following conditions
     *
     * <ol>
     * <li> the tag names are not null or empty and matches the names in the bodies
     * <li> the tag owners are not null or empty
     * <li> all the channels exist
     * </ol>
     *
     * @param tags the list of tags to be validated
     */
    public void validateTagRequest(Iterable<Tag> tags) {
        for(Tag tag: tags) {
            validateTagRequest(tag);
        }
    }

    /**
     * Checks if tag satisfies the following conditions
     *
     * <ol>
     * <li> the tag name is not null or empty and matches the name in the body
     * <li> the tag owner is not null or empty
     * <li> all the listed channels exist
     * </ol>
     *
     * @param tag the tag to be validates
     */
    public void validateTagRequest(Tag tag) {
        // 1 
        if (tag.getName() == null || tag.getName().isEmpty()) {
            String message = MessageFormat.format(TextUtil.TAG_NAME_CANNOT_BE_NULL_OR_EMPTY, tag.toLog());
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
        }
        // 2
        if (tag.getOwner() == null || tag.getOwner().isEmpty()) {
            String message = MessageFormat.format(TextUtil.TAG_OWNER_CANNOT_BE_NULL_OR_EMPTY, tag.toLog());
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
        }
        // 3
        List <String> channelNames = tag.getChannels().stream().map(Channel::getName).collect(Collectors.toList());
        for(String channelName:channelNames) {
            if(!channelRepository.existsById(channelName)) {
                String message = MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channelName);
                logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
        }
    }

    /**
     * Checks if channel with name "channelName" exists
     * @param channelName check channel exists
     */
    public void validateTagWithChannelRequest(String channelName) {
        if(!channelRepository.existsById(channelName)) {
            String message = MessageFormat.format(TextUtil.CHANNEL_NAME_DOES_NOT_EXIST, channelName);
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

}
