package gov.bnl.channelfinder;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

public class CustomUserDetails implements LdapUserDetails {
    private String iin;
    private String colvirId;
    private LdapUserDetails details;

    public CustomUserDetails(LdapUserDetails details) {
        this.details = details;
    }

    public String getIin() {
        return iin;
    }

    public void setIin(String iin) {
        this.iin = iin;
    }

    public String getColvirId() {
        return colvirId;
    }

    public void setColvirId(String colvirId) {
        this.colvirId = colvirId;
    }

    @Override
    public String getDn() {
        return details.getDn();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return details.getAuthorities();
    }

    @Override
    public String getPassword() {
        return details.getPassword();
    }

    @Override
    public String getUsername() {
        return details.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return details.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return details.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return details.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return details.isEnabled();
    }

	@Override
	public void eraseCredentials() {
		// TODO Auto-generated method stub
		
	}
}