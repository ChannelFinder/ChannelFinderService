package org.phoebus.channelfinder.common;

public class CFResourceDescriptors {

  public static final String CF_SERVICE = "ChannelFinder";
  public static final String CF_SERVICE_INFO = CF_SERVICE;
  public static final String TAG_RESOURCE_URI = CF_SERVICE + "/resources/tags";
  public static final String PROPERTY_RESOURCE_URI = CF_SERVICE + "/resources/properties";
  public static final String CHANNEL_RESOURCE_URI = CF_SERVICE + "/resources/channels";
  public static final String SCROLL_RESOURCE_URI = CF_SERVICE + "/resources/scroll";
  public static final String CHANNEL_PROCESSOR_RESOURCE_URI = CF_SERVICE + "/resources/processors";

  public static final String SEARCH_PARAM_DESCRIPTION =
      "Search parameters. Examples:\n"
          + "- ~name: Filter by channel name (e.g., ~name=SR*)\n"
          + "- ~tag: Filter by tag name, use ! to negate (e.g., ~tag=active)\n"
          + "- ~size: Number of results (e.g., ~size=100)\n"
          + "- ~from: Starting index (e.g., ~from=0)\n"
          + "Use |,; as value separators";
}
