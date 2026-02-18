package org.phoebus.channelfinder.service.model.archiver.aa;

public enum ArchiveAction {
  ARCHIVE("/archivePV", "Archive request submitted"),
  PAUSE("/pauseArchivingPV", "ok"),
  RESUME("/resumeArchivingPV", "ok"),
  NONE("", "ok");

  private final String endpoint;
  private final String successfulStatus;

  ArchiveAction(final String endpoint, final String successfulStatus) {
    this.endpoint = endpoint;
    this.successfulStatus = successfulStatus;
  }

  public String getEndpoint() {
    return this.endpoint;
  }

  public String getSuccessfulStatus() {
    return this.successfulStatus;
  }
}
