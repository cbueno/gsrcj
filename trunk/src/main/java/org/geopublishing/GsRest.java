/*******************************************************************************
 * Copyright (c) 2010 Stefan A. Tzeggai
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-3.0.html
 * 
 * Contributors:
 *     Stefan A. Tzeggai - initial API and implementation
 ******************************************************************************/
package org.geopublishing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.misc.BASE64Encoder;

public class GsRest {

	final static Pattern workspaceNameRegEx = Pattern.compile(
			"<workspace>.*?<name>(.*?)</name>.*?</workspace>", Pattern.DOTALL);
	final static Pattern datastoreNameRegEx = Pattern.compile(
			"<dataStore>.*?<name>(.*?)</name>.*?</dataStore>", Pattern.DOTALL);
	final static Pattern coverageNameRegEx = Pattern.compile(
			"<coverage>.*?<name>(.*?)</name>.*?</coverage>", Pattern.DOTALL);
	final static Pattern coverageStoreNameRegEx = Pattern.compile(
			"<coverageStore>.*?<name>(.*?)</name>.*?</coverageStore>",
			Pattern.DOTALL);
	static final Pattern featuretypesNameRegEx = Pattern.compile(
			"<featureType>.*?<name>(.*?)</name>.*?</featureType>",
			Pattern.DOTALL + Pattern.MULTILINE);
	final static Pattern layerNamesRegExPattern = Pattern.compile(
			"<layer>.*?<name>(.*?)</name>.*?</layer>", Pattern.DOTALL
					+ Pattern.MULTILINE);
	final static Pattern coverageNamesRegExPattern = Pattern.compile(
			"<coverage>.*?<name>(.*?)</name>.*?</coverage>", Pattern.DOTALL
					+ Pattern.MULTILINE);
	/**
	 * <code>
	   <styles>
			<style>
			<name>point</name>
			<atom:link rel="alternate" href="http://localhost:8085/geoserver/rest/styles/point.xml" type="application/xml"/>
			</style>
			<style>
			<name>line</name>
			<atom:link rel="alternate" href="http://localhost:8085/geoserver/rest/styles/line.xml" type="application/xml"/>
			</style>
			...
		</styles>
		</code>
	 */
	static final Pattern stylesNameRegEx = Pattern.compile(
			"<style>.*?<name>(.*?)</name>.*?</style>", Pattern.DOTALL
					+ Pattern.MULTILINE);

	public final String METHOD_DELETE = "DELETE";
	public final String METHOD_GET = "GET";
	public final String METHOD_POST = "POST";

	public final String METHOD_PUT = "PUT";

	private String password;

	private String restUrl;

	private String username;

	/**
	 * Creates a {@link GsRest} instance to work on a Geoserver that allows
	 * anonymous read- and write access.
	 * 
	 * @param gsBaseUrl
	 *            The base URL of Geoserver. Usually ending with "../geoserver"
	 */
	public GsRest(String gsBaseUrl) {

		if (!gsBaseUrl.endsWith("rest")) {
			if (!gsBaseUrl.endsWith("/"))
				gsBaseUrl += "/";
			this.restUrl = gsBaseUrl + "rest";
		}

		this.username = null;
		this.password = null;
	}

	/**
	 * Creates a {@link GsRest} instance to work on a Geoserver which needs
	 * authorization (default in 2.0.2).
	 * 
	 * @param gsBaseUrl
	 *            The base URL of Geoserver. Usually ending with "../geoserver"
	 * @param username
	 *            plain text username
	 * @param password
	 *            plain text password
	 */
	public GsRest(String gsBaseUrl, String username, String password) {

		if (gsBaseUrl != null && !gsBaseUrl.endsWith("/"))
			gsBaseUrl += "/";

		this.restUrl = gsBaseUrl + "rest";
		this.username = username;
		this.password = password;
	}

