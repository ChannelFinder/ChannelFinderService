package org.phoebus.channelfinder.web.v0.controller;

import java.util.Map;
import org.phoebus.channelfinder.common.SearchParameterMergerUtil;
import org.phoebus.channelfinder.entity.Scroll;
import org.phoebus.channelfinder.service.ChannelScrollService;
import org.phoebus.channelfinder.web.v0.api.IChannelScroll;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
@RequestMapping("${channelfinder.legacy.service-root:ChannelFinder}/resources/scroll")
public class ChannelScrollController implements IChannelScroll {

  private final ChannelScrollService channelScrollService;

  public ChannelScrollController(ChannelScrollService channelScrollService) {
    this.channelScrollService = channelScrollService;
  }

  @Override
  public Scroll query(
      MultiValueMap<String, String> allRequestParams, Map<String, String> searchParamsBody) {
    MultiValueMap<String, String> mergedParams =
        SearchParameterMergerUtil.mergeParameters(allRequestParams, searchParamsBody);
    return channelScrollService.search(null, mergedParams);
  }

  @Override
  public Scroll query(
      String scrollId,
      MultiValueMap<String, String> searchParameters,
      Map<String, String> searchParamsBody) {
    MultiValueMap<String, String> mergedParams =
        SearchParameterMergerUtil.mergeParameters(searchParameters, searchParamsBody);
    return channelScrollService.search(scrollId, mergedParams);
  }
}
