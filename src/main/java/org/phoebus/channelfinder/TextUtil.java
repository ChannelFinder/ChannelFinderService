package org.phoebus.channelfinder;

/**
 * Utility class to assist in handling of text.
 * 
 * @author Lars Johansson
 */
public class TextUtil {

	// common
	// channel
	// property
	// scroll
	// tag

    public static final String CLIENT_INITIALIZATION      = "Client initialization {0}";
    public static final String PATH_POST_VALIDATION_TIME  = "|{0} |POST|validation {1}";

    public static final String COUNT_FAILED_CAUSE         = "Count failed for {0} Cause {1}";
    public static final String SEARCH_FAILED_CAUSE        = "Search failed for {0} Cause {1}";
    public static final String PAYLOAD_PROPERTY_DOES_NOT_MATCH_URI_OR_HAS_BAD_VALUE = "The payload property {0} does not match uri name or has a bad value";

    public static final String BULK_HAD_ERRORS            = "Bulk had errors";
    public static final String CREATED_INDEX_ACKNOWLEDGED = "Created index {0} acknowledged {1}";
    public static final String DELETE_ALL_NOT_SUPPORTED   = "Delete all is not supported.";
    public static final String FAILED_TO_CREATE_INDEX     = "Failed to create index {0}";

    // ----------------------------------------------------------------------------------------------------

    public static final String CHANNEL_FOUND                            = "Channel found {0}";
    public static final String CHANNEL_NOT_FOUND                        = "Channel not found {0}";
    public static final String CHANNEL_NAME_DOES_NOT_EXIST              = "The channel with the name {0} does not exist";
    public static final String CHANNEL_NAME_CANNOT_BE_NULL_OR_EMPTY     = "The channel name cannot be null or empty {0}";
    public static final String CHANNEL_OWNER_CANNOT_BE_NULL_OR_EMPTY    = "The channel owner cannot be null or empty {0}";
    public static final String CHANNEL_NAME_NO_VALID_INSTANCE_PROPERTY  = "The channel with the name {0} does not include a valid instance to the property {1}";

    public static final String CREATE_CHANNEL                           = "Create channel {0}";
    public static final String CREATE_PROPERTY                          = "Create property {0}";
    public static final String CREATE_TAG                               = "Create tag {0}";

    public static final String DELETE_CHANNEL                           = "Delete channel {0}";
    public static final String DELETE_PROPERTY                          = "Delete property {0}";
    public static final String DELETE_TAG                               = "Delete tag {0}";

    public static final String FIND_ALL_CHANNELS_NOT_SUPPORTED          = "Find all is not supported. It could return hundreds of thousands of channels.";
    public static final String FIND_CHANNEL                             = "Find channel {0}";

    public static final String FAILED_TO_INDEX_CHANNEL                  = "Failed to index channel {0}";
    public static final String FAILED_TO_INDEX_CHANNELS                 = "Failed to index channels {0}";
    public static final String FAILED_TO_FIND_CHANNEL                   = "Failed to find channel {0}";
    public static final String FAILED_TO_FIND_ALL_CHANNELS              = "Failed to find all channels";
    public static final String FAILED_TO_CHECK_IF_CHANNEL_EXISTS        = "Failed to check if channel exists {0}";
    public static final String FAILED_TO_DELETE_CHANNEL                 = "Failed to delete channel {0}";

    public static final String USER_NOT_AUTHORIZED_ON_CHANNEL           = "User does not have the proper authorization to perform an operation on this channel {0}";
    public static final String USER_NOT_AUTHORIZED_ON_CHANNELS          = "User does not have the proper authorization to perform an operation on these channels {0}";

    // ----------------------------------------------------------------------------------------------------

    public static final String PROPERTY_FOUND                           = "Property found {0}";
    public static final String PROPERTY_NOT_FOUND                       = "Property not found {0}";
    public static final String PROPERTY_NAME_DOES_NOT_EXIST             = "The property with the name {0} does not exist";
    public static final String PROPERTY_NAME_CANNOT_BE_NULL_OR_EMPTY    = "The property name cannot be null or empty {0}";
    public static final String PROPERTY_OWNER_CANNOT_BE_NULL_OR_EMPTY   = "The property owner cannot be null or empty {0}";
    public static final String PROPERTY_VALUE_NULL_OR_EMPTY             = "The property with the name {0} has value {1} is null or empty";

    public static final String FIND_PROPERTY                            = "Find property {0}";

    public static final String FAILED_TO_INDEX_PROPERTY                 = "Failed to index property {0}";
    public static final String FAILED_TO_INDEX_PROPERTIES               = "Failed to index properties {0}";
    public static final String FAILED_TO_UPDATE_SAVE_PROPERTIES         = "Failed to update/save properties {0}";
    public static final String FAILED_TO_FIND_PROPERTY                  = "Failed to find property {0}";
    public static final String FAILED_TO_FIND_ALL_PROPERTIES            = "Failed to find all properties";
    public static final String FAILED_TO_CHECK_IF_PROPERTY_EXISTS       = "Failed to check if property exists {0}";
    public static final String FAILED_TO_DELETE_PROPERTY                = "Failed to delete property {0}";

    public static final String USER_NOT_AUTHORIZED_ON_PROPERTY          = "User does not have the proper authorization to perform an operation on this property {0}";
    public static final String USER_NOT_AUTHORIZED_ON_PROPERTIES        = "User does not have the proper authorization to perform an operation on these properties {0}";

    // ----------------------------------------------------------------------------------------------------

    public static final String TAG_FOUND                                = "Tag found {0}";
    public static final String TAG_NOT_FOUND                            = "Tag not found {0}";
    public static final String TAG_NAME_DOES_NOT_EXIST                  = "The tag with the name {0} does not exist";
    public static final String TAG_NAME_CANNOT_BE_NULL_OR_EMPTY         = "The tag name cannot be null or empty {0}";
    public static final String TAG_OWNER_CANNOT_BE_NULL_OR_EMPTY        = "The tag owner cannot be null or empty {0}";

    public static final String FIND_TAG                                 = "Find tag {0}";

    public static final String FAILED_TO_INDEX_TAGS                     = "Failed to index tags {0}";
    public static final String FAILED_TO_UPDATE_SAVE_TAG                = "Failed to update/save tag {0}";
    public static final String FAILED_TO_FIND_TAG                       = "Failed to find tag {0}";
    public static final String FAILED_TO_FIND_ALL_TAGS                  = "Failed to find all tags";
    public static final String FAILED_TO_CHECK_IF_TAG_EXISTS            = "Failed to check if tag exists {0}";
    public static final String FAILED_TO_DELETE_TAG                     = "Failed to delete tag {0}";

    public static final String USER_NOT_AUTHORIZED_ON_TAG               = "User does not have the proper authorization to perform an operation on this tag {0}";
    public static final String USER_NOT_AUTHORIZED_ON_TAGS              = "User does not have the proper authorization to perform an operation on these tags {0}";

    /**
     * This class is not to be instantiated.
     */
    private TextUtil() {
        throw new IllegalStateException("Utility class");
    }

}
