package org.phoebus.channelfinder.service.model.archiver.aa;

import java.util.List;

public record ArchiverPolicies(List<String> policies) {
  @Override
  public String toString() {
    return policies.size() + " policies";
  }
}
