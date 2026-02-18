package org.phoebus.channelfinder.service.model.archiver.aa;

import java.util.List;

public record ArchiverInfo(String alias, String url, List<String> policies) {}
