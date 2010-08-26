package org.geopublishing;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;

import org.junit.Test;

public class GsRestTest extends GsRest {

	public GsRestTest() {
		super("http://localhost:8085/geoserver", "admin", "geoserver");
	}

	@Test
	public void testParseDatastoresXml() throws MalformedURLException,
			ProtocolException, IOException {
//		deleteWorkspace("ws", true);
//		createWorkspace("ws");
//
//		assertTrue(createDatastorePg("ws", "ds", "http://keck.cpa.de",
//				"localhost", "5432", "keck", "postgres", "secretIRI69.", false));

//		System.out.println(getDatastore("ws", "ds"));
//		
//		System.out.println(getDatastore("keck", "keckPg"));

		// String xml =
		// "<dataStores>  <dataStore>    <name>keckPg</name>    <atom:link xmlns:atom=\"http://www.w3.org/2005/Atom\" rel=\"alternate\" href=\"http://localhost:8085/geoserver/rest/workspaces/testWorkspace1276269538724/datastores/keckPg.xml\" type=\"application/xml\"/>  </dataStore></dataStores><coverageStores/>";
		// List<String> parseDatastoresXml = parseDatastoresXml(xml);
		//
		// assertEquals(1, parseDatastoresXml.size());
		// assertEquals("keckPg", parseDatastoresXml.get(0));

	}

}
