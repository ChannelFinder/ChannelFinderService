package org.phoebus.channelfinder.web.v0.controller;

import org.phoebus.channelfinder.service.InfoService;
import org.phoebus.channelfinder.web.v0.api.IInfo;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
@RequestMapping("#{@legacyApiProperties.serviceRoot}")
public class InfoController implements IInfo {

  private final InfoService infoService;

  public InfoController(InfoService infoService) {
    this.infoService = infoService;
  }

  @Override
  public String info() {
    return infoService.info();
  }
}