	/**
	 * This method does not upload a shapefile via zip. It rather creates a
	 * reference to a Shapefile that has already exists in the GS data
	 * directory. <br/>
	 * 
	 * TODO: This is buggy and always puts the coveragestore in the default
	 * workspace. Therefore we set the default workspace defore every command,
	 * and reset it afterwards. This will change the default workspace for a
	 * moment!
	 * 
	 * @param relpath
	 *            A path to the file, relative to gsdata dir, e.g.
	 *            "file:data/water.shp"
	 */
	public boolean createCoverageGeoTiff(String wsName, String csName,
			String csNamespace, String relpath, Configure autoConfig)
			throws IOException {

		String oldDefault = getDefaultWs();

		try {
			setDefaultWs(wsName);

			if (relpath == null)
				throw new IllegalArgumentException(
						"parameter relpath may not be null");

			if (autoConfig == null)
				autoConfig = Configure.first;

			String urlParamter = "<url>" + relpath + "</url>";

			// String namespaceParamter = "<entry key=\"namespace\">" + dsName
			// + "</entry>";

			String typeParamter = "<type>GeoTIFF</type>";

			String xml = "<coverageStore><name>" + csName
					+ "</name><enabled>true</enabled>" + typeParamter
					+ urlParamter + "</coverageStore>";

			int returnCode = sendRESTint(METHOD_POST, "/workspaces/" + wsName
					+ "/coveragestores?configure=" + autoConfig.toString(), xml);
			return 201 == returnCode;
		} catch (IOException e) {
			setDefaultWs(oldDefault);
			throw e;
		} finally {
			reload();
		}

	}

	public boolean setDefaultWs(String wsName) throws IOException {
		String xml = "<workspace><name>" + wsName + "</name></workspace>";

		return 200 == sendRESTint(METHOD_PUT, "/workspaces/default.xml", xml);
	}

	/**
	 * Returns the name of the default workspace
	 * 
	 * @throws IOException
	 */
	public String getDefaultWs() throws IOException {
		String xml = sendRESTstring(METHOD_GET, "/workspaces/default", null);
		List<String> workspaces = parseXmlWithregEx(xml, workspaceNameRegEx);
		return workspaces.get(0);
	}

	public boolean createDatastorePg(String workspace, String dsName,
			String dsNamespace, String host, String port, String db,
			String user, String pwd, boolean exposePKs) throws IOException {

		String dbType = "postgis";

		return createDbDatastore(workspace, dsName, dsNamespace, host, port,
				db, user, pwd, dbType, exposePKs);
	}

	public boolean createDatastoreShapefile(String workspace, String dsName,
			String dsNamespace, String relpath, String chartset)
			throws IOException {

		return createDatastoreShapefile(workspace, dsName, dsNamespace,
				relpath, chartset, null, null, null);
	}

	/**
	 * − <dataStore> <name>xxx</name> <description>xxx</description>
	 * <type>Shapefile</type> <enabled>true</enabled> − <workspace>
	 * <name>ws</name> <atom:link rel="alternate"
	 * href="http://localhost:8085/geoserver/rest/workspaces/ws.xml"
	 * type="application/xml"/> </workspace> − <connectionParameters> <entry
	 * key="memory mapped buffer">true</entry> <entry
	 * key="create spatial index">true</entry> <entry
	 * key="charset">ISO-8859-1</entry> <entry
	 * key="url">file:data/ad2/soils.shp</entry> <entry
	 * key="namespace">http://ws</entry> </connectionParameters> −
	 * <featureTypes> <atom:link rel="alternate" href=
	 * "http://localhost:8085/geoserver/rest/workspaces/ws/datastores/xxx/featuretypes.xml"
	 * type="application/xml"/> </featureTypes> </dataStore>
	 */
	/**
	 * This method does not upload a shapefile via zip. It rather creates a
	 * reference to a Shapefile that has already exists in the GS data
	 * directory.
	 * 
	 * @param charset
	 *            defaults to UTF-8 if not set. Charset, that any text content
	 *            is stored in.
	 * 
	 * @param relpath
	 *            A path to the file, relative to gsdata dir, e.g.
	 *            "file:data/water.shp"
	 */
	public boolean createDatastoreShapefile(String workspace, String dsName,
			String dsNamespace, String relpath, String charset,
			Boolean memoryMappedBuffer, Boolean createSpatialIndex,
			Configure autoConfig) throws IOException {

		if (autoConfig == Configure.first)
			autoConfig = null;

		if (relpath == null)
			throw new IllegalArgumentException(
					"parameter relpath may not be null");

		String createSpatialIndexParam = RestUtil.entryKey(
				"create spatial index", createSpatialIndex);

		String memoryMappedBufferParamter = RestUtil.entryKey(
				"memory mapped buffer", memoryMappedBuffer);

		String charsetParamter = "<entry key=\"charset\">"
				+ (charset == null ? "UTF-8" : charset) + "</entry>";

		String urlParamter = "<entry key=\"url\">" + relpath + "</entry>";

		String namespaceParamter = "<entry key=\"namespace\">" + dsName
				+ "</entry>";

		String typeParamter = "<type>Shapefile</type>";

		String xml = "<dataStore><name>" + dsName
				+ "</name><enabled>true</enabled>" + typeParamter
				+ "<connectionParameters>" + createSpatialIndexParam
				+ memoryMappedBufferParamter + charsetParamter + urlParamter
				+ namespaceParamter + typeParamter
				+ "</connectionParameters></dataStore>";

		String configureParam = autoConfig == null ? "" : "?configure="
				+ autoConfig.toString();

		int returnCode = sendRESTint(METHOD_POST, "/workspaces/" + workspace
				+ "/datastores" + configureParam, xml);
		return 201 == returnCode;
	}

