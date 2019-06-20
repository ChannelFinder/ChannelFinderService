package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.ES_PROPERTY_INDEX;
import static gov.bnl.channelfinder.CFResourceDescriptors.ES_PROPERTY_TYPE;
import static gov.bnl.channelfinder.CFResourceDescriptors.PROPERTY_RESOURCE_URI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.RequestOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping(PROPERTY_RESOURCE_URI)
@EnableAutoConfiguration
public class PropertyManager {

    // private SecurityContext securityContext;
    static Logger propertyManagerAudit = Logger.getLogger(PropertyManager.class.getName() + ".audit");
    static Logger log = Logger.getLogger(PropertyManager.class.getName());

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    AuthorizationService authorizationService;

    /**
     * GET method for retrieving the list of properties in the database.
     *
     * @return list of all properties
     */
    @GetMapping
    public Iterable<XmlProperty> list() {
        return propertyRepository.findAll();
    }

    /**
     * GET method for retrieving the property with the path parameter <tt>propertyName</tt> 
     * 
     * To get all its channels use the parameter "withChannels"
     *
     * @param propertyName - property name to search for
     * @return found property
     */
    @GetMapping("/{propertyName}")
    public XmlProperty read(@PathVariable("propertyName") String propertyName,
            @RequestParam(value = "withChannels", defaultValue = "true") boolean withChannels) {
        propertyManagerAudit.info("getting property: " + propertyName);
        Optional<XmlProperty> foundProperty = propertyRepository.findById(propertyName);
        if (foundProperty.isPresent()) {
            return foundProperty.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The property with the name " + propertyName + " does not exist");
        }
    }

    /**
     * PUT method for creating and <b>exclusively</b> adding the property
     * identified by the path parameter <tt>propertyName</tt> to all channels
     * identified by the payload structure <tt>property</tt>. Setting the owner
     * attribute in the XML root element is mandatory. Values for the properties
     * are taken from the payload.
     *
     *
     * @param propertyName - name of property to be created
     * @param property - an XmlProperty instance with the list of channels to add the property <tt>propertyName</tt> to
     * @return the created property
     */
    @PutMapping("/{propertyName}")
    public XmlProperty create(@PathVariable("propertyName") String propertyName, @RequestBody XmlProperty property) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            long start = System.currentTimeMillis();
            propertyManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validatePropertyRequest(property);

