/**
 * 
 */
package com.amazonaws.lambda.s3url;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

/**
 * @author Jaret
 *
 */
public class S3URLConnection extends URLConnection {

	protected S3URLConnection(URL url) {
		super(url);
	}

	/* (non-Javadoc)
	 * @see java.net.URLConnection#connect()
	 */
	@Override
	public void connect() throws IOException {
		// Don't need to do anything here
	}

	/* (non-Javadoc)
	 * @see java.net.URLConnection#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		// Get the URL parts and map them into S3 bucket and key components
		AmazonS3 s3Client = AmazonS3Client.builder().build();
		try {
			System.out.println("Establish S3 input stream");
			String bucketName = this.url.getPath().substring(0, this.url.getPath().indexOf("/"));
			String key = this.url.getPath().substring(bucketName.length() + 1);
			S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, key));
			System.out.println("Content-Length: " + Long.toString(s3object.getObjectMetadata().getContentLength()));
			System.out.println("Content-Type: " + s3object.getObjectMetadata().getContentType());
			
			// Read file into buffer to ensure it is properly retrieved
			byte[] file = new byte[(int)s3object.getObjectMetadata().getContentLength()];
			int readCount = 0;
			S3ObjectInputStream is = s3object.getObjectContent();
			int nextByte = is.read();
			while (nextByte > -1) {
				file[readCount++] = (byte) nextByte;
				nextByte = is.read();
			}
			is.close();
			System.out.println("Read " + readCount + " bytes.");
			return new ByteArrayInputStream(file);

		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which" + " means your request made it "
					+ "to Amazon S3, but was rejected with an error response" + " for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means" + " the client encountered "
					+ "an internal error while trying to " + "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
		return null;
	}
}
