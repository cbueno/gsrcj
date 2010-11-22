package org.geopublishing;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Retrieves user, password and URL for a Testing-Geoserver from an optional
 * properties file in the users home directory. If the file doesn't exist,
 * returns <code>false</code> which should end the test without failure.
 */
public class GsTestingUtil {

	private static String baseUrl;
	private static boolean initialized;
	private static final String KEY_PASSWORD = "password";

	private static final String KEY_URL = "url";

	private static final String KEY_USERNAME = "user";
	private static final String propertiesLocation = System
			.getProperty("user.home") + "/gstesting.properties";

	private static String password;

	private static String username;

	public static String getPassword() {
		readProps();

		return password;
	}

	public static String getUsername() {
		readProps();

		return username;
	}

	private static String gsBaseUrl() {
		readProps();

		return baseUrl;
	}

	/**
	 * If <code>true</code> a testing GS is available.
	 */
	public static boolean isAvailable() {

		readProps();

		try {
			List<String> layerNames = new GsRest(gsBaseUrl(), getUsername(),
					getPassword()).getLayerNames();
			return true;
		} catch (IOException e) {
			System.out.println("Geoserver is offline, or create a "
					+ propertiesLocation + " file with keys " + KEY_USERNAME
					+ ", " + KEY_PASSWORD + ", and " + KEY_URL
					+ " to run JUnit tests against a Geoserver: "
					+ e.getMessage());
			return false;
		}
	}

	private static void readProps() {
		if (!initialized) {

			Properties properties = new Properties();
			File pf = new File(propertiesLocation);
			try {
				properties.load(new FileReader(pf));
				username = properties.getProperty(KEY_USERNAME);
				password = properties.getProperty(KEY_PASSWORD);
				baseUrl = properties.getProperty(KEY_URL);

				initialized = true;
			} catch (Exception e) {
				System.out.println("Geoserver is offline, or create a "
						+ propertiesLocation + " file with keys "
						+ KEY_USERNAME + ", " + KEY_PASSWORD + ", and "
						+ KEY_URL + " to run JUnit tests against a Geoserver: "
						+ e.getMessage());
			}
		}
	}

	public static String getUrl() {
		readProps();

		return baseUrl;
	}
}
