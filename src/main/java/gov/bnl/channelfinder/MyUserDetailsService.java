package gov.bnl.channelfinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.springframework.context.annotation.Bean;
//import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class MyUserDetailsService implements UserDetailsService {

	@Override
	public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
		
		URL url = getClass().getResource("users.txt");
		File file = new File(url.getPath());
		Scanner scanner;
		String user = null;

		try {
			scanner = new Scanner(file);

			while(scanner.hasNextLine())
			{
				user = scanner.nextLine();
				if(user.split(" ")[0].equals(username))
					break;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//The magic is happen in this private method !
		return buildUserForAuthentication(user);

	}


	//Fill your extended User object (CurrentUser) here and return it
	private User buildUserForAuthentication(String user) {

		String[] info = user.split(" ");

		String username = info[0];
		String password = info[1];
	    List<GrantedAuthority> setAuths = new ArrayList<GrantedAuthority>();
	    setAuths.add(new SimpleGrantedAuthority(info[2]));
		List<GrantedAuthority> authorities = setAuths;
		List<String> groups = new ArrayList<String>();

		for(int i = 3; i < info.length; i++)
		{
			groups.add(info[i]);
		}

		MyUser new_user = new MyUser(username, encoder().encode(password), authorities);
		new_user.setGroups(groups);

		return new_user;
	}
	
	@Bean
	public PasswordEncoder encoder() {
		return new BCryptPasswordEncoder();
	}

}

