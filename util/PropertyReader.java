package util;

/**
 * 
 * Copyright 2009-2010 Daniel Lemire and Owen Kaser. 
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 * 
 */
import java.util.Properties;
import java.io.*;

/**************************************************************
 * This class has a static method to load a properties file
 * Code recycled from litOLAP
 *
 * @author owen
 */

public class PropertyReader {
    public static Properties read( String pfn) throws IOException {

        // Using the ClassLoader... want something that works
        // for both standalone and servlet...
        // cf http://forum.java.sun.com/thread.jspa?threadID=5175637&messageID=9678754
	Properties props = new Properties();
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(pfn);
        
        if (is == null) {
            String errmsg = pfn + " not found by class loader";
            throw new RuntimeException(errmsg);
        }
	props.load(is);
	return props;
    }
}
	
