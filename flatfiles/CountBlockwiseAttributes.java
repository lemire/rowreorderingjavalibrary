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

import java.io.*;
import java.util.*;
import inmemory.*;

/**
 * This is an alternative to counting the number of runs.
 * We divide up the tables into blocks of x rows. Over each
 * block, we want to have as few attribute values as possible.
 * 
 * This problem is dominated by the number of attributes in
 * the column with highest cardinality. This makes sorting
 * with the column in reverse cardinality order a very good heuristic
 * when minimizing the *total* number of attributes.
 * 
 * Another related problem is to minimize the volume of the attribute space.
 * For this problem, it looks like sorting with the colums in increasing
 * cardinality is a good heuristic.
 * 
 * The motivation for this problem comes from the following
 * text on Curt Monash's blog:
 * 
 * "Exadata has a Storage Index thats a lot like a Netezza zone map. 
 * I.e., for each megabyte or so of data it stores the min and max value
 *  of every column; if a query predicate rules out those ranges, that megabyte
 *  is never retrieved."
 * 
 * Reference:
 *   http://www.dbms2.com/2010/01/22/oracle-database-hardware-strategy/
 * 
 * @author lemire
 *
 */
public class CountBlockwiseAttributes {
	public static void main(String[] args) throws IOException {
		if(args.length<1) {
			System.out.println("please provide a filename");
			return;
		}
		// the default number of rows per block is 2**15 = 32768
		// which is compatible with Monash's "megabyte of data"
		int blocksize = 32768;
		for(String inputfilename: args) {
			System.out.println(inputfilename);
			FlatBinaryReader fbr = new FlatBinaryReader(inputfilename);
			Iterator<Row> i = fbr.iterator();
			List<Set<Integer>> x = new Vector<Set<Integer>>();
			for(int k = 0; k< fbr.columns; ++k) x.add(new HashSet<Integer>());
			int[] counters = new int[fbr.columns];
			long volumespace = 0;
			int currentblocksize = 0;
			while(i.hasNext()) {
				Row r = i.next();
				for(int k = 0; k<r.size();++k) {
					x.get(k).add(r.get(k));
				}
				++currentblocksize;
				if(currentblocksize == blocksize) {
					long tmpvolume = 1;
					for(int k = 0; k<counters.length;++k) {
						counters[k] += x.get(k).size();				
						tmpvolume *= x.get(k).size();
					}
					volumespace += tmpvolume;
					x = new Vector<Set<Integer>>();
					for(int k = 0; k< fbr.columns; ++k) x.add(new HashSet<Integer>());
					currentblocksize = 0;
				}
			}
			if(currentblocksize > 0) {
				long tmpvolume = 1;
				for(int k = 0; k<counters.length;++k) {
					counters[k] += x.get(k).size();
					tmpvolume *= x.get(k).size();
				}
				volumespace += tmpvolume;
				x = null;
				currentblocksize = 0;
			}
			long totalcount = 0;
			for(int k = 0; k<counters.length;++k) {
				totalcount += counters[k];
				System.out.print(counters[k]+" ");
			}
			System.out.println(totalcount);		
			System.out.println(volumespace);		

		}
	}
}
