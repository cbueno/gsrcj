package org.geopublishing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Ignore;
import org.junit.Test;

//@Ignore
public class GsRestTest extends GsRest {

	public GsRestTest() {
		super("http://localhost:8085/geoserver", "admin", "geoserver");
	}

	/**
	 */
	@Test
	public void testSendShape() throws MalformedURLException,
			ProtocolException, IOException, URISyntaxException {
		deleteWorkspace("ws", true);
		createWorkspace("ws");

		URL soilsZipFile = GsRestTest.class.getResource("/arabicData.zip");
		assertNotNull(soilsZipFile);

		// String sldString = RestUtil.readURLasString(soilsSldFile);

		String r = uploadShape("ws", "zipUpload_" + System.currentTimeMillis(),
				soilsZipFile);
		System.out.println(r);

	}

	@Test
	public void testParseDatastoresXml() throws MalformedURLException,
			ProtocolException, IOException {
		deleteWorkspace("ws", true);
		createWorkspace("ws");

		assertTrue(createDatastorePg("ws", "ds", "http://foo.bar.de",
				"localhost", "5432", "keck", "postgres", "secret", false));
		System.out.println(getDatastore("ws", "ds"));
		// System.out.println(getDatastore("foo", "fooPg"));
	}

	@Test
	public void testSendSld() throws IOException {

		URL soilsSldFile = GsRestTest.class.getResource("/soils.sld");
		String sldString = RestUtil.readURLasString(soilsSldFile);
		assertTrue(uploadSld("test_" + System.currentTimeMillis(), sldString));
	}

	@Test
	public void testSendAndDeleteSld() throws MalformedURLException,
			ProtocolException, IOException {
		deleteWorkspace("ws", true);
		createWorkspace("ws");

		String styleName = "test_" + System.currentTimeMillis();
		assertFalse(deleteSld(styleName + System.currentTimeMillis()));
		assertFalse(deleteSld(styleName + System.currentTimeMillis(), true));
		assertFalse(deleteSld(styleName + System.currentTimeMillis(), false));
		assertFalse(purgeSld(styleName + System.currentTimeMillis()));

		URL soilsSldFile = GsRestTest.class.getResource("/soils.sld");
		assertNotNull(soilsSldFile);

		String sldString = RestUtil.readURLasString(soilsSldFile);

		assertTrue(uploadSld(styleName, sldString));

		assertFalse(uploadSld(styleName, sldString));

		boolean deletedSld = deleteSld(styleName, true);
		System.out.println("deleted existing XXX.sld : " + deletedSld);

	}

	@Test
	public void testCreateDatastoreShapefile() throws IOException {
		boolean created = createDatastoreShapefile("ws",
				"testShape" + System.currentTimeMillis(), "http://test",
				"file:data/ad2/soils.shp", "ISO-8859-1");

		assertTrue(created);

		assertTrue(createDatastoreShapefile("ws",
				"testShape" + System.currentTimeMillis(), "http://test",
				"file:data/ad2/soils.shp", "UTF-8"));
	}

}
