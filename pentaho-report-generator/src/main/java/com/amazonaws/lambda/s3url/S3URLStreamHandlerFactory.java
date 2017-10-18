/**
 * 
 */
package com.amazonaws.lambda.s3url;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * @author Jaret
 *
 */
public class S3URLStreamHandlerFactory implements URLStreamHandlerFactory {

	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if ("s3".equals(protocol)) {
			return new S3URLStreamHandler();
		}
		return null;
	}

}
