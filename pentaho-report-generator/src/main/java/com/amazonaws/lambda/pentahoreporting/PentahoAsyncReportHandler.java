/**
 * 
 */
package com.amazonaws.lambda.pentahoreporting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.json.simple.parser.ParseException;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.lambda.pentahoreporting.AbstractReportGenerator.OutputType;
import com.amazonaws.lambda.s3url.S3URLStreamHandlerFactory;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

/**
 * @author Jaret
 *
 */
public class PentahoAsyncReportHandler extends PentahoReportHandlerBase implements RequestStreamHandler {
	
	private static final String PARM_TYPE_PREFIX = "parm";
	private static final String DATA_PREFIX = "data"; 
	
	private static final String PARM_TYPE_INTEGER = "integer";
	private static final String PARM_TYPE_DOUBLE = "double";
	private static final String PARM_TYPE_STRING = "string";
	private static final String PARM_TYPE_NUMBER = "number";

	public PentahoAsyncReportHandler() {
	    ClassicEngineBoot.getInstance().start();
	    URL.setURLStreamHandlerFactory(new S3URLStreamHandlerFactory());
	}

	/* (non-Javadoc)
	 * @see com.amazonaws.services.lambda.runtime.RequestStreamHandler#handleRequest(java.io.InputStream, java.io.OutputStream, com.amazonaws.services.lambda.runtime.Context)
	 */
	@Override
	public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
    	String corsHeaders = "";
    	System.out.println("Generating report.");
    	try {
    		String inputString = IOUtils.toString(input, "UTF-8");
    		Map<String, Object> parms = parseParameters(inputString);

// Set up the parameters from potential sources.  Start with json structure values, fall back to parameters, fall back to env settings
//        	String prptFile = System.getenv(PARM_REPORT);
//        	String prptS3Bucket = System.getenv(PARM_S3_BUCKET);
//        	String outputFolder = System.getenv(PARM_OUTPUT_BUCKET);
//        	String outputFile = System.getenv(PARM_OUTPUT_KEY);
//        	String outputType = System.getenv(PARM_OUTPUT_TYPE);

        	String prptFile = (String)parms.get(PARM_REPORT);
        	String prptS3Bucket = (String)parms.get(PARM_S3_BUCKET);
        	String outputFolder = (String)parms.get(PARM_OUTPUT_BUCKET);
        	String outputFile = (String)parms.get(PARM_OUTPUT_KEY);
        	String outputType = (String)parms.get(PARM_OUTPUT_TYPE);

        	if (prptFile == null) prptFile = System.getenv(PARM_REPORT);
        	if (prptS3Bucket == null) prptS3Bucket = System.getenv(PARM_S3_BUCKET);
        	if (outputFolder == null) outputFolder = System.getenv(PARM_OUTPUT_BUCKET);
        	if (outputFile == null) outputFile = System.getenv(PARM_OUTPUT_KEY);
        	if (outputType == null) outputType = System.getenv(PARM_OUTPUT_TYPE);
        	final boolean useCors = "true".equalsIgnoreCase(System.getenv(VAR_USE_CORS));
        	if(useCors) {
            	String corsDomain = System.getenv(VAR_CORS_DOMAIN);
            	if(corsDomain == null) {
            		corsDomain = "*";
            	}
            	corsHeaders = String.format(",\n" 
    				    + "        \"Access-Control-Allow-Headers\": \"Content-Type\",\n" 
    				    + "        \"Access-Control-Allow-Origin\": \"%s\"", corsDomain);
        	}
        	if (prptFile != null) {
    			Properties props = new Properties();
    			URL s3PropsUrl = new URL("s3:" + prptS3Bucket + "/" + prptFile + ".properties");
    			try {
	    			InputStream propStream = s3PropsUrl.openStream();
	    			if (propStream != null) {
		    			props.load(propStream);
		    			propStream.close();
	    			} else {
	    				System.out.println("No properties file found for " + prptFile + ". Report's default data settings will be used.");
	    			}
    			} catch(AmazonClientException e) {
    				System.out.println("No properties file found for " + prptFile + ". Report's default data settings will be used.");
    			}
    			
    			OutputType outputTypeValue = OutputType.PDF;
    			String outputExtension = "pdf";
    			if (outputType != null) {
    				if (outputType.equalsIgnoreCase(PARM_OUTPUT_TYPE_PDF)) {
    					outputTypeValue = OutputType.PDF;
    				} else if (outputType.equalsIgnoreCase(PARM_OUTPUT_TYPE_EXCEL)) {
    					outputTypeValue = OutputType.EXCEL;
    					outputExtension = "xls";
    				} else if (outputType.equalsIgnoreCase(PARM_OUTPUT_TYPE_HTML)) {
    					outputTypeValue = OutputType.HTML;
    					outputExtension = "html";
    				}
    			}
    			
    			for (String parm : parms.keySet()) {
    				if (props.containsKey(PARM_TYPE_PREFIX + parm)) {
    					String prop = props.getProperty(PARM_TYPE_PREFIX + parm);
    					if (PARM_TYPE_INTEGER.equalsIgnoreCase(prop)) {
    						parms.put(parm, new Integer((String)parms.get(parm)));
    					} else if (PARM_TYPE_DOUBLE.equalsIgnoreCase(prop)) {
    						parms.put(parm,  new Double((String)parms.get(parm)));
    					} else if (PARM_TYPE_NUMBER.equalsIgnoreCase(prop)) {
    						parms.put(parm, new Float((String)parms.get(parm)));
    					}
    				}
    			}
    			
    			ReportGenerator reportGenerator = new ReportGenerator(new URL("s3:" + prptS3Bucket + "/" + prptFile + ".prpt"),
    					parms,
    					props.getProperty(PROP_DATA_DRIVER),
    					props.getProperty(PROP_DATA_URL),
    					props.getProperty(PROP_DATA_USER),
    					props.getProperty(PROP_DATA_PASSWORD));
    			for (Object key : props.keySet()) {
    				if (!(((String)key).startsWith(DATA_PREFIX) | ((String)key).startsWith(PARM_TYPE_PREFIX))) {
    					reportGenerator.addQuery((String)key, props.getProperty((String)key));
    				}
    			}
    			ByteArrayOutputStream reportByteStream = new ByteArrayOutputStream();
    			try {
    				reportGenerator.generateReport(outputTypeValue, reportByteStream);
    			} catch(ResourceException e) {
    				ByteArrayOutputStream baos = new ByteArrayOutputStream();
    				PrintStream ps = new PrintStream(baos, true, "utf-8");
    				
    				Throwable cause = e;
    				while (cause.getCause() != null) {
    					cause = cause.getCause();
    				}
    				cause.printStackTrace(ps);
    				output.write((getResponse(500, "{"
    						+ "\\\"errorMessage\\\": \\\"" + e.getMessage() + "\\\", "
    						+ "\\\"causeMessage\\\": \\\"" + cause.getMessage() + "\\\", "
    						+ "\\\"causeStackTrace\\\": \\\"" + new String(baos.toByteArray(), "utf-8") + "\\\""
    						+ " }", corsHeaders)).getBytes());
    				return;
    			}
    			byte[] reportBytes = reportByteStream.toByteArray();
    			if (outputFolder == null) {
    				output.write(getResponse(500, "{ \\\"errorMessage\\\": \\\"You must provide a folder parameter\\\" }", corsHeaders).getBytes());
    			} else if (outputFile == null) {
    				output.write(getResponse(500, "{ \\\"errorMessage\\\": \\\"You must provide a file parameter\\\" }", corsHeaders).getBytes());
    			} else {
    				System.out.println("Creating output file on S3, bucket=" + outputFolder + "; key=" + outputFile + ".");
    				putS3Object(outputFolder, outputFile + "." + outputExtension, new ByteArrayInputStream(reportBytes), reportBytes.length);
    				output.write((getResponse(200, "{ "
    						+ "\\\"message\\\": \\\"Report generated\\\", "
    						+ "\\\"output_type\\\": \\\"" + outputType + "\\\", "
    						+ "\\\"output_s3_bucket\\\": \\\"" + outputFolder + "\\\", "
    						+ "\\\"output_file\\\": \\\"" + outputFile + "\\\""
    						+ " }", corsHeaders)).getBytes());
    			}
    		} else {
    			StringBuffer out = new StringBuffer();
    			out.append("{"
    					+ "\\\"errorMessage\\\": \\\"You must provide a report parameter\\\",");
    			out.append("\\\"inputString\\\": \\\"" + inputString + "\\\",");
    			out.append("\\\"parameters\\\": { ");
    			for (String parm : parms.keySet()) {
    				out.append("\\\"" + parm + "\\\": \\\"" + parms.get(parm) + "\\\",");
    			}
    			out.append("}"
    					+ "}");
    			output.write(getResponse(500, out.toString(), corsHeaders).getBytes());
    		}
        } catch(ParseException | IllegalArgumentException | ReportProcessingException e) {
        	context.getLogger().log(e.getMessage());
			e.printStackTrace();
			final String response = getResponse(500, 
				"{ \\\"errorMessage\\\": \\\"" + e.getMessage() + "\\\" }", corsHeaders); 
			output.write(response.getBytes());
		}
	}

}
