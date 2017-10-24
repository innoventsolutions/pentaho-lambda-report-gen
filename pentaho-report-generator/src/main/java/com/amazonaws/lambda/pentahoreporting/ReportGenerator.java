/**
 * 
 */
package com.amazonaws.lambda.pentahoreporting;

import java.net.URL;
import java.util.HashMap;
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

	private final URL mReport;
	private final Map<String, Object> mParameters;
	private final Map<String, String> mDBQueries = new HashMap<String, String>();
	private final String mDBDriver;
	private final String mDBUrl;
	private final String mDBUser;
	private final String mDBPassword;
	
	public ReportGenerator(URL report, Map<String, Object> parameters, String dbDriver, String dbUrl, String dbUser, String dbPassword) {
		mReport = report;
		mParameters = parameters;
		mDBDriver = dbDriver;
		mDBUrl = dbUrl;
		mDBUser = dbUser;
		mDBPassword = dbPassword;
	}
	
	public void addQuery(String aName, String aQuery) {
		mDBQueries.put(aName, aQuery);
	}
	
	/* (non-Javadoc)
	 * @see com.amazonaws.lambda.pentahosyncreporting.AbstractReportGenerator#getReportDefinition()
	 */
	@Override
	public MasterReport getReportDefinition() throws ResourceException {
		System.out.println("Entering getReportDefinition.");
	    // Parse the report file
	    final ResourceManager resourceManager = new ResourceManager();
	    Resource directly;
//		try {
		System.out.println("Calling createDirectly with " + mReport + ".");
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
			System.out.println("Configuring data factory.");
		    final DriverConnectionProvider sampleDriverConnectionProvider = new DriverConnectionProvider();
		    sampleDriverConnectionProvider.setDriver(mDBDriver);
		    sampleDriverConnectionProvider.setUrl(mDBUrl);
		    sampleDriverConnectionProvider.setProperty("user", mDBUser);
		    sampleDriverConnectionProvider.setProperty("password", mDBPassword);
	
		    final SQLReportDataFactory dataFactory = new SQLReportDataFactory(sampleDriverConnectionProvider);
		    for (String dbName : mDBQueries.keySet()) {
			    dataFactory.setQuery(dbName, mDBQueries.get(dbName));
		    }
		    return dataFactory;
		} else {
			System.out.println("No db information defined.");
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
