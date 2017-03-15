package com.servicebus.messaging.util;


import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import SevenZip.Compression.LZMA.Decoder;
import SevenZip.Compression.LZMA.Encoder;

public class SevenZipUtil {
	
	private static final int DictionarySize = 8388608;
	private static final int NumFastBytes = 128;
	private static final int MatchFinder = 1;
	private static int Lc = 3;
	private static int Lp = 0;
	private static int Pb = 2;
	private static boolean eos = false;;
	
	private Encoder encoder;
	private Decoder decoder;
	
	private String unZipLocation;
	
	public SevenZipUtil() throws ZipCreationException, IOException {
		encoder = new Encoder();
		initializeEncoder();
		decoder = new Decoder();
		
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    	InputStream input = classLoader.getResourceAsStream("location.properties");
    	
    	Properties properties = new Properties();
    	properties.load(input);
    	unZipLocation = properties.getProperty("azure.service.bus.unzip.location.final");
	}
	
	/**
	 * Creates uncompressed 7zip in memory
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public byte[] unCompress7z(byte[] contents) throws IOException {
		InputStream inStream  = new ByteArrayInputStream(contents);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStream outStream = new BufferedOutputStream(bos);
		byte[] data = null;
		try {
			decode(inStream, outStream);
			data = bos.toByteArray();
		} finally {
			closeResources(outStream, inStream);
		}
		return data;
	}
	
	/**
	 * Creates Uncompressed 7zip file
	 * @param fileName
	 * @throws IOException
	 */
	public void unCompress7zFile(String fileName, byte[] contents) throws IOException {
		InputStream inStream  = new ByteArrayInputStream(contents);
		OutputStream outStream = new BufferedOutputStream(
				new FileOutputStream(unZipLocation + File.separator +  fileName));

		try {
			decode(inStream, outStream);
		} finally {
			closeResources(outStream, inStream);
		}
	}
	
	/**
	 * Uncompress data
	 * @param inStream
	 * @param outStream
	 * @throws IOException
	 */
	private void decode(InputStream inStream, OutputStream outStream) throws IOException {
		int propertiesSize = 5;
		byte[] properties = new byte[propertiesSize];
		inStream.read(properties, 0, propertiesSize);
		
		decoder.SetDecoderProperties(properties);
		long outSize = 0;
		for (int i = 0; i < 8; i++)
		{
			int v = inStream.read();
			outSize |= ((long)v) << (8 * i);
		}
		decoder.Code(inStream, outStream, outSize);
	}

	/**
	 * Creates 7zip in memory
	 * @param infileName
	 * @return
	 * @throws IOException
	 */
	public byte[] create7zipBytes(String message) throws IOException {
		byte[] content = null;
		byte[] byteMsg = message.getBytes(AppConstants.UTF_8);
		
		// Read the input file to be compressed 
		InputStream inStream = new ByteArrayInputStream(byteMsg);
		
		// Create output stream 
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStream outStream = new BufferedOutputStream(bos);
		try {
			create7Zip(byteMsg.length, inStream, outStream);
			content = bos.toByteArray();
		} finally {
			// Close Output Streams 
			closeResources(outStream, inStream);
		}
		return content;
	}

	private void create7Zip(long fileLength, InputStream inStream, OutputStream outStream) throws IOException {
		encoder.WriteCoderProperties(outStream);
		
		writeHeader(fileLength, outStream);
		
		// Write Compressed Data to File 
		encoder.Code(inStream, outStream, -1, -1, null);
	}

	/**
	 * Write 7z File Header
	 * @param inputToCompress
	 * @param outStream
	 * @throws IOException
	 */
	private static void writeHeader(long fileLength, OutputStream outStream) throws IOException {
		long fileSize;
		if (eos)
			fileSize = -1;
		else
			fileSize = fileLength;
		
		// Write 7z File Header
		for (int i = 0; i < 8; i++) {
			outStream.write((int) (fileSize >>> (8 * i)) & 0xFF);
		}
	}

	/**
	 * initialize Encoder
	 * @param encoder
	 * @param inStream
	 * @param outStream
	 * @throws ZipCreationException
	 * @throws IOException
	 */
	private void initializeEncoder() throws ZipCreationException, IOException {
		if (!encoder.SetAlgorithm(2)) {
			throw new ZipCreationException("Incorrect compression mode");
		}
		if (!encoder.SetDictionarySize(DictionarySize)) {
			throw new ZipCreationException("Incorrect dictionary size");
		}
		if (!encoder.SetNumFastBytes(NumFastBytes)) {
			throw new ZipCreationException("Incorrect NumFastBytes value");
		}
		if (!encoder.SetMatchFinder(MatchFinder)) {
			throw new ZipCreationException("Incorrect MatchFinder value");
		}
		if (!encoder.SetLcLpPb(Lc, Lp, Pb)) {
			throw new ZipCreationException("Incorrect -lc or -lp or -pb value");
		}
		encoder.SetEndMarkerMode(eos);
	}

	/**
	 * Close all resources
	 * @param outStream
	 * @param inStream
	 * @throws IOException
	 */
	private void closeResources(OutputStream outStream, InputStream inStream) throws IOException {
		outStream.flush();
		outStream.close();
		inStream.close();
	}
}