	public boolean createDbDatastore(String workspace, String dsName,
			String dsNamespace, String host, String port, String db,
			String user, String pwd, String dbType, boolean exposePKs)
			throws IOException {

		String exposePKsParamter = "<entry key=\"Expose primary keys\">"
				+ exposePKs + "</entry>";

		String xml = "<dataStore><name>" + dsName
				+ "</name><enabled>true</enabled><connectionParameters><host>"
				+ host + "</host><port>" + port + "</port><database>" + db
				+ "</database><user>" + user + "</user><passwd>" + pwd
				+ "</passwd><dbtype>" + dbType + "</dbtype><namespace>"
				+ dsNamespace + "</namespace>" + exposePKsParamter
				+ "</connectionParameters></dataStore>";

		int returnCode = sendRESTint(METHOD_POST, "/workspaces/" + workspace
				+ "/datastores", xml);
		return 201 == returnCode;
	}

	public boolean createFeatureType(String wsName, String dsName, String ftName)
			throws IOException {

		// "<entry key=\"namespace\"><name>" + dsName
		// + "</name></entry>";

		String xml = "<featureType><name>" + ftName + "</name><title>" + ftName
				+ "</title></featureType>";

		int sendRESTint = sendRESTint(METHOD_POST, "/workspaces/" + wsName
				+ "/datastores/" + dsName + "/featuretypes", xml);
		return 201 == sendRESTint;
	}

	/**
	 * Uploads an SLD to the Geoserver
	 * 
	 * @param stylename
	 * @param sldString
	 *            SLD-XML as String
	 * @return <code>true</code> successfully uploaded
	 */
	public boolean createSld(String stylename, String sldString)
			throws IOException {

		return null != createSld_location(stylename, sldString);
	}

	/**
	 * @param stylename
	 * @param sldString
	 * @return REST location URL string to the new style
	 * @throws IOException
	 */
	public String createSld_location(String stylename, String sldString)
			throws IOException {

		String location = sendRESTlocation(METHOD_POST, "/styles/" + "?name="
				+ stylename, sldString, "application/vnd.ogc.sld+xml",
				"application/vnd.ogc.sld+xml");
		return location;
	}

	public boolean createWorkspace(String workspaceName) throws IOException {
		return 201 == sendRESTint(METHOD_POST, "/workspaces",
				"<workspace><name>" + workspaceName + "</name></workspace>");
	}

	/**
	 * Deletes a datastore
	 * 
	 * @param wsName
	 *            name of the workspace
	 * @param dsName
	 *            name of the datastore
	 * @param recusively
	 *            delete all contained featureytpes also
	 */
	private boolean deleteDatastore(String wsName, String dsName,
			boolean recusively) throws IOException {
		if (recusively == true) {
			List<String> layerNames = getLayersUsingStore(wsName, dsName);

			for (String lName : layerNames) {
				if (!deleteLayer(lName))
					throw new RuntimeException("Could not delete layer "
							+ wsName + ":" + dsName + ":" + lName);
			}

			List<String> ftNames = getFeatureTypes(wsName, dsName);
			//
			for (String ftName : ftNames) {
				// it happens that this returns false, e.g maybe for
				// notpublished featuretypes!?
				deleteFeatureType(wsName, dsName, ftName);
			}
		}
		return 200 == sendRESTint(METHOD_DELETE, "/workspaces/" + wsName
				+ "/datastores/" + dsName, null);
	}

