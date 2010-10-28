package org.geopublishing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class RestUtil {

	public static String entryKey(String key, Boolean value) {
		return "<entry key=\"" + key + "\">" + (value == null ? true : value)
				+ "</entry>";
	}

	/**
	 * Reads the contents of a File into one String. Watch the size!
	 * 
	 * @param file
	 *            a File to read
	 * @return as String the content of the file.
	 * 
	 * @throws java.io.IOException
	 */
	public static String readFileAsString(File file) throws java.io.IOException {
		byte[] buffer = new byte[(int) file.length()];
		FileInputStream f = new FileInputStream(file);
		try {
			f.read(buffer);
			return new String(buffer);
		} finally {
			f.close();
		}
	}

	/**
	 * @return a {@link String} with the content of the {@link URL}. Do not use
	 *         this on long files! Returns <code>null</code> if an error occured
	 *         or the file doesn't exists.<br/>
	 *         A newline-character is added at every new line.
	 * @throws IOException
	 */
	public static String readURLasString(URL url) throws IOException {

		InputStream openStream = url.openStream();
		try {

			InputStreamReader inStream = new InputStreamReader(openStream);
			try {

				BufferedReader inReader = new BufferedReader(inStream);
				try {
					String oneLine = inReader.readLine();
					if (oneLine == null)
						return "";
					StringBuffer content = new StringBuffer();
					while (oneLine != null) {
						content.append(oneLine);
						oneLine = inReader.readLine();
						if (oneLine != null)
							content.append("\n");
					}
					return content.toString();
				} finally {
					inReader.close();
				}

			} finally {
				inStream.close();
			}
		} finally {
			openStream.close();
		}
	}
}
