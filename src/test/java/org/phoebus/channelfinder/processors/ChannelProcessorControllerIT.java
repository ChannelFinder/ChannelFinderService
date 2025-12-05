package org.phoebus.channelfinder.processors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.phoebus.channelfinder.common.CFResourceDescriptors;
import org.phoebus.channelfinder.entity.Scroll;
import org.phoebus.channelfinder.rest.api.IChannelScroll;
import org.phoebus.channelfinder.rest.controller.ChannelProcessorController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ChannelProcessorController.class)
@TestPropertySource(
    value = "classpath:application_test.properties",
    properties = {"elasticsearch.create.indices = false"})
class ChannelProcessorControllerIT {

  protected static final String AUTHORIZATION =
      "Basic " + Base64.getEncoder().encodeToString("admin:adminPass".getBytes());

  @Autowired protected MockMvc mockMvc;
  @MockBean IChannelScroll channelScroll;

  @Test
  void testProcessorCount() throws Exception {
    MockHttpServletRequestBuilder request =
        get("/" + CFResourceDescriptors.CHANNEL_PROCESSOR_RESOURCE_URI + "/count");
    mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().string("2"));
  }

  @Test
  void testProcessorsInfo() throws Exception {
    MockHttpServletRequestBuilder request =
        get("/" + CFResourceDescriptors.CHANNEL_PROCESSOR_RESOURCE_URI + "/processors");
    mockMvc
        .perform(request)
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].name", containsInAnyOrder("AAChannelProcessor", "DummyProcessor")));
  }

  @Test
  void testProcessorEnabled() throws Exception {
    MockHttpServletRequestBuilder request =
        put("/"
                + CFResourceDescriptors.CHANNEL_PROCESSOR_RESOURCE_URI
                + "/processor/AAChannelProcessor/enabled")
            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
            .contentType("application/json")
            .content("{\"enabled\": false}");
    mockMvc.perform(request).andExpect(status().isOk());
  }

  @Test
  void testProcessAllChannels() throws Exception {
    Mockito.when(channelScroll.query(Mockito.any())).thenReturn(new Scroll("", List.of()));

    MockHttpServletRequestBuilder request =
        put("/" + CFResourceDescriptors.CHANNEL_PROCESSOR_RESOURCE_URI + "/process/all")
            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION);
    mockMvc.perform(request).andExpect(status().isOk());
  }

  @Test
  void testProcessQuery() throws Exception {
    Mockito.when(channelScroll.query(Mockito.any())).thenReturn(new Scroll("", List.of()));
    MockHttpServletRequestBuilder request =
        put("/" + CFResourceDescriptors.CHANNEL_PROCESSOR_RESOURCE_URI + "/process/query")
            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION);
    mockMvc.perform(request).andExpect(status().isOk());
  }
}
