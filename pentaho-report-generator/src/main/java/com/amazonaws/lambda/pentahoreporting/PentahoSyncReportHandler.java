package com.amazonaws.lambda.pentahoreporting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.json.simple.parser.ParseException;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;

import com.amazonaws.lambda.pentahoreporting.AbstractReportGenerator.OutputType;
import com.amazonaws.lambda.s3url.S3URLStreamHandlerFactory;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class PentahoSyncReportHandler extends PentahoReportHandlerBase implements RequestStreamHandler {

	public PentahoSyncReportHandler() {
	    ClassicEngineBoot.getInstance().start();
	    URL.setURLStreamHandlerFactory(new S3URLStreamHandlerFactory());
	}
	
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
    	System.out.println("Generating report.");
    	try {
    		String inputString = IOUtils.toString(input, "UTF-8");
    		Map<String, Object> parms = parseParameters(inputString);
    		if (parms.containsKey(PARM_REPORT)) {
    			Properties props = new Properties();
    			URL s3PropsUrl = new URL("s3:" + System.getenv(ENV_S3_BUCKET) + "/" + parms.get(PARM_REPORT) + ".properties");
    			InputStream propStream = s3PropsUrl.openStream();
    			props.load(propStream);
    			propStream.close();

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
    			
    			ReportGenerator reportGenerator = new ReportGenerator(new URL("s3:" + System.getenv(ENV_S3_BUCKET) + "/" + parms.get(PARM_REPORT) + ".prpt"),
    					parms,
    					props.getProperty(PROP_DATA_DRIVER),
    					props.getProperty(PROP_DATA_URL),
    					props.getProperty(PROP_DATA_USER),
    					props.getProperty(PROP_DATA_PASSWORD),
    					props.getProperty(PROP_DATA_QUERY));
    			
    			try {
    				reportGenerator.generateReport(outputType, output);
    			} catch(ResourceException e) {
    				output.write(("{ errorMessage: '" + e.getMessage() + "' }").getBytes());
    			}
    		}
        } catch(ParseException | IllegalArgumentException | ReportProcessingException e) {
        	context.getLogger().log(e.getMessage());
			e.printStackTrace();
		}
    }
}
