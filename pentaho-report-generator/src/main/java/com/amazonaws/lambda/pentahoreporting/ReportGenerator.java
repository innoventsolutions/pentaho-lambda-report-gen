/**
 * 
 */
package com.amazonaws.lambda.pentahoreporting;

import java.net.URL;
import java.util.Map;

import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.DriverConnectionProvider;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.SQLReportDataFactory;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

/**
 * @author Jaret
 *
 */
public class ReportGenerator extends AbstractReportGenerator {
	private static final String QUERY_NAME = "report_query";

	private final URL mReport;
	private final Map<String, Object> mParameters;
	private final String mDBDriver;
	private final String mDBUrl;
	private final String mDBUser;
	private final String mDBPassword;
	private final String mDBQuery;
	
	public ReportGenerator(URL report, Map<String, Object> parameters, String dbDriver, String dbUrl, String dbUser, String dbPassword, String dbQuery) {
		mReport = report;
		mParameters = parameters;
		mDBDriver = dbDriver;
		mDBUrl = dbUrl;
		mDBUser = dbUser;
		mDBPassword = dbPassword;
		mDBQuery = dbQuery;
	}
	
	/* (non-Javadoc)
	 * @see com.amazonaws.lambda.pentahosyncreporting.AbstractReportGenerator#getReportDefinition()
	 */
	@Override
	public MasterReport getReportDefinition() throws ResourceException {
	    // Parse the report file
	    final ResourceManager resourceManager = new ResourceManager();
	    Resource directly;
//		try {
			directly = resourceManager.createDirectly(mReport, MasterReport.class);
		    return (MasterReport) directly.getResource();
//		} catch (ResourceException e) {
//			System.out.println("Encountered error locating the report definition.");
//			System.out.println("Error Message:    " + e.getMessage());
//		}
//		return null;
	}

	/* (non-Javadoc)
	 * @see com.amazonaws.lambda.pentahosyncreporting.AbstractReportGenerator#getDataFactory()
	 */
	@Override
	public DataFactory getDataFactory() {
		if (mDBUrl != null) {
		    final DriverConnectionProvider sampleDriverConnectionProvider = new DriverConnectionProvider();
		    sampleDriverConnectionProvider.setDriver(mDBDriver);
		    sampleDriverConnectionProvider.setUrl(mDBUrl);
		    sampleDriverConnectionProvider.setProperty("user", mDBUser);
		    sampleDriverConnectionProvider.setProperty("password", mDBPassword);
	
		    final SQLReportDataFactory dataFactory = new SQLReportDataFactory(sampleDriverConnectionProvider);
		    dataFactory.setQuery(QUERY_NAME, mDBQuery);
	
		    return dataFactory;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.amazonaws.lambda.pentahosyncreporting.AbstractReportGenerator#getReportParameters()
	 */
	@Override
	public Map<String, Object> getReportParameters() {
		return mParameters;
	}

}
