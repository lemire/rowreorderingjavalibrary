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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Random;

import util.MersenneTwisterFast;

public class Shuffle {

	// This is the classical Knuth algorithm...
	//
	// we can do much faster, see the update on my blog...
	// http://www.daniel-lemire.com/blog/archives/2010/03/15/external-memory-shuffling-in-linear-time/
	public static void main(String[] args) throws IOException {
		if(args.length<2) {
			System.out.println("please give my a binary input file and a binary output file");
			return;
		}
		MersenneTwisterFast rand = new MersenneTwisterFast();
		//Random rand = new Random();
		FlatBinaryReader fbr = new FlatBinaryReader(args[0]);
		FlatBinaryWriter fbw = new FlatBinaryWriter(args[1],fbr.columns);
		int rowcounter = 0;
		try{
			Iterator<Row> i = fbr.iterator();
			if(i.hasNext()) {
				fbw.write(i.next());
				++rowcounter;
			}
			while(i.hasNext()) {
				Row r = i.next();
				long j = rand.nextLong( fbw.getNumberOfRows() ); 
				if(j<0) j*=-1;
				Row rnew = fbw.getRow(j);
				fbw.putRow(r,j);
				fbw.write(rnew);
				++rowcounter;
				//if(rowcounter+" "+fbw.getNumberOfRows());
			}
		} finally {
			try{fbr.close();}
			finally{
				fbw.close();
			}
		}
		System.out.println("wrote "+rowcounter+" rows");
	}
}
