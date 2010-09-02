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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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

	public final String METHOD_POST = "POST";
	public final String METHOD_GET = "GET";
	public final String METHOD_PUT = "PUT";
	public final String METHOD_DELETE = "DELETE";

	private String password;
	private String username;
	private String restUrl;

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

		if (!gsBaseUrl.endsWith("/"))
			gsBaseUrl += "/";

		this.restUrl = gsBaseUrl + "rest";
		this.username = username;
		this.password = password;
	}

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
	 * @return <code>true</code> if authorization is used for requests
	 */
	public boolean isAuthorization() {
		return password != null && username != null;
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

	/**
	 * Tell this instance of {@link GsRest} to not use authorization
	 */
	public void disableAuthorization() {
		this.password = null;
		this.username = null;
	}

	/**
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
	public int sendRESTint(String method, String urlEncoded, String postData,
			String contentType, String accept) throws IOException {
		HttpURLConnection connection = sendREST(method, urlEncoded, postData,
				contentType, accept);

		return connection.getResponseCode();
	}

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

	private HttpURLConnection sendREST(String method, String urlAppend,
			String postData, String contentType, String accept)
			throws MalformedURLException, IOException, ProtocolException {
		boolean doOut = !METHOD_DELETE.equals(method) && postData != null;
		// boolean doIn = true; // !doOut

		String link = restUrl + urlAppend;
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(doOut);
		// uc.setDoInput(false);
		if (contentType != null && !"".equals(contentType)) {
			connection.setRequestProperty("Content-type", contentType);
		}
		if (accept != null && !"".equals(accept)) {
			connection.setRequestProperty("Accept", accept);
		}

		connection.setRequestMethod(method.toString());
		// type XML
		connection.setRequestProperty("Content-Type", contentType);

		if (isAuthorization()) {
			String userPasswordEncoded = new BASE64Encoder().encode((username
					+ ":" + password).getBytes());
			connection.setRequestProperty("Authorization", "Basic "
					+ userPasswordEncoded);
		}

		if (connection.getDoOutput()) {
			Writer writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(postData);
			writer.flush();
			writer.close();
		}
		connection.connect();
		return connection;
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
	 * Deletes a workspace recursively. If the workspace doesn't exist it throws
	 * an {@link IOException}. <code>false</code> is returned if the workspace
	 * is not empty.
	 * 
	 * @param wsName
	 *            name of the workspace to delete, including all content.
	 * @throws IOException
	 */
	public boolean deleteWorkspace(String wsName, boolean recursive)
			throws IOException {

		try {

			if (recursive) {

				// Check if the workspace exists and delete all datastores
				// recusively
				String datastoresXml = sendRESTstring(METHOD_GET,
						"/workspaces/" + wsName + "/datastores", null);
				List<String> datastores = parseXmlWithregEx(datastoresXml,
						datastoreNameRegEx);
				for (String dsName : datastores) {
					if (!deleteDatastore(wsName, dsName, true))
						throw new IOException("Could not delete Datastore "
								+ dsName + " in workspace " + wsName);
				}

				// TODO NOT IMPLEMENETED YET
				// String coveragestoresXml = sendRESTstring(METHOD_GET,
				// "/workspaces/"
				// + wsName + "/coveragestores", null);
				// List<String> coveragestores =
				// parseCoveragestoresXml(coveragestoresXml);
			}

			return 200 == sendRESTint(METHOD_DELETE, "/workspaces/" + wsName,
					null, "application/xml", "application/xml");
		} catch (FileNotFoundException e) {
			// Workspace didn't exist
			return false;
		}
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
	 * @throws IOException
	 */
	private boolean deleteDatastore(String wsName, String dsName,
			boolean recusively) throws IOException {
		if (recusively == true) {
			List<String> layerNames = getLayersUsingDatastore(wsName, dsName);

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

	private boolean deleteLayer(String lName) throws IOException {
		int result = sendRESTint(METHOD_DELETE, "/layers/" + lName, null);
		return result == 200;
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

	final static Pattern layerNamesRegExPattern = Pattern.compile(
			"<layer>.*?<name>(.*?)</name>.*?</layer>", Pattern.DOTALL
					+ Pattern.MULTILINE);

	public List<String> getLayersUsingDatastore(String wsName, String dsName)
			throws IOException {
		final Pattern pattern = Pattern
				.compile("<layer>.*?<name>(.*?)</name>.*?/rest/workspaces/"
						+ wsName + "/datastores/" + dsName
						+ "/featuretypes/.*?</layer>", Pattern.DOTALL
						+ Pattern.MULTILINE);

		List<String> layersUsingDs = new ArrayList<String>();
		for (String lName : getLayerNames()) {
			String xml = sendRESTstring(METHOD_GET, "/layers/" + lName, null);
			// System.out.println(xml);

			Matcher matcher = pattern.matcher(xml);
			if (matcher.find())
				layersUsingDs.add(lName);
		}

		return layersUsingDs;

	}

	/**
	 * Questionalble what is happening here?! Delete the Layers instead!?
	 */
	public boolean deleteFeatureType(String wsName, String dsName, String ftName)
			throws IOException {

		int result = sendRESTint(METHOD_DELETE, "/workspaces/" + wsName
				+ "/datastores/" + dsName + "/featuretypes/" + ftName, null);

		return result == 200;
	}

	static final Pattern featuretypesNameRegEx = Pattern.compile(
			"<featureType>.*?<name>(.*?)</name>.*?</featureType>",
			Pattern.DOTALL + Pattern.MULTILINE);

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

	private List<String> parseXmlWithregEx(String xml, Pattern pattern) {
		ArrayList<String> list = new ArrayList<String>();

		Matcher nameMatcher = pattern.matcher(xml);
		while (nameMatcher.find()) {
			String name = nameMatcher.group(1);
			list.add(name.trim());
		}
		return list;
	}

	final static Pattern datastoreNameRegEx = Pattern.compile(
			"<dataStore>.*?<name>(.*?)</name>.*?</dataStore>", Pattern.DOTALL);

	public String sendRESTstring(String method, String url,
			String xmlPostContent) throws IOException {
		return sendRESTstring(method, url, xmlPostContent, "application/xml",
				"application/xml");
	}

	public int sendRESTint(String method, String url, String xmlPostContent)
			throws IOException {
		return sendRESTint(method, url, xmlPostContent, "application/xml",
				"application/xml");
	}

	public boolean createWorkspace(String workspaceName) throws IOException {
		return 201 == sendRESTint(METHOD_POST, "/workspaces",
				"<workspace><name>" + workspaceName + "</name></workspace>");
	}

	public boolean createDatastorePg(String workspace, String dsName,
			String dsNamespace, String host, String port, String db,
			String user, String pwd, boolean exposePKs) throws IOException {

		String dbType = "postgis";

		return createDbDatastore(workspace, dsName, dsNamespace, host, port,
				db, user, pwd, dbType, exposePKs);
	}

	private boolean createDbDatastore(String workspace, String dsName,
			String dsNamespace, String host, String port, String db,
			String user, String pwd, String dbType, boolean exposePKs)
			throws IOException {

		String exposePKsParamter = "<entry key=\"Expose primary keys\">"
				+ exposePKs + "</entry>";

		/*
		 * <dataStore> <name>nyc</name> <connectionParameters>
		 * <host>localhost</host> <port>5432</port> <database>nyc</database>
		 * <user>bob</user> <dbtype>postgis</dbtype> </connectionParameters>
		 * </dataStore>
		 */
		// <namespace>"+dsNamespace+"</namespace>
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

	public String getDatastore(String wsName, String dsName)
			throws MalformedURLException, ProtocolException, IOException {
		return sendRESTstring(METHOD_GET, "/workspaces/" + wsName
				+ "/datastores/" + dsName, null);
	}

	public boolean createFeatureType(String wsName, String dsName, String ftName)
			throws IOException {
		String xml = "<featureType><name>" + ftName + "</name><title>" + ftName
				+ "</title></featureType>";
		int sendRESTint = sendRESTint(METHOD_POST, "/workspaces/" + wsName
				+ "/datastores/" + dsName + "/featuretypes", xml);
		return 201 == sendRESTint;
	}

	public String getFeatureType(String wsName, String dsName, String ftName)
			throws IOException {
		return sendRESTstring(METHOD_GET, "/workspaces/" + wsName
				+ "/datastores/" + dsName + "/featuretypes/" + ftName, null);
	}

}
