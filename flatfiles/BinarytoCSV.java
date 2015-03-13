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

import inmemory.Row;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import util.IOUtil;


/*
 * Goes back from the binary flat file format to the CSV format. Can also
 * print a binary file to standard output.
 */
public class BinarytoCSV {

	public static void main(String[] args) throws IOException {
		if(args.length<1) {
			System.out.println("please give my a binary (and maybe a csv file name) ");
			return;
		}
		PrintStream bw = System.out;
		if(args.length>1) 
			bw = new PrintStream(args[1]);
		//new BufferedWriter(new FileWriter(args[1]));
		String binaryfilename = args[0];
		FlatBinaryReader fbr = new FlatBinaryReader(binaryfilename);
		try{
			Iterator<Row> i = fbr.iterator();
			while(i.hasNext()) {
				bw.println(i.next());
			}
		} finally {
			try{fbr.close();}
			finally{
				bw.close();
			}
		}
	}
}
