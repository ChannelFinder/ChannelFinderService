package org.phoebus.channelfinder.processors.aa;

public enum ArchiveAction {
  ARCHIVE("/archivePV"),
  PAUSE("/pauseArchivingPV"),
  RESUME("/resumeArchivingPV"),
  NONE("");

  private final String endpoint;

  ArchiveAction(final String endpoint) {
    this.endpoint = endpoint;
  }

  public String getEndpoint() {
    return this.endpoint;
  }
}
