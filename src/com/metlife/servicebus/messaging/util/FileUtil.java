package com.metlife.servicebus.messaging.util;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 * @author rprasad017
 *
 */
public class FileUtil {
	
	private static String UTF_8 = "UTF-8";

	/**
	 * Reads file names from a folder
	 * @param srcFolder
	 * @return list of file names in a folder
	 * @throws Exception
	 */
	public static List<String> readFileNames(String srcFolder) {
		List<String> files = new ArrayList<String>(0);
		
		final File folder = new File(srcFolder);
		for (final File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory()) {
	        	files.add(fileEntry.getAbsolutePath());
	        }
	    }
		return files;
	}
	
	/**
	 * Read file contents in UTF-8 encoding
	 * @param absoluteFileName
	 * @return contents of file
	 * @throws IOException 
	 */
	public static String readFile(String absoluteFileName) throws IOException {
		File fileDir = new File(absoluteFileName);
		InputStreamReader is = new InputStreamReader(new FileInputStream(fileDir), UTF_8);
		
		byte[] contents = IOUtils.toByteArray(is, UTF_8);
		is.close();
		return new String(contents, UTF_8);
	}
	
	/**
	 * Deletes existing file
	 * @param absoluteFileName
	 * @return true if file is deleted
	 */
	public static boolean deleteFile(String absoluteFileName) {
		return (new File(absoluteFileName)).delete();
	}
	
	/**
	 * Write file in UTF-8 encoding
	 * @param text
	 * @param absoluteFileName
	 * @throws IOException 
	 */
	public static void writeToFile(String text, String absoluteFileName) throws IOException {
		Writer unicodeFileWriter = new OutputStreamWriter(
				    new FileOutputStream(absoluteFileName), UTF_8);
		unicodeFileWriter.write(text);
		unicodeFileWriter.close();
	}

	/**
	 * Get file name from absolute path containing file name
	 * @param absoluteFileName
	 * @return
	 */
	public static String getFileName(String absoluteFileName) {
		return (new File(absoluteFileName)).getName();
	}
	
	/**
	 * Check if file already exists
	 * @param absoluteFileName
	 * @return true if file exists, False otherwise
	 */
	public static boolean isFileExists(String absoluteFileName) {
		File file = new File(absoluteFileName);
		return file.exists();
	}
}
