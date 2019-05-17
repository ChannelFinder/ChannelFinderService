package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.PROPERTY_RESOURCE_URI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

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
     * @return list of properties
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Iterable<XmlProperty> list() {
        return propertyRepository.findAll();
    }

    /**
     * GET method for retrieving the property with the path parameter <tt>propertyName</tt> 
     * 
     * To get all its channels use the parameter "withChannels"
     *
     * @param propertyName - property name to search for
     * @return list of channels with their properties and tags that match
     */
    @GetMapping(value = "/{propertyName}", produces = MediaType.APPLICATION_JSON_VALUE)
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
    @PutMapping(value = "/{propertyName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public XmlProperty create(@PathVariable("propertyName") String propertyName, @RequestBody XmlProperty property) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            XmlProperty createdProperty = propertyRepository.index(property);
            // Updated the listed channels in the properties payload with new properties/property values
            channelRepository.saveAll(property.getChannels());
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
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            Iterable<XmlProperty> createdProperties = propertyRepository.indexAll(properties);
            // Updated the listed channels in the properties payload with new properties/property values
            List<XmlChannel> channels = new ArrayList<>();
            properties.forEach(property -> {
                channels.addAll(property.getChannels());
            });
            channelRepository.saveAll(channels);
            return createdProperties;
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on these properties: " + properties, null);
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
    @PostMapping(value = "/{propertyName}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public XmlProperty update(@PathVariable("propertyName") String propertyName, @RequestBody XmlProperty property) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            XmlProperty updatedProperty = propertyRepository.save(property);
            // Updated the listed channels in the properties payload with new properties/property values
            channelRepository.saveAll(property.getChannels());
            return updatedProperty;
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this property: " + propertyName, null);
    }

    /**
     * POST method for updating multiple properties.
     *
     * If the channels don't exist it will fail
     *
     * @param properties - XmlProperties to be updated
     * @return the updated properties
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Iterable<XmlProperty> update(@RequestBody List<XmlProperty> properties) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            Iterable<XmlProperty> createdProperties = propertyRepository.saveAll(properties);
            // Updated the listed channels in the properties payload with new properties/property values
            List<XmlChannel> channels = new ArrayList<>();
            properties.forEach(property -> {
                channels.addAll(property.getChannels());
            });
            channelRepository.saveAll(channels);
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
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            propertyRepository.deleteById(propertyName);
        } else
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User does not have the proper authorization to perform an operation on this property: " + propertyName, null);
    }

    /**
     * DELETE method for deleting the property identified by <tt>propertyName</tt> from the
     * channel <tt>channelName</tt> (both path parameters).
     *
     * @param propertyName - name of property to remove
     */
    @DeleteMapping("/{propertyName}/{channelName}")
    public void removeSingle(@PathVariable("propertyName") final String propertyName, @PathVariable("channelName") String channelName) {
        if(authorizationService.isAuthorizedRole(SecurityContextHolder.getContext().getAuthentication(), ROLES.CF_PROPERTY)) {
            Optional<XmlChannel> ch = channelRepository.findById(channelName);
            if(ch.isPresent()) {
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
    }
}
