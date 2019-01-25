package gov.bnl.channelfinder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

@RestController
@RequestMapping("/tags")
@EnableAutoConfiguration
public class TagManager {

    // private SecurityContext securityContext;
    static Logger tagManagerAudit = Logger.getLogger(TagManager.class.getName() + ".audit");
    static Logger log = Logger.getLogger(TagManager.class.getName());

    @Autowired
    TagRepository tagRepository;

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
    @PutMapping("/tag/{tag}")
    public XmlTag create(@PathVariable("tag") String tag, @RequestBody XmlTag data) {
        long start = System.currentTimeMillis();
        tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
        try {
            if (tag.equals(data.getName())) {
                return tagRepository.index(data);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * GET method for retrieving the list of tags in the database.
     *
     * @return list of tags
     */
    @GetMapping
    public List<XmlTag> listTags(@RequestParam Map<String, String> allRequestParams) {
        tagRepository.findAll();
        return null;
    }

    /**
     * GET method for retrieving the tag with the path parameter <tt>tagName</tt>
     * 
     * To get all its channels use the parameter "withChannels"
     *
     * @param tag URI path parameter: tag name to search for
     * @return list of channels with their properties and tags that match
     */
    @GetMapping("/tag")
    public XmlTag read(@RequestParam("tag") String tag, @RequestParam("withChannels") boolean withChannels) {
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
    @PutMapping("/tag")
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
     * @param tag  URI path parameter: tag name
     * @param data list of channels to addSingle the tag <tt>name</tt> to
     * @return HTTP Response
     */
    @PostMapping("/tag/{tag}")
    public XmlTag update(@PathVariable("tag") String tag, @RequestBody XmlTag data) {
        long start = System.currentTimeMillis();
        tagManagerAudit.info("client initialization: " + (System.currentTimeMillis() - start));
        return tagRepository.save(data);
    }

    /**
     * POST method for creating multiple tags and updating all the appropriate
     * channels If the channels don't exist it will fail
     *
     * @param data XmlTags data (from payload)
     * @return HTTP Response
     * @throws IOException when audit or log fail
     */
    @PostMapping("/tag")
    public List<XmlTag> updateTags(@RequestBody List<XmlTag> data) throws IOException {
        return null;
    }

    /**
     * DELETE method for deleting the tag identified by the path parameter
     * <tt>name</tt> from all channels.
     *
     * @param tag URI path parameter: tag name to remove
     * @return HTTP Response
     */
    @DeleteMapping
    public void remove(@RequestParam("tagName") String tag) {
        tagRepository.deleteById(tag);
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
    public String addSingle(@PathVariable("tagName") String tag, @PathVariable("chName") String chan, XmlTag data) {
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
     * DELETE method for deleting the tag identified by <tt>tag</tt> from the
     * channel <tt>chan</tt> (both path parameters).
     *
     * @param tag  URI path parameter: tag name to remove
     * @param chan URI path parameter: channel to remove <tt>tag</tt> from
     * @return HTTP Response
     */
    @DeleteMapping("/{tagName}")
    public String removeSingle(@PathVariable("tagName") final String tag, @RequestParam("chName") String chan) {
        return null;
    }

    abstract class OnlyXmlTag {
        @JsonIgnore
        private List<XmlChannel> channels;
    }

    /**
     * A filter to be used with the jackson mapper to ignore the embedded
     * xmlchannels in the tag object
     * 
     * @author Kunal Shroff
     *
     */
    @JsonIgnoreType
    public class MyMixInForXmlChannels {
        //
    }

}
