package gov.bnl.channelfinder;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class MyUser extends User{

	   //This constructor is a must
	    public MyUser(String username, String password, Collection<? extends GrantedAuthority> authorities) {
	        super(username, password, authorities);
	    }
	    //Setter and getters are required
	    private String firstName;
	    public String getFirstName() {
			return firstName;
		}
		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
		public String getLastName() {
			return lastName;
		}
		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
		public List<String> getGroups() {
			return groups;
		}
		public void setGroups(List<String> groups) {
			this.groups = groups;
		}
		private String lastName;
	    private List<String> groups;

	}