package gov.bnl.channelfinder;

public class CFResourceDescriptors {

    static final String CF_SERVICE = "ChannelFinder";
    static final String TAG_RESOURCE_URI = CF_SERVICE + "/resources/tags";
    static final String PROPERTY_RESOURCE_URI = CF_SERVICE + "/resources/properties";
    static final String CHANNEL_RESOURCE_URI = CF_SERVICE + "/resources/channels";

    public static final String ES_TAG_INDEX = "cf_tags";
    public static final String ES_TAG_TYPE = "cf_tag";

    public static final String ES_PROPERTY_INDEX = "cf_properties";
    public static final String ES_PROPERTY_TYPE = "cf_property";

    public static final String ES_CHANNEL_INDEX = "channelfinder";
    public static final String ES_CHANNEL_TYPE = "cf_channel";

}
