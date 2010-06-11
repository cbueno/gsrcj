package cpa.keck.gsrest;

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

	public enum HttpMethod {
		POST, GET, PUT, DELETE;
	}

	private final String password;
	private final String gsBaseUrl;
	private final String username;
	private String restUrl;

	/**
	 * @param gsBaseUrl
	 *            The base URL of geoserver. 
	 * @param username
	 * @param password
	 */
	public GsRest(String gsBaseUrl, String username, String password) {
		
		if (!gsBaseUrl.endsWith("/")) gsBaseUrl += "/"; 
		this.gsBaseUrl = gsBaseUrl;
		
		this.restUrl = gsBaseUrl + "rest";
		this.username = username;
		this.password = password;
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
	public int sendRESTint(HttpMethod method, String urlEncoded,
			String postData, String contentType, String accept)
			throws IOException {
		HttpURLConnection connection = sendREST(method, urlEncoded, postData,
				contentType, accept);

		return connection.getResponseCode();
	}

	public String sendRESTstring(HttpMethod method, String urlEncoded,
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

	private HttpURLConnection sendREST(HttpMethod method, String urlEncoded,
			String postData, String contentType, String accept)
			throws MalformedURLException, IOException, ProtocolException {
		boolean doOut = method != HttpMethod.DELETE && postData != null;
		// boolean doIn = true; // !doOut

		String link = gsBaseUrl + "rest" + urlEncoded;
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
		String userPasswordEncoded = new BASE64Encoder()
				.encode((username + ":" + password).getBytes());

		connection.setRequestMethod(method.toString());
		// type XML
		connection.setRequestProperty("Content-Type", contentType);
		// Basic Auth
		connection.setRequestProperty("Authorization", "Basic "
				+ userPasswordEncoded);

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
		return 201 != sendRESTint(HttpMethod.DELETE, "/workspaces",
				"<workspace><name>" + workspaceName + "</name></workspace>",
				"application/xml", "application/xml");

	}

	public boolean createWorkspace(String workspaceName) throws IOException {
		return 201 == sendRESTint(HttpMethod.POST, "/workspaces",
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
				+ "</passwd><dbtype>" + dbType
				+ "</dbtype></connectionParameters></dataStore>";

		int returnCode = sendRESTint(HttpMethod.POST, "/workspaces/"
				+ workspace + "/datastores", xml, "application/xml",
				"application/xml");
		return 201 == returnCode;
	}

	public String getDatastore(String wsName, String dsName)
			throws MalformedURLException, ProtocolException, IOException {
		return sendRESTstring(HttpMethod.GET, "/workspaces/" + wsName
				+ "/datastores/" + dsName, null, "application/xml",
				"application/xml");
	}

	public boolean createFeatureType(String wsName, String dsName, String ftName)
			throws IOException {
		String xml = "<featureType><name>" + ftName + "</name><title>" + ftName
				+ "</title></featureType>";
		int sendRESTint = sendRESTint(HttpMethod.POST, "/workspaces/" + wsName
				+ "/datastores/" + dsName + "/featuretypes", xml,
				"application/xml", "application/xml");
		return 201 == sendRESTint;
	}

	public String getFeatureType(String wsname, String dsname, String ftName)
			throws IOException {
		return sendRESTstring(HttpMethod.GET, "/workspaces/" + wsname
				+ "/datastores/" + dsname + "/featuretypes/" + ftName, null,
				"application/xml", "application/xml");
	}

}