	/**
	 * Deletes a coveragestore
	 * 
	 * @param wsName
	 *            name of the workspace
	 * @param csName
	 *            name of the coveragestore
	 * @param recusively
	 *            delete all contained coverages also
	 * @throws IOException
	 */
	public boolean deleteCoveragestore(String wsName, String csName,
			boolean recusively) throws IOException {
		if (recusively == true) {
			List<String> layerNames = getLayersUsingCoverageStore(wsName,
					csName);

			for (String lName : layerNames) {
				if (!deleteLayer(lName))
					throw new RuntimeException("Could not delete layer "
							+ lName);
			}

			List<String> covNames = getCoverages(wsName, csName);
			//
			for (String ftName : covNames) {
				// it happens that this returns false, e.g maybe for
				// notpublished featuretypes!?
				deleteCoverage(wsName, csName, ftName);
			}
		}
		return 200 == sendRESTint(METHOD_DELETE, "/workspaces/" + wsName
				+ "/coveragestores/" + csName, null);
	}

	public boolean deleteCoverage(String wsName, String csName, String covName)
			throws IOException {

		int result = sendRESTint(METHOD_DELETE, "/workspaces/" + wsName
				+ "/coveragestores/" + csName + "/coverages/" + covName, null);

		return result == 200;
	}

	public boolean deleteFeatureType(String wsName, String dsName, String ftName)
			throws IOException {

		int result = sendRESTint(METHOD_DELETE, "/workspaces/" + wsName
				+ "/datastores/" + dsName + "/featuretypes/" + ftName, null);

		return result == 200;
	}

	private boolean deleteLayer(String lName) throws IOException {
		int result = sendRESTint(METHOD_DELETE, "/layers/" + lName, null);
		return result == 200;
	}

	public boolean deleteSld(String styleName, Boolean... purge_)
			throws IOException {
		Boolean purge = null;
		if (purge_.length > 1)
			throw new IllegalArgumentException(
					"only one purge paramter allowed");
		if (purge_.length == 1) {
			purge = purge_[0];
		}
		if (purge == null)
			purge = false;
		int result = sendRESTint(METHOD_DELETE, "/styles/" + styleName
				+ ".sld?purge=" + purge.toString(), null);
		// + "&name=" + styleName
		return result == 200;
	}

	/**
	 * Deletes an empty workspace. If the workspace is not empty or doesn't
	 * exist <code>false</code> is returned.
	 * 
	 * @param wsName
	 *            name of the workspace to delete
	 */
	public boolean deleteWorkspace(String wsName) throws IOException {
		return deleteWorkspace(wsName, false);
	}

	/**
	 * Deletes a workspace recursively. If the workspace could not be deleted
	 * (e.g. didn't exist, or not recursively deleting and not empty) returns
	 * <code>false</code>
	 * 
	 * @param wsName
	 *            name of the workspace to delete, including all content.
	 */
	public boolean deleteWorkspace(String wsName, boolean recursive) {

		try {

			if (recursive) {

				reload();

				// Selete all datastores
				// recusively
				List<String> datastores = getDatastores(wsName);
				for (String dsName : datastores) {
					if (!deleteDatastore(wsName, dsName, true))
						throw new IOException("Could not delete dataStore "
								+ dsName + " in workspace " + wsName);
				}

				// Selete all datastores
				// recusively
				List<String> coveragestores = getCoveragestores(wsName);
				for (String csName : coveragestores) {
					if (!deleteCoveragestore(wsName, csName, true))
						throw new IOException("Could not delete coverageStore "
								+ csName + " in workspace " + wsName);
				}

			}

			return 200 == sendRESTint(METHOD_DELETE, "/workspaces/" + wsName,
					null, "application/xml", "application/xml");
		} catch (IOException e) {
			// Workspace didn't exist
			return false;
		}
	}

