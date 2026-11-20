package org.phoebus.channelfinder.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.entity.Property;
import org.phoebus.channelfinder.entity.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

  private static final String ROLE_PREFIX = "ROLE_";

  private List<String> adminGroups;
  private List<String> channelGroups;
  private List<String> propertyGroups;
  private List<String> tagGroups;

  @Value("${admin-groups:cf-admins}")
  void initializeAdminRoles(String groups) {
    adminGroups =
        Arrays.asList(groups.split(",")).stream()
            .map(g -> ROLE_PREFIX + g.trim().toUpperCase())
            .collect(Collectors.toList());
  }

  @Value("${channel-groups:cf-channels}")
  void initializeChannelModRoles(String groups) {
    channelGroups =
        Arrays.asList(groups.split(",")).stream()
            .map(g -> ROLE_PREFIX + g.trim().toUpperCase())
            .collect(Collectors.toList());
  }

  @Value("${property-groups:cf-properties}")
  void initializePropertyRoles(String groups) {
    propertyGroups =
        Arrays.asList(groups.split(",")).stream()
            .map(g -> ROLE_PREFIX + g.trim().toUpperCase())
            .collect(Collectors.toList());
  }

  @Value("${tag-groups:cf-tags}")
  void initializeTagRoles(String groups) {
    tagGroups =
        Arrays.asList(groups.split(",")).stream()
            .map(g -> ROLE_PREFIX + g.trim().toUpperCase())
            .collect(Collectors.toList());
  }

  public enum ROLES {
    CF_ADMIN,
    CF_CHANNEL,
    CF_PROPERTY,
    CF_TAG
  }

  public boolean isAuthorizedOwner(Authentication authentication, Tag data) {
    ArrayList<String> auth = new ArrayList<>();
    Collection<? extends GrantedAuthority> auths = authentication.getAuthorities();
    for (GrantedAuthority a : auths) auth.add(a.getAuthority());

    if (!Collections.disjoint(auth, adminGroups)) return true;
    if (authentication.getName().equals(data.getOwner())
        || auth.contains(ROLE_PREFIX + data.getOwner().trim().toUpperCase())) return true;
    return false;
  }

  public boolean isAuthorizedOwner(Authentication authentication, Property data) {
    ArrayList<String> auth = new ArrayList<>();
    Collection<? extends GrantedAuthority> auths = authentication.getAuthorities();
    for (GrantedAuthority a : auths) auth.add(a.getAuthority());

    if (!Collections.disjoint(auth, adminGroups)) return true;
    if (authentication.getName().equals(data.getOwner())
        || auth.contains(ROLE_PREFIX + data.getOwner().trim().toUpperCase())) return true;
    return false;
  }

  public boolean isAuthorizedOwner(Authentication authentication, Channel data) {
    ArrayList<String> auth = new ArrayList<>();
    Collection<? extends GrantedAuthority> auths = authentication.getAuthorities();
    for (GrantedAuthority a : auths) auth.add(a.getAuthority());

    if (!Collections.disjoint(auth, adminGroups)) return true;
    if (authentication.getName().equals(data.getOwner())
        || auth.contains(ROLE_PREFIX + data.getOwner().trim().toUpperCase())) return true;
    return false;
  }

  public boolean isAuthorizedRole(Authentication authentication, ROLES expectedRole) {
    ArrayList<String> auth = new ArrayList<>();
    Collection<? extends GrantedAuthority> auths = authentication.getAuthorities();
    for (GrantedAuthority a : auths) auth.add(a.getAuthority());

    if (!Collections.disjoint(auth, adminGroups)) return true;
    else if (!Collections.disjoint(auth, channelGroups) && expectedRole != ROLES.CF_ADMIN)
      return true;
    else if (!Collections.disjoint(auth, propertyGroups)
        && (expectedRole == ROLES.CF_PROPERTY || expectedRole == ROLES.CF_TAG)) return true;
    else if (!Collections.disjoint(auth, tagGroups) && expectedRole == ROLES.CF_TAG) return true;
    return false;
  }
}
