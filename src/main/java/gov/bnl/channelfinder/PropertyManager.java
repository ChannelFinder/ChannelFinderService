package gov.bnl.channelfinder;

import static gov.bnl.channelfinder.CFResourceDescriptors.PROPERTY_RESOURCE_URI;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Lists;

@RestController
@RequestMapping(PROPERTY_RESOURCE_URI)
@EnableAutoConfiguration
public class PropertyManager {

    // private SecurityContext securityContext;
    static Logger logManagerAudit = Logger.getLogger(PropertyManager.class.getName() + ".audit");
    static Logger log = Logger.getLogger(PropertyManager.class.getName());

    @Autowired
    TagRepository tagRepository;
    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ChannelRepository channelRepository;

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
     * GET method for retrieving the property with the path parameter
     * <tt>propertyName</tt> 
     * 
     * To get all its channels use the parameter "withChannels"
     *
     * @param propertyName
     *            URI path parameter: property name to search for
     * @return list of channels with their properties and tags that match
     */
    @GetMapping(value = "/{propName}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public XmlProperty read(@PathVariable("propName") String propertyName) {
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
     * @param propertyName URI path parameter: property name
     * @param property an XmlProperty instance with the list of channels to add the property <tt>propertyName</tt> to
     * @return the created property
     */
    @PutMapping(value = "/{propertyName}",
                consumes = MediaType.APPLICATION_JSON_VALUE)
    public XmlProperty create(@PathVariable("propertyName") String propertyName, XmlProperty property) {
        return propertyRepository.index(property);
    }

    /**
     * PUT method for creating multiple properties.
     *
     * @param data XmlProperties data (from payload)
     * @return The list of properties created
     */
    @PutMapping()
    public List<XmlProperty> create(List<XmlProperty> data) {
        return Lists.newArrayList(propertyRepository.indexAll(data));
    }
    
    /**
     * POST method for updating the property identified by the path parameter
     * <tt>propertyName</tt>, adding it to all channels identified by the payload structure
     * <tt>property</tt>. Setting the owner attribute in the XML root element is
     * mandatory. Values for the properties are taken from the payload.
     *
     * @param propertyName URI path parameter: property name
     * @param property a XmlProperty instance with the  list of channels to add the property <tt>propertyName</tt> to
     * @return XmlProperty the updated property
     */
    @PostMapping(value = "/{propertyName}",
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    public XmlProperty update(@PathVariable("propertyName") String propertyName, XmlProperty property) {
        return propertyRepository.save(property);
    }
    /**
     * POST method for creating multiple properties.
     *
     * If the channels don't exist it will fail
     *
     * @param properties List of XmlProperty data (from payload)
     * @return List of all the updated properties
     * @throws IOException
     *             when audit or log fail
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Iterable<XmlProperty> update(List<XmlProperty> properties) {
        return propertyRepository.saveAll(properties);
    }
    

    /**
     * DELETE method for deleting the property identified by the path parameter
     * <tt>propertyName</tt> from all channels.
     *
     * @param propertyName
     *            URI path parameter: property name to remove
     */
    @DeleteMapping(value = "/{propertyName}")
    public void remove(@PathVariable("propertyName") String propertyName) {
        propertyRepository.deleteById(propertyName);
    }
}
