/**
 * 
 */
package com.amazonaws.lambda.pentahoreporting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.lambda.pentahoreporting.AbstractReportGenerator.OutputType;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

/**
 * @author Jaret
 *
 */
public class PingTest implements RequestStreamHandler {

	/* (non-Javadoc)
	 * @see com.amazonaws.services.lambda.runtime.RequestStreamHandler#handleRequest(java.io.InputStream, java.io.OutputStream, com.amazonaws.services.lambda.runtime.Context)
	 */
	@Override
	public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
    	System.out.println("Initiating Ping Test.");
    	String out = "{ testResult: 'completed',"
    			+ "code: 200,"
    			+ "result: 'OK'"
    			+ "}";
    	output.write(out.getBytes());
//    	try {
//    		Map<String, Object> parms = parseParameters(input);
//    		
//        } catch(ParseException | IllegalArgumentException e) {
//        	context.getLogger().log(e.getMessage());
//			e.printStackTrace();
//		}
    }
    
    private InputStream getS3Object(String bucketName, String key) {
		AmazonS3 s3Client = AmazonS3Client.builder().build();
		try {
			System.out.println("Downloading an object");
			S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, key));
			System.out.println("Content-Type: " + s3object.getObjectMetadata().getContentType());
			return s3object.getObjectContent();

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

    private Map<String, Object> parseParameters(InputStream input) throws IOException, ParseException {
    	Map<String, Object> out = new HashMap<String, Object>();

    	JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        JSONObject event = (JSONObject)parser.parse(reader);
        if (event.get("queryStringParameters") != null) {
            JSONObject qps = (JSONObject)event.get("queryStringParameters");
            for (Object key : qps.keySet()) {
            	out.put((String) key, (String)qps.get(key));
            }
        }

        if (event.get("pathParameters") != null) {
            JSONObject pps = (JSONObject)event.get("pathParameters");
            for (Object key : pps.keySet()) {
            	out.put((String) key, (String)pps.get(key));
            }
        }

    	return out;
    }

}
