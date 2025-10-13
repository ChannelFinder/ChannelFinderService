package org.phoebus.channelfinder.processors.aa;

import java.util.List;

public record ArchiverInfo(String alias, String url, String version, List<String> policies) {}
