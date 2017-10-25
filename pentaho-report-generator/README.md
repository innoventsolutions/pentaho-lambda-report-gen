# Lambda-Pentaho Integration
## Overview
This project allows an AWS Lambda function to execute a Pentaho report in an S3 environment and depositing the report result to a parameterized S3 location.
## Installation Instructions
#### Create the Lambda function
1. From the Lambda console, click the *Create function* button
1. Set the Runtime to Java 8
1. Upload the pentahosyncreporting-*x.x.x*.jar file
1. Set the Handler to com.amazonaws.lambda.pentahoreporting.PentahoAsyncReportHandler
1. Create a new *Environment variable*
   - Name it s3_bucket
   - Set it to the bucket where the report file (prpt) to be executed is stored
1. Set the execution role
   - It will probably need to be custom
   - It will need the *AWSLambdaBasicExecutionRole*
   - It will need the *AmazonS3FullAccess policy*
   - If logging is desired, the *CloudWatchLogsFullAccess policy* will be needed
1. Ensure enough *Memory* is allocated to hold the entire final report, as it must be contained in memory before it can be saved back to the S3 environment
1. Click the *Save* button

#### Create the API Gateway
1. From the *Trigges* tag, click the *Add trigger* button
1. Select the *API Gateway* trigger type
   - Configure as appropriate for your uses
1. Select the function in the *Resources* column, a process flow should appear to the right
1. Open the *Integration Request*
1. Ensure *Use Lambda Proxy integration* is checked
1. Return to *Method Execution* and open the *Method Request* details
1. Add the following parameters, all set to be *Required*
   - report
   - outputType
   - folder
   - file
1. Configuration should be completed at this point

## Usage
The four parameters set up when creating the API Gateway are the bare minimum required to run a report and are interpreted as follows:
* report - This is the name of the report to be executed. It must have a corresponding PRPT file in the s3_bucket *Environment variable*.
  * There may also be a *properties* file with the name of the report, which can be used to define data sources if they are not embedded in the *prpt* file; it should contain the following values
    * dataDriver - the java class that supports the database interface (i.e. com.mysql.jdbc.Driver)
    * dataUrl - the URL of the database to be used (this will be passed directly into the driver)
    * dataUser - the username to be used as part of the credentials
    * dataPassword - the password associated with dataUser
    * One or more entries mapping to named datasources in the report which are set to the SQL query to be used
* outputType - one of three values indicated the desired output for the report
  * pdf
  * html
  * excel
* folder - the S3 bucket where the final report will be created
* file - the name of the file to create the report as, including the extension

In addition to the four core parameters, any parameters required by the report itself should be included in the query string. These parameters will automatically be passed into the report when it is run.