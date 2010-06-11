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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

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

	private HttpURLConnection sendREST(String method, String urlEncoded,
			String postData, String contentType, String accept)
			throws MalformedURLException, IOException, ProtocolException {
		boolean doOut = METHOD_DELETE.equals(method) && postData != null;
		// boolean doIn = true; // !doOut

		String link = restUrl + urlEncoded;
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
			// Basic Auth
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

	public boolean deleteWorkspace(String workspaceName) throws IOException {
		return 201 != sendRESTint(METHOD_DELETE, "/workspaces",
				"<workspace><name>" + workspaceName + "</name></workspace>",
				"application/xml", "application/xml");

	}

	public boolean createWorkspace(String workspaceName) throws IOException {
		return 201 == sendRESTint(METHOD_POST, "/workspaces",
				"<workspace><name>" + workspaceName + "</name></workspace>",
				"application/xml", "application/xml");
	}

	public boolean createDatastorePg(String workspace, String dsName,
			String dsNamespace, String host, String port, String db,
			String user, String pwd) throws IOException {

		String dbType = "postgis";

		return createDbDatastore(workspace, dsName, dsNamespace, host, port,
				db, user, pwd, dbType);
	}

	private boolean createDbDatastore(String workspace, String dsName,
			String dsNamespace, String host, String port, String db,
			String user, String pwd, String dbType) throws IOException {

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
				+ dsNamespace
				+ "</namespace></connectionParameters></dataStore>";

		int returnCode = sendRESTint(METHOD_POST, "/workspaces/" + workspace
				+ "/datastores", xml, "application/xml", "application/xml");
		return 201 == returnCode;
	}

	public String getDatastore(String wsName, String dsName)
			throws MalformedURLException, ProtocolException, IOException {
		return sendRESTstring(METHOD_GET, "/workspaces/" + wsName
				+ "/datastores/" + dsName, null, "application/xml",
				"application/xml");
	}

	public boolean createFeatureType(String wsName, String dsName, String ftName)
			throws IOException {
		String xml = "<featureType><name>" + ftName + "</name><title>" + ftName
				+ "</title></featureType>";
		int sendRESTint = sendRESTint(METHOD_POST, "/workspaces/" + wsName
				+ "/datastores/" + dsName + "/featuretypes", xml,
				"application/xml", "application/xml");
		return 201 == sendRESTint;
	}

	public String getFeatureType(String wsName, String dsName, String ftName)
			throws IOException {
		return sendRESTstring(METHOD_GET, "/workspaces/" + wsName
				+ "/datastores/" + dsName + "/featuretypes/" + ftName, null,
				"application/xml", "application/xml");
	}

}
