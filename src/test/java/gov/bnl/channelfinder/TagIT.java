package gov.bnl.channelfinder;

import org.elasticsearch.client.Client;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TagIT {

	@Autowired
	private Client client;
	
	@Test
	public void simpleTest() {
		System.out.println(client);
	}

}
