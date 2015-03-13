package flatfiles;

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

import gnu.trove.TObjectIntHashMap;
import inmemory.Row;

import java.io.*;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import net.sf.csv4j.CSVReader;

import util.IOUtil;

/*
 * convert a text file in CSV format to the flat binary file format.
 */
public class CSVtoBinary {
	
	public static void main(String[] args) throws IOException {
		if(args.length<2) {
			System.out.println("please give my a csv file name and a binary file name");
			return;
		}
		String csvfilename = args[0];
		String binaryfilename = args[1];
		System.out.println("building normalization table for "+csvfilename+"...");
		final List<TObjectIntHashMap<String>> normalmap = IOUtil.buildNormalMap(args[0]);
		System.out.println("writing out binary file "+binaryfilename+"...");
		FlatBinaryWriter fbr = new FlatBinaryWriter(binaryfilename,normalmap.size());
		//BufferedReader br = new BufferedReader(new FileReader(csvfilename));
		String line;		
		int counter = 0;
	    CSVReader p = new CSVReader(new FileReader(csvfilename));
		List<String> fields;
	    while((fields = p.readLine()).size() != 0){
		//while((line=br.readLine())!= null) {
			   //String[] fields = IOUtil.parseCSVLine(line);
			   int[] values = new int[normalmap.size()];
			   for(int k = 0; k < fields.size(); ++k) {
				   values[k] = normalmap.get(k).get(fields.get(k));
			   }
			   fbr.write(new Row(values));
			   counter +=1;
		}
		p.close();
		fbr.close();
		System.out.println("wrote "+counter+" rows");

	}

}