	/**
	 * A list of coveragestores
	 */
	public List<String> getCoveragestores(String wsName) throws IOException {
		String coveragesXml = sendRESTstring(METHOD_GET, "/workspaces/"
				+ wsName + "/coveragestores.xml", null);
		List<String> coveragestores = parseXmlWithregEx(coveragesXml,
				coverageStoreNameRegEx);
		return coveragestores;
	}

	/**
	 * A list of datastorenames
	 */
	public List<String> getDatastores(String wsName) throws IOException {
		String datastoresXml = sendRESTstring(METHOD_GET, "/workspaces/"
				+ wsName + "/datastores", null);
		List<String> datastores = parseXmlWithregEx(datastoresXml,
				datastoreNameRegEx);
		return datastores;
	}

	/**
	 * A list of all workspaces
	 */
	public List<String> getWorkspaces() throws IOException {
		String xml = sendRESTstring(METHOD_GET, "/workspaces", null);
		List<String> workspaces = parseXmlWithregEx(xml, workspaceNameRegEx);
		return workspaces;
	}

	/**
	 * Tell this instance of {@link GsRest} to not use authorization
	 */
	public void disableAuthorization() {
		this.password = null;
		this.username = null;
	}

	/**
	 * Tell this {@link GsRest} instance to use authorization
	 * 
	 * @param username
	 *            cleartext username
	 * @param password
	 *            cleartext password
	 */
	public void enableAuthorization(String username, String password) {
		this.password = password;
		this.username = username;
	}

	public String getDatastore(String wsName, String dsName)
			throws MalformedURLException, ProtocolException, IOException {
		return sendRESTstring(METHOD_GET, "/workspaces/" + wsName
				+ "/datastores/" + dsName, null);
	}

	public String getFeatureType(String wsName, String dsName, String ftName)
			throws IOException {
		return sendRESTstring(METHOD_GET, "/workspaces/" + wsName
				+ "/datastores/" + dsName + "/featuretypes/" + ftName, null);
	}

	/**
	 * Returns a list of all featuretypes inside a a datastore
	 * 
	 * @param wsName
	 * @param dsName
	 * @throws IOException
	 */
	public List<String> getFeatureTypes(String wsName, String dsName)
			throws IOException {
		String xml = sendRESTstring(METHOD_GET, "/workspaces/" + wsName
				+ "/datastores/" + dsName + "/featuretypes", null);

		return parseXmlWithregEx(xml, featuretypesNameRegEx);

	}

	/**
	 * Returns a {@link List} of all layer names
	 * 
	 * @param wsName
	 */
	public List<String> getLayerNames() throws IOException {
		String xml = sendRESTstring(METHOD_GET, "/layers", null);
		return parseXmlWithregEx(xml, layerNamesRegExPattern);
	}

	/**
	 * Returns a list of all coverageNames inside a a coveragestore
	 */
	public List<String> getCoverages(String wsName, String csName)
			throws IOException {
		String xml = sendRESTstring(METHOD_GET, "/workspaces/" + wsName
				+ "/coveragestores/" + csName + "/coverages", null);

		return parseXmlWithregEx(xml, coverageNameRegEx);

	}

	/**
	 * Returns a list of all layers using a specific dataStore
	 */

	public List<String> getLayersUsingCoverageStore(String wsName, String csName)
			throws IOException {
		final Pattern pattern = Pattern.compile(
				"<layer>.*?<name>(.*?)</name>.*?/rest/workspaces/" + wsName
						+ "/coveragestores/" + csName
						+ "/coverages/.*?</layer>", Pattern.DOTALL
						+ Pattern.MULTILINE);

		List<String> coveragesUsingStore = new ArrayList<String>();
		for (String cName : getLayerNames()) {
			String xml = sendRESTstring(METHOD_GET, "/layers/" + cName, null);
			// System.out.println(xml);

			Matcher matcher = pattern.matcher(xml);
			if (matcher.find())
				coveragesUsingStore.add(cName);
		}

		return coveragesUsingStore;

	}

