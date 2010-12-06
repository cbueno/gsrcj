package org.geopublishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class GsRestTest extends GsRest {

	public GsRestTest() {
		super(GsTestingUtil.getUrl(), GsTestingUtil.getUsername(),
				GsTestingUtil.getPassword());
	}

	/**
	 */
	@Test
	@Ignore
	public void testSendShape() throws MalformedURLException,
			ProtocolException, IOException, URISyntaxException {
		if (!GsTestingUtil.isAvailable())
			return;

		deleteWorkspace("ws", true);
		createWorkspace("ws");

		URL soilsZipFile = GsRestTest.class.getResource("/points.zip");
		assertNotNull(soilsZipFile);
		soilsZipFile.openStream().close();

		String r = uploadShape("ws", "zipUpload_" + System.currentTimeMillis(),
				soilsZipFile);
		System.out.println(r);

	}

	@Test
	public void testParseDatastoresXml() throws MalformedURLException,
			ProtocolException, IOException {
		if (!GsTestingUtil.isAvailable())
			return;

		deleteWorkspace("ws", true);
		createWorkspace("ws");

		assertTrue(createDatastorePg("ws", "ds", "http://foo.bar.de",
				"localhost", "5432", "keck", "postgres", "secret", false));
		System.out.println(getDatastore("ws", "ds"));
	}

	@Test
	public void testSendSld() throws IOException {
		if (!GsTestingUtil.isAvailable())
			return;

		URL soilsSldFile = GsRestTest.class.getResource("/soils.sld");
		String sldString = RestUtil.readURLasString(soilsSldFile);
		assertTrue(createSld("test_" + System.currentTimeMillis(), sldString));
	}

	@Test
	public void testSendAndDeleteSld() throws MalformedURLException,
			ProtocolException, IOException {
		if (!GsTestingUtil.isAvailable())
			return;

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

		assertTrue(createSld(styleName, sldString));

		// assertFalse(createSld(styleName, sldString));

		assertTrue(deleteSld(styleName, true));
		// System.out.println("deleted existing XXX.sld : " + deletedSld);

		assertFalse(deleteSld(styleName, true));

		// PURGE IS NOT WORKING! :-(
	}

	@Test
	public void testCreateDatastoreShapefile() throws IOException {
		if (!GsTestingUtil.isAvailable())
			return;

		final String dsName = "testShape" + System.currentTimeMillis();
		boolean created = createDatastoreShapefile("ws", dsName, "http://test",
				"file:data/ad2/soils.shp", "ISO-8859-1");
		assertTrue(created);

		final String ftName = "newtestftnmae.shp";
		boolean created2 = createFeatureType("ws", dsName, ftName);
		assertTrue(created2);

		assertTrue("No featureType created after datastore creation",
				getFeatureTypes("ws", dsName).contains(ftName));

		assertTrue(deleteFeatureType("ws", dsName, ftName));

		assertFalse(getFeatureTypes("ws", dsName).contains(ftName));
	}

	@Test
	public void testCreateCoverageGeoTiff() throws IOException {
		createWorkspace("another_maybedefault");
		createWorkspace("ws");
		final String csName = "testRaster_mean" + System.currentTimeMillis();
		boolean created = createCoverageGeoTiff("ws", csName, "http://test",
				"file:data/iida/raster_mean_utm200263259529/mean_utm2.tif",
				Configure.first);

		assertTrue(created);

		assertTrue(createCoverage("ws", csName, csName));

		assertTrue(getCoveragestores("ws").contains(csName));

		deleteCoveragestore("ws", csName, true);

		assertFalse(getCoveragestores("ws").contains(csName));

	}

	@Test
	public void testGetStyles() throws IOException {
		List<String> s = getStyles();
		assertNotNull(s);
	}

	@Test
	public void testSetDefaultWs() throws IOException {
		createWorkspace("a");
		setDefaultWs("a");
		assertEquals("a", getDefaultWs());
		createWorkspace("b");
		assertEquals("a", getDefaultWs());
		setDefaultWs("b");
		assertEquals("b", getDefaultWs());

	}

}
