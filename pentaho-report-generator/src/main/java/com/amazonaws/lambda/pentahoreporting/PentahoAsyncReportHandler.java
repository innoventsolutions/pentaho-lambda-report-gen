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
    	System.out.println("Generating report.");
    	try {
    		String inputString = IOUtils.toString(input, "UTF-8");
    		Map<String, Object> parms = parseParameters(inputString);
    		if (parms.containsKey(PARM_REPORT)) {
    			Properties props = new Properties();
    			URL s3PropsUrl = new URL("s3:" + System.getenv(ENV_S3_BUCKET) + "/" + parms.get(PARM_REPORT) + ".properties");
    			try {
	    			InputStream propStream = s3PropsUrl.openStream();
	    			if (propStream != null) {
		    			props.load(propStream);
		    			propStream.close();
	    			} else {
	    				System.out.println("No properties file found for " + parms.get(PARM_REPORT) + ". Report's default data settings will be used.");
	    			}
    			} catch(AmazonClientException e) {
    				System.out.println("No properties file found for " + parms.get(PARM_REPORT) + ". Report's default data settings will be used.");
    			}
    			
    			OutputType outputType = OutputType.PDF;
    			if (parms.containsKey(PARM_OUTPUT_TYPE)) {
    				if (((String)parms.get(PARM_OUTPUT_TYPE)).toLowerCase().equals(PARM_OUTPUT_TYPE_PDF)) {
    					outputType = OutputType.PDF;
    				} else if (((String)parms.get(PARM_OUTPUT_TYPE)).toLowerCase().equals(PARM_OUTPUT_TYPE_EXCEL)) {
    					outputType = OutputType.EXCEL;
    				} else if (((String)parms.get(PARM_OUTPUT_TYPE)).toLowerCase().equals(PARM_OUTPUT_TYPE_HTML)) {
    					outputType = OutputType.HTML;
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
    			
    			ReportGenerator reportGenerator = new ReportGenerator(new URL("s3:" + System.getenv(ENV_S3_BUCKET) + "/" + parms.get(PARM_REPORT) + ".prpt"),
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
    				reportGenerator.generateReport(outputType, reportByteStream);
    			} catch(ResourceException e) {
    				ByteArrayOutputStream baos = new ByteArrayOutputStream();
    				PrintStream ps = new PrintStream(baos, true, "utf-8");
    				
    				Throwable cause = e;
    				while (cause.getCause() != null) {
    					cause = cause.getCause();
    				}
    				cause.printStackTrace(ps);
    				output.write((String.format(RESPONSE_TEMPLATE, 500, "{"
    						+ "\"errorMessage\": \"" + e.getMessage() + "\", "
    						+ "\"causeMessage\": \"" + cause.getMessage() + "\", "
    						+ "\"causeStackTrace\": \"" + new String(baos.toByteArray(), "utf-8") + "\""
    						+ " }")).getBytes());
    				return;
    			}
    			byte[] reportBytes = reportByteStream.toByteArray();
    			if (!parms.containsKey(PARM_OUTPUT_BUCKET)) {
    				output.write(String.format(RESPONSE_TEMPLATE, 500, "{ \"errorMessage\": \"You must provide a folder parameter\" }").getBytes());
    			} else if (!parms.containsKey(PARM_OUTPUT_KEY)) {
    				output.write(String.format(RESPONSE_TEMPLATE, 500, "{ \"errorMessage\": \"You must provide a file parameter\" }").getBytes());
    			} else {
    				System.out.println("Creating output file on S3, bucket=" + (String)parms.get(PARM_OUTPUT_BUCKET) + "; key=" + (String)parms.get(PARM_OUTPUT_KEY) + ".");
    				putS3Object((String)parms.get(PARM_OUTPUT_BUCKET), (String)parms.get(PARM_OUTPUT_KEY), new ByteArrayInputStream(reportBytes), reportBytes.length);
    				output.write((String.format(RESPONSE_TEMPLATE, 200, "{ "
    						+ "\"message\": \"Report generated\", "
    						+ "\"type\": \"" + parms.get(PARM_OUTPUT_TYPE) + "\", "
    						+ "\"folder\": \"" + parms.get(PARM_OUTPUT_BUCKET) + "\", "
    						+ "\"file\": \"" + parms.get(PARM_OUTPUT_KEY) + "\""
    						+ " }")).getBytes());
    			}
    		} else {
    			StringBuffer out = new StringBuffer();
    			out.append("{"
    					+ "\"errorMessage\": \"You must provide a report parameter\",");
    			out.append("\"inputString\": \"" + inputString + "\",");
    			out.append("\"parameters\": { ");
    			for (String parm : parms.keySet()) {
    				out.append("\"" + parm + "\": \"" + parms.get(parm) + "\",");
    			}
    			out.append("}"
    					+ "}");
    			output.write(String.format(RESPONSE_TEMPLATE, 500, out.toString()).getBytes());
    		}
        } catch(ParseException | IllegalArgumentException | ReportProcessingException e) {
        	context.getLogger().log(e.getMessage());
			e.printStackTrace();
			output.write((String.format(RESPONSE_TEMPLATE, 500, "{ \"errorMessage\": \"" + e.getMessage() + "\" }")).getBytes());
		}
	}

}