	/**
	 * Returns a list of all layers using a specific dataStore
	 */
	public List<String> getLayersUsingStore(String wsName, String dsName)
			throws IOException {

		final Pattern layersUsingStoreRegEx = Pattern
				.compile("<layer>.*?<name>(.*?)</name>.*?/rest/workspaces/"
						+ wsName + "/datastores/" + dsName
						+ "/featuretypes/.*?</layer>", Pattern.DOTALL
						+ Pattern.MULTILINE);

		List<String> layersUsingDs = new ArrayList<String>();
		for (String lName : getLayerNames()) {
			String xml = sendRESTstring(METHOD_GET, "/layers/" + lName, null);
			// System.out.println(xml);

			Matcher matcher = layersUsingStoreRegEx.matcher(xml);
			if (matcher.find())
				layersUsingDs.add(lName);
		}

		return layersUsingDs;

	}

	/**
	 * @return A list of all stylenames stored in geoserver. Includes "default"
	 *         stylenames like <code>point</code>,<code>line</code>,etc.
	 */
	public List<String> getStyles() throws IOException {
		String xml = sendRESTstring(METHOD_GET, "/styles", null);
		return parseXmlWithregEx(xml, stylesNameRegEx);
	}

	/**
	 * @return <code>true</code> if authorization is used for requests
	 */
	public boolean isAuthorization() {
		return password != null && username != null;
	}

	private List<String> parseXmlWithregEx(String xml, Pattern pattern) {
		ArrayList<String> list = new ArrayList<String>();

		Matcher nameMatcher = pattern.matcher(xml);
		while (nameMatcher.find()) {
			String name = nameMatcher.group(1);
			list.add(name.trim());
		}
		return list;
	}

	public boolean purgeSld(String styleName) throws IOException {
		return deleteSld(styleName, true);
	}