            // check if authorized owner
            Optional<XmlProperty> existingProperty = propertyRepository.findById(property.getName());
            boolean present = existingProperty.isPresent();
            if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), property)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User does not have the proper authorization to perform an operation on this property: " + property, null);
            }
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingProperty.get())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this property: " + existingProperty, null);
                } 
                // delete existing property
                propertyRepository.deleteById(propertyName);
            } 

            // create new property
            XmlProperty createdProperty = propertyRepository.index(property);

            if(!property.getChannels().isEmpty()) {
                // update the listed channels in the property's payload with the new property
                channelRepository.saveAll(property.getChannels());
            }
            return createdProperty;
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this property: " + propertyName, null);
    }

    /**
     * PUT method for creating multiple properties.
     *
     * @param properties - XmlProperties to be created
     * @return the list of properties created
     */
    @PutMapping()
    public Iterable<XmlProperty> create(@RequestBody List<XmlProperty> properties) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            long start = System.currentTimeMillis();
            propertyManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validatePropertyRequest(properties);

            // check if authorized owner
            for(XmlProperty property: properties) {
                Optional<XmlProperty> existingProperty = propertyRepository.findById(property.getName());
                boolean present = existingProperty.isPresent();
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), property)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this property: " + property, null);
                }
                if(present) {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingProperty.get())) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "User does not have the proper authorization to perform an operation on this property: " + existingProperty, null);
                    } 
                    // delete existing property
                    propertyRepository.deleteById(property.getName());
                }         
            }

            // create new properties
            Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(properties);

            // update the listed channels in the properties' payloads with the new properties
            List<XmlChannel> channels = new ArrayList<>();
            properties.forEach(property -> {
                channels.addAll(property.getChannels());
            });
            if(!channels.isEmpty()) {
                channelRepository.saveAll(channels);
            }
            return createdProperties;
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on these properties: " + properties, null);
    }

    /**
     * PUT method for adding the property identified by <tt>property</tt> to the single
     * channel <tt>chan</tt> (both path parameters).
     * 
     * TODO: could be simplified with multi index update and script which can use
     * wildcards thus removing the need to explicitly define the entire property
     * 
     * @param propertyName - name of tag to be created
     * @param channelName - channel to update <tt>tag</tt> to
     * @return added property
     */
    @PutMapping("/{propertyName}/{chName}")
    public XmlProperty addSingle(@PathVariable("propertyName") String propertyName, @PathVariable("channelName") String channelName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_TAG)) {
            long start = System.currentTimeMillis();
            propertyManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validatePropertyRequest(channelName);

            // check if authorized owner
            Optional<XmlProperty> existingProperty = propertyRepository.findById(propertyName);
            boolean present = existingProperty.isPresent();
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingProperty.get())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this property: " + existingProperty, null);
                } 
                // add property to channel
                XmlChannel channel = channelRepository.findById(channelName).get();
                channel.addProperty(existingProperty.get());
                channelRepository.save(channel);
                return existingProperty.get();
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The tag with the name " + propertyName + " does not exist");
            }
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this property: " + propertyName, null);
    }

    /**
     * POST method for updating the property identified by the path parameter
     * <tt>propertyName</tt>, adding it to all channels identified by the payload structure
     * <tt>property</tt>. Setting the owner attribute in the XML root element is
     * mandatory. Values for the properties are taken from the payload.
     *
     * @param propertyName - name of property to be updated
     * @param property - a XmlProperty instance with the list of channels to add the property <tt>propertyName</tt> to
     * @return the updated property
     */
    @PostMapping("/{propertyName}")
    public XmlProperty update(@PathVariable("propertyName") String propertyName, @RequestBody XmlProperty property) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            long start = System.currentTimeMillis();
            propertyManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validatePropertyRequest(property);

            // check if authorized owner
            Optional<XmlProperty> existingProperty = propertyRepository.findById(property.getName());
            boolean present = existingProperty.isPresent();
            if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), property)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User does not have the proper authorization to perform an operation on this property: " + property, null);
            }
            if(present) {
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingProperty.get())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this property: " + existingProperty, null);
                } 
            } 

            // update property
            XmlProperty updatedProperty = propertyRepository.save(propertyName,property);

            if(!property.getChannels().isEmpty()) {
                // update the listed channels in the property's payload with the updated property
                channelRepository.saveAll(property.getChannels());
            }
            return updatedProperty;
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this property: " + propertyName, null);
    }

    /**
     * POST method for updating multiple properties and updating all the appropriate
     * channels.
     *
     * If the channels don't exist it will fail
     *
     * @param properties - XmlProperties to be updated
     * @return the updated properties
     */
    @PostMapping()
    public Iterable<XmlProperty> update(@RequestBody List<XmlProperty> properties) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            long start = System.currentTimeMillis();
            propertyManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
            // Validate request parameters
            validatePropertyRequest(properties);

            // check if authorized owner
            for(XmlProperty property: properties) {
                Optional<XmlProperty> existingProperty = propertyRepository.findById(property.getName());
                boolean present = existingProperty.isPresent();
                if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), property)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this property: " + property, null);
                }
                if(present) {
                    if(!authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingProperty.get())) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "User does not have the proper authorization to perform an operation on this property: " + existingProperty, null);
                    } 
                }               
            }

            // update tags
            Iterable<XmlProperty> createdProperties = propertyRepository.saveAll(properties);

            // update the listed channels in the properties' payloads with the updated properties
            List<XmlChannel> channels = new ArrayList<>();
            properties.forEach(property -> {
                channels.addAll(property.getChannels());
            });
            if(!channels.isEmpty()) {
                channelRepository.saveAll(channels);
            }
            return createdProperties;
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on these properties: " + properties, null);
    }

    /**
     * DELETE method for deleting the property identified by the path parameter
     * <tt>propertyName</tt> from all channels.
     *
     * @param propertyName - name of property to remove
     */
    @DeleteMapping("/{propertyName}")
    public void remove(@PathVariable("propertyName") String propertyName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            Optional<XmlProperty> existingProperty = propertyRepository.findById(propertyName);
            if(existingProperty.isPresent()) {
                // check if authorized owner
                if(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingProperty.get())) {
                    // delete property
                    propertyRepository.deleteById(propertyName);
                } else {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this property: " + propertyName, null);
                }
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The property with the name " + propertyName + " does not exist");
            }
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this property: " + propertyName, null);
    }

    /**
     * DELETE method for deleting the property identified by <tt>propertyName</tt> from the
     * channel <tt>channelName</tt> (both path parameters).
     *
     * @param propertyName - name of property to remove
     * @param channelName - channel to remove <tt>propertyName</tt> from
     */
    @DeleteMapping("/{propertyName}/{channelName}")
    public void removeSingle(@PathVariable("propertyName") final String propertyName, @PathVariable("channelName") String channelName) {
        // check if authorized role
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            Optional<XmlProperty> existingProperty = propertyRepository.findById(propertyName);
            if(existingProperty.isPresent()) {
                // check if authorized owner
                if(authorizationService.isAuthorizedOwner(SecurityContextHolder.getContext().getAuthentication(), existingProperty.get())) {
                    Optional<XmlChannel> ch = channelRepository.findById(channelName);
                    if(ch.isPresent()) {
                        // remove property from channel
                        XmlChannel channel = ch.get();
                        channel.removeProperty(new XmlProperty(propertyName, ""));
                        channelRepository.index(channel);
                    } else {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "The channel with the name " + channelName + " does not exist");
                    }
                } else
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User does not have the proper authorization to perform an operation on this property: " + propertyName, null);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The tag with the name " + propertyName + " does not exist");
            }
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this tag: " + propertyName, null);             
    }

    /**
     * Check that the existing property and the property in the request body match
     * 
     * @param existing
     * @param request
     * @return
     */
    boolean validatePropertyRequest(XmlProperty existing, XmlProperty request) {
        return existing.getName().equals(request.getName());
    }

    /**
     * Checks if
     * 1. the property name is not null and matches the name in the body
     * 2. the property owner is not null or empty
     * 3. all the listed channels exist and have the property with a non null & non empty value
     * 
     * @param data
     */
    private void validatePropertyRequest(XmlProperty property) {
        // 1 
        if (property.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The property name cannot be null " + property.toString(), null);
        }
        // 2
        if (property.getOwner() == null || property.getOwner().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The property owner cannot be null or empty " + property.toString(), null);
        }
        // 3
        property.getChannels().stream().forEach((channel) -> {
            if(!channelRepository.existsById(channel.getName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The channel with the name " + channel.getName() + " does not exist");
            }
            if(channel.getProperties().stream().anyMatch((t) -> {
                return t.getName().equals(property.getName()) && t.getValue() != null && !t.getValue().isEmpty();
            })) {

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "The channel with the name " + channel.getName()
                                    + " does not include a valid instance to the property " + property);
            }
        });

    }

    /**
     * Checks if
     * 1. the property name is not null and matches the name in the body
     * 2. the property owner is not null or empty
     * 3. the property value is not null or empty
     * 4. all the listed channels exist
     * 
     * @param data
     */
    private void validatePropertyRequest(List<XmlProperty> properties) {
        for(XmlProperty property: properties) {
            validatePropertyRequest(property);
        }
    }

    /**
     * Checks if
     * 1. the property name is not null and matches the name in the body
     * 2. the property owner is not null or empty
     * 3. the property value is not null or empty
     * 4. all the listed channels exist
     * 
     * @param data
     */
    private void validatePropertyRequest(String channelName) {
        Optional<XmlChannel> ch = channelRepository.findById(channelName);
        if(!ch.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The channel with the name " + channelName + " does not exist");
        }
    }
}
