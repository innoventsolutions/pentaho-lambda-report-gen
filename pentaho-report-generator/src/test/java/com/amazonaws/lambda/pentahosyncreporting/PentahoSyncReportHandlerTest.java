package com.amazonaws.lambda.pentahosyncreporting;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.amazonaws.lambda.pentahoreporting.PentahoSyncReportHandler;
import com.amazonaws.services.lambda.runtime.Client;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class PentahoSyncReportHandlerTest {

    private static final String PDF_SAMPLE_INPUT_STRING = "{\"pathParameters\": {"
    		+ "\"report\": \"customers.prpt\","
    		+ "\"outputType\": \"pdf\""
    		+ "} }";
    private static final String EXCEL_SAMPLE_INPUT_STRING = "{\"pathParameters\": {"
    		+ "\"report\": \"customers.prpt\","
    		+ "\"outputType\": \"excel\""
    		+ "} }";
    private static final String HTML_SAMPLE_INPUT_STRING = "{\"pathParameters\": {"
    		+ "\"report\": \"customers.prpt\","
    		+ "\"outputType\": \"html\""
    		+ "} }";
    
    @Test
    public void testPentahoSyncReportHandlerPDF() throws IOException {
        PentahoSyncReportHandler handler = new PentahoSyncReportHandler();

        InputStream input = new ByteArrayInputStream(PDF_SAMPLE_INPUT_STRING.getBytes());;
        File file = new File(File.separator + "temp" + File.separator + "testReport.pdf");
        file.getParentFile().mkdirs();
        OutputStream output = new BufferedOutputStream(new FileOutputStream(file));

        handler.handleRequest(input, output, null);

        output.close();
    }

    @Test
    public void testPentahoSyncReportHandlerExcel() throws IOException {
        PentahoSyncReportHandler handler = new PentahoSyncReportHandler();

        InputStream input = new ByteArrayInputStream(EXCEL_SAMPLE_INPUT_STRING.getBytes());;
        File file = new File(File.separator + "temp" + File.separator + "testReport.xls");
        file.getParentFile().mkdirs();
        OutputStream output = new BufferedOutputStream(new FileOutputStream(file));

        handler.handleRequest(input, output, null);

        output.close();
    }

    @Test
    public void testPentahoSyncReportHandlerHTML() throws IOException {
        PentahoSyncReportHandler handler = new PentahoSyncReportHandler();

        InputStream input = new ByteArrayInputStream(HTML_SAMPLE_INPUT_STRING.getBytes());;
        File file = new File(File.separator + "temp" + File.separator + "testReport.htm");
        file.getParentFile().mkdirs();
        OutputStream output = new BufferedOutputStream(new FileOutputStream(file));

        handler.handleRequest(input, output, null);

        output.close();
    }
}