	private HttpURLConnection sendREST(String method, String urlAppend,
			Reader postDataReader, String contentType, String accept)
			throws MalformedURLException, IOException {
		boolean doOut = !METHOD_DELETE.equals(method) && postDataReader != null;
		// boolean doIn = true; // !doOut

		String link = restUrl + urlAppend;
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(doOut);
		// uc.setDoInput(false);
		if (contentType != null && !"".equals(contentType)) {
			connection.setRequestProperty("Content-type", contentType);
			connection.setRequestProperty("Content-Type", contentType);
		}
		if (accept != null && !"".equals(accept)) {
			connection.setRequestProperty("Accept", accept);
		}

		connection.setRequestMethod(method.toString());

		if (isAuthorization()) {
			String userPasswordEncoded = new BASE64Encoder().encode((username
					+ ":" + password).getBytes());
			connection.setRequestProperty("Authorization", "Basic "
					+ userPasswordEncoded);
		}

		connection.connect();
		if (connection.getDoOutput()) {
			Writer writer = new OutputStreamWriter(connection.getOutputStream());
			char[] buffer = new char[1024];

			Reader reader = new BufferedReader(postDataReader);
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}

			writer.flush();
			writer.close();
		}
		return connection;
	}

	private HttpURLConnection sendREST(String method, String urlEncoded,
			String postData, String contentType, String accept)
			throws MalformedURLException, IOException {
		StringReader postDataReader = postData == null ? null
				: new StringReader(postData);
		return sendREST(method, urlEncoded, postDataReader, contentType, accept);
	}

	public int sendRESTint(String method, String url, String xmlPostContent)
			throws IOException {
		return sendRESTint(method, url, xmlPostContent, "application/xml",
				"application/xml");
	}

	/**
	 * @param method
	 *            e.g. 'POST', 'GET', 'PUT' or 'DELETE'
	 * @param urlEncoded
	 *            e.g. '/workspaces' or '/workspaces.xml'
	 * @param contentType
	 *            format of postData, e.g. null or 'text/xml'
	 * @param accept
	 *            format of response, e.g. null or 'text/xml'
	 * @param postData
	 *            e.g. xml data
	 * @throws IOException
	 * @return null, or response of server
	 */
	public int sendRESTint(String method, String urlEncoded, String postData,
			String contentType, String accept) throws IOException {
		HttpURLConnection connection = sendREST(method, urlEncoded, postData,
				contentType, accept);

		String location = connection.getHeaderField("Location");

		return connection.getResponseCode();
	}

	/**
	 * @param method
	 *            e.g. 'POST', 'GET', 'PUT' or 'DELETE'
	 * @param urlEncoded
	 *            e.g. '/workspaces' or '/workspaces.xml'
	 * @param contentType
	 *            format of postData, e.g. null or 'text/xml'
	 * @param accept
	 *            format of response, e.g. null or 'text/xml'
	 * @param postData
	 *            e.g. xml data
	 * @return null, or location field of the response header
	 */
	public String sendRESTlocation(String method, String urlEncoded,
			String postData, String contentType, String accept)
			throws IOException {
		HttpURLConnection connection = sendREST(method, urlEncoded, postData,
				contentType, accept);

		return connection.getHeaderField("Location");
	}

	/**
	 * Sends a REST request and return the answer as a String.
	 * 
	 * @param method
	 *            e.g. 'POST', 'GET', 'PUT' or 'DELETE'
	 * @param urlEncoded
	 *            e.g. '/workspaces' or '/workspaces.xml'
	 * @param contentType
	 *            format of postData, e.g. null or 'text/xml'
	 * @param accept
	 *            format of response, e.g. null or 'text/xml'
	 * @param is
	 *            where to read the data from
	 * @throws IOException
	 * @return null, or response of server
	 */
	public String sendRESTstring(String method, String urlEncoded, Reader is,
			String contentType, String accept) throws IOException {
		HttpURLConnection connection = sendREST(method, urlEncoded, is,
				contentType, accept);

		// Read response
		InputStream in = connection.getInputStream();
		try {

			int len;
			byte[] buf = new byte[1024];
			StringBuffer sbuf = new StringBuffer();
			while ((len = in.read(buf)) > 0) {
				sbuf.append(new String(buf, 0, len));
			}
			return sbuf.toString();
		} finally {
			in.close();
		}
	}

	public String sendRESTstring(String method, String url,
			String xmlPostContent) throws IOException {
		return sendRESTstring(method, url, xmlPostContent, "application/xml",
				"application/xml");
	}

	/**
	 * Sends a REST request and return the answer as a String
	 * 
	 * @param method
	 *            e.g. 'POST', 'GET', 'PUT' or 'DELETE'
	 * @param urlEncoded
	 *            e.g. '/workspaces' or '/workspaces.xml'
	 * @param contentType
	 *            format of postData, e.g. null or 'text/xml'
	 * @param accept
	 *            format of response, e.g. null or 'text/xml'
	 * @param postData
	 *            e.g. xml data
	 * @throws IOException
	 * @return null, or response of server
	 */
	public String sendRESTstring(String method, String urlEncoded,
			String postData, String contentType, String accept)
			throws IOException {
		HttpURLConnection connection = sendREST(method, urlEncoded, postData,
				contentType, accept);

		// Read response
		InputStream in = connection.getInputStream();
		try {

			int len;
			byte[] buf = new byte[1024];
			StringBuffer sbuf = new StringBuffer();
			while ((len = in.read(buf)) > 0) {
				sbuf.append(new String(buf, 0, len));
			}
			return sbuf.toString();
		} finally {
			in.close();
		}
	}

	/**
	 * Works: curl -u admin:geoserver -v -XPUT -H 'Content-type:
	 * application/zip' --data-binary @/home/stefan/Desktop/arabicData.zip
	 * http:/
	 * /localhost:8085/geoserver/rest/workspaces/ws/datastores/test1/file.shp
	 */
	protected String uploadShape(String workspace, String dsName, URL zip)
			throws IOException {

		InputStream os = zip.openStream();
		try {
			InputStreamReader postDataReader = new InputStreamReader(os);

			String returnString = sendRESTstring(METHOD_PUT, "/workspaces/"
					+ workspace + "/datastores/" + dsName + "/file.shp",
					postDataReader, "application/zip", null);

			// "?configure=all"
			return returnString;
		} finally {
			os.close();
		}
	}

	/**
	 * @throws IOException
	 */
	public boolean createCoverage(String wsName, String csName, String cName)
			throws IOException {

		// "<entry key=\"namespace\"><name>" + dsName
		// + "</name></entry>";

		String xml = "<coverage><name>" + cName + "</name><title>" + cName
				+ "</title></coverage>";

		int sendRESTint = sendRESTint(METHOD_POST, "/workspaces/" + wsName
				+ "/coveragestores/" + csName + "/coverages", xml);

		return 201 == sendRESTint;
	}

	public boolean reload() throws IOException {
		int sendRESTint = sendRESTint(METHOD_POST, "/reload", null);
		return 201 == sendRESTint;
	}

}
