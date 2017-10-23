package com.amazonaws.lambda.pentahosyncreporting;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;

import com.amazonaws.lambda.pentahoreporting.AbstractReportGenerator.OutputType;
import com.amazonaws.lambda.s3url.S3URLStreamHandlerFactory;
import com.amazonaws.lambda.pentahoreporting.ReportGenerator;

public class ReportGeneratorTest {

	@Test
	public void testReportGenerator() throws MalformedURLException {
	    ClassicEngineBoot.getInstance().start();
	    URL.setURLStreamHandlerFactory(new S3URLStreamHandlerFactory());

		ReportGenerator reportGenerator = new ReportGenerator(getClass().getClassLoader().getResource("customers.prpt"),
//		ReportGenerator reportGenerator = new ReportGenerator(new URL("s3:foundationforblindchildrentest/customers.prpt"),
				new HashMap<String, Object>(),
				null,
				null,
				null,
				null,
				null);
		
		try {
			File out = new File("c:\\temp\\customers.pdf");
			out.getParentFile().mkdirs();
			reportGenerator.generateReport(OutputType.PDF, out);
		} catch(ResourceException | IllegalArgumentException | IOException | ReportProcessingException e) {
			fail(e.getMessage());
		}
	}

}
