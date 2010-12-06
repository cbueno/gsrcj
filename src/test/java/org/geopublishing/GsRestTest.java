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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class GsRestTest extends GsRest {

	public GsRestTest() {
		super(GsTestingUtil.getUrl(), GsTestingUtil.getUsername(),
				GsTestingUtil.getPassword());
	}

	@Before
	public void before() throws IOException {
		deleteWorkspace("ws");
		createWorkspace("ws");
	}

	@After
	public void after() throws IOException {
		deleteWorkspace("ws");
	}

	/**
	 */
	@Test
	@Ignore
	public void testSendShape() throws MalformedURLException,
			ProtocolException, IOException, URISyntaxException {
		if (!GsTestingUtil.isAvailable())
			return;

		// deleteWorkspace("ws");
		// createWorkspace("ws");

		URL soilsZipFile = GsRestTest.class.getResource("/points.zip");
		assertNotNull(soilsZipFile);
		soilsZipFile.openStream().close();

		String r = uploadShape("ws", "zipUpload_" + System.currentTimeMillis(),
				soilsZipFile);
		System.out.println(r);

		// deleteWorkspace("ws");
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

		// deleteWorkspace("ws");
		// assertFalse(getWorkspaces().contains("ws"));
		// createWorkspace("ws");
		assertTrue(getWorkspaces().contains("ws"));

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

	}

	String nativeWktPoints = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]]";

	@Test
	public void testCreateDatastoreShapefile() throws IOException {
		if (!GsTestingUtil.isAvailable())
			return;

		final String dsName = "points";

		URL pointsShpUrl = GsRestTest.class.getResource("/points.shp");
		assertNotNull(pointsShpUrl);
		assertEquals("file", pointsShpUrl.getProtocol());

		boolean created = createDatastoreShapefile("ws", dsName, "http://test",
				pointsShpUrl.toString(), "ISO-8859-1");
		assertTrue(created);

		final String ftName = "points";
		boolean created2 = createFeatureType("ws", dsName, ftName,
				"EPSG:32631", nativeWktPoints);
		assertTrue(created2);

		assertTrue(getFeatureTypes("ws", dsName).contains(ftName));

		assertTrue(getLayerNames().contains(ftName));

		URL soilsSldFile = GsRestTest.class.getResource("/soils.sld");
		String sldString = RestUtil.readURLasString(soilsSldFile);
		final String stylename = "test_" + System.currentTimeMillis();
		assertTrue(createSld(stylename, sldString));

		assertEquals(0, getStylesForLayer(ftName).size());

		assertTrue(addStyleToLayer(stylename, ftName));

		assertTrue(getStylesForLayer(ftName).contains(stylename));

		assertTrue(deleteLayer(ftName));
		assertTrue(deleteFeatureType("ws", dsName, ftName));

		assertFalse(getLayerNames().contains(ftName));
		assertFalse(getFeatureTypes("ws", dsName).contains(ftName));
	}

	@Test
	@Ignore
	public void testCreateSameLayerInDifferentWorkspaces() throws IOException {
		if (!GsTestingUtil.isAvailable())
			return;

		final String dsName = "points";

		assertTrue(deleteWorkspace("ws1"));
		assertTrue(deleteWorkspace("ws2"));
		createWorkspace("ws1");
		createWorkspace("ws2");

		URL pointsShpUrl = GsRestTest.class.getResource("/points.shp");
		assertNotNull(pointsShpUrl);
		assertEquals("file", pointsShpUrl.getProtocol());

		assertTrue(createDatastoreShapefile("ws1", dsName, "http://testWs1",
				pointsShpUrl.toString(), "ISO-8859-1"));

		assertTrue(createDatastoreShapefile("ws2", dsName, "http://testWs2",
				pointsShpUrl.toString(), "ISO-8859-1"));

		final String ftName = "points";
		assertTrue(createFeatureType("ws1", dsName, ftName, "EPSG:32631",
				nativeWktPoints));
		assertTrue(createFeatureType("ws2", dsName, ftName, "EPSG:32631",
				nativeWktPoints));

		assertTrue(getLayerNames().contains(ftName));

		assertTrue(deleteWorkspace("ws1"));
		assertTrue(deleteWorkspace("ws2"));
	}

	@Test
	public void testCreateCoverageGeoTiff() throws IOException {
		if (!GsTestingUtil.isAvailable())
			return;

		URL geotiffUrl = GsRestTest.class.getResource("/geotiffwithsld.tif");
		assertNotNull(geotiffUrl);
		assertEquals("file", geotiffUrl.getProtocol());

		createWorkspace("another_maybedefault");

		String wsName = "wsgeotiff";
		deleteWorkspace(wsName);
		createWorkspace(wsName);

		final String csName = wsName + "_" + "geotiffwithsld"
				+ System.currentTimeMillis();
		assertTrue(createCoverageGeoTiff(wsName, csName, "http://test",
				geotiffUrl.toString(), Configure.first));

		String cName = "geotiffwithsld";
		assertTrue(getCoveragestores(wsName).contains(csName));

		assertTrue(createCoverage(wsName, csName, cName));
		assertTrue(getCoverages(wsName, csName).contains(cName));

		deleteCoverage(wsName, csName, cName);
		assertFalse(getCoverages(wsName, csName).contains(cName));

		deleteCoveragestore(wsName, csName, true);
		assertFalse(getCoveragestores(wsName).contains(csName));

		assertTrue(deleteWorkspace(wsName));
		assertTrue(deleteWorkspace("another_maybedefault"));

	}

	@Test
	@Ignore
	public void testCreateSameCoverageInTwoWorkspaces() throws IOException {
		if (!GsTestingUtil.isAvailable())
			return;
		URL geotiffUrl = GsRestTest.class.getResource("/geotiffwithsld.tif");
		assertNotNull(geotiffUrl);
		assertEquals("file", geotiffUrl.getProtocol());
		final String csName = "geotiffwithsld" + System.currentTimeMillis();
		String cName = "geotiffwithsld";

		deleteWorkspace("ws1");
		deleteWorkspace("ws2");

		createWorkspace("ws1");
		createWorkspace("ws2");

		assertTrue(createCoverageGeoTiff("ws1", csName, "http://test1",
				geotiffUrl.toString(), Configure.first));
		assertTrue(getCoveragestores("ws1").contains(csName));

		assertTrue(createCoverageGeoTiff("ws2", csName, "http://test2",
				geotiffUrl.toString(), Configure.first));
		assertTrue(getCoveragestores("ws2").contains(csName));

		assertTrue(createCoverage("ws1", csName, cName));

		reload();

		assertTrue(getCoverages("ws1", csName).contains(cName));
		assertTrue(createCoverage("ws2", csName, cName));
		assertTrue(getCoverages("ws2", csName).contains(cName));

		deleteCoverage("ws1", csName, cName);
		assertFalse(getCoverages("ws1", csName).contains(cName));

		deleteCoveragestore("ws1", csName, true);
		assertFalse(getCoveragestores("ws1").contains(csName));
		deleteCoveragestore("ws2", csName, true);
		assertFalse(getCoveragestores("ws2").contains(csName));

		assertTrue(deleteWorkspace("ws1"));
		assertTrue(deleteWorkspace("ws2"));

	}

	@Test
	public void testGetStyles() throws IOException {
		if (!GsTestingUtil.isAvailable())
			return;
		List<String> s = getStyles();
		assertNotNull(s);
	}

	@Test
	public void testSetDefaultWs() throws IOException {
		if (!GsTestingUtil.isAvailable())
			return;
		createWorkspace("a");
		setDefaultWs("a");
		assertEquals("a", getDefaultWs());
		createWorkspace("b");
		assertEquals("a", getDefaultWs());
		setDefaultWs("b");
		assertEquals("b", getDefaultWs());

		deleteWorkspace("a");
		deleteWorkspace("b");
	}

}
