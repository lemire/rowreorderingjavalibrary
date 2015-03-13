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

import inmemory.*;

import java.io.*;
import java.util.*;

/*
 * count the number of column runs in a binary flat file.
 * The -m3 cost model assigns a cost of 1 to a run of length 1,
 * and a cost of 3 to every run of length 2 or more
 * based on <value><samevalue><counter>, ignoring counter range limits
 */
public class CountRunsBinary {
	public static void main(String[] args) throws IOException {
            boolean owensCostModel = false;
            String binaryfilename = null;

		if(args.length<1) {
			System.out.println("please provide a binary file name");
			return;
		}
		Vector<Integer> blocksizes = new Vector<Integer>();
		boolean owencostmodel = false, useHistograms = false;
                int minHistoSize=0;
		int commandcounter = 0;
		boolean countlongruns = false;
		boolean rigid = false;
		boolean verbose = false;
		boolean varyblocksize = false;
		while (args[commandcounter].startsWith("-")) {
			if (args[commandcounter].equals("-m3")) { 
				owencostmodel = true;
			} else if(args[commandcounter].equals("-v")) {
			  verbose = true;
			} else if(args[commandcounter].equals("-countlongruns")) {
			  countlongruns = true;
			}   else if(args[commandcounter].equals("-rigid")) {
				  rigid = true;
			} else if (args[commandcounter].equals("-h")) {
				useHistograms = true;
                        } else if (args[commandcounter].equals("-H")) {
                            useHistograms = true;
                            ++commandcounter;
                            minHistoSize = Integer.parseInt(args[commandcounter]);
			} else if (args[commandcounter].equals("-same")) {
				System.out.println("# -same flag is obselete");
			} else {
				blocksizes.add( Integer.parseInt(args[commandcounter].substring(1)) );
			} 
			//else throw new RuntimeException("args: [-m3] <filename>");
			commandcounter++;
		}
		
		if(blocksizes.size()==0)
			blocksizes.add(1);
		while(commandcounter< args.length) {
			binaryfilename = args[commandcounter];
			if(verbose) System.out.println(binaryfilename);
			System.out.println("# "+binaryfilename);
			for(int blocksize : blocksizes) {
				System.out.print(blocksize+" ");
				if(rigid)
					rigidProcessing(binaryfilename, blocksize, verbose);
				else
					processFile(binaryfilename, blocksize, owencostmodel, useHistograms,  minHistoSize,countlongruns, verbose);
			}
			commandcounter++;
			if(commandcounter< args.length) {
				System.out.println();System.out.println();
			}
		}
	}
	
	/// this uses a rigid, word-aligned approach and counts dirty and clean blocks
	public static void rigidProcessing(String binaryfilename, int blocksize, boolean verbose) throws IOException {
		FlatBinaryReader fbr = new FlatBinaryReader(binaryfilename);
		int[] clean = new int[fbr.columns];
		int[] dirty = new int[fbr.columns];
		Vector<Vector<Integer> > brc = new Vector<Vector<Integer> >();
		for(int k = 0; k <fbr.columns; ++k) {
			brc.add(new Vector());
		}
		int rowcounter = 0;
		try {
			while (true) {
				Row r = fbr.read();
				++rowcounter;
				for(int k = 0; k< r.values.length;++k) {
					brc.get(k).add(r.values[k]);
				}
				if(brc.get(0).size()==blocksize) {
					for(int k = 0; k< r.values.length;++k) {
						int potentialval = brc.get(k).get(0);
						boolean isclean = true;
						for(int j = 1; j<brc.get(k).size();++j) {
							if(brc.get(k).get(j)!=potentialval) { isclean=false; break;}
						}
						if(isclean) clean[k]++; else dirty[k]++;
						brc.get(k).clear();
					}
				}
			}
		} catch (EOFException oef) {
			fbr.close();
		}
		if(verbose) System.out.println("read "+rowcounter+"rows");
		if(brc.get(0).size()>0) {
			for(int k = 0; k< fbr.columns;++k) {
				 dirty[k]++;//automatically dirty
			}
		}
		int sum = 0;
		for(int k : clean) {
			sum += k;
			System.out.print(k+" ");
		}
		System.out.println(sum);
		sum = 0;
		for(int k : dirty) {
			sum += k;
			System.out.print(k+" ");
		}
		System.out.println(sum);
	}
	
	public static void processFile(String binaryfilename, int blocksize, 
                                       boolean owencostmodel, boolean useHistograms, 
                                       int minHistoSize, boolean countlongruns, boolean verbose ) throws IOException {
		Vector<BlockRunCounter> brc = countRuns(binaryfilename, blocksize,owencostmodel,useHistograms | countlongruns ,verbose);
		long count = 0;
		for (BlockRunCounter b : brc) {
			b.close();
			System.out.print(b.NumberOfRuns+ " ");
			count += b.NumberOfRuns;
		}
		System.out.print(count);		
		System.out.println();
		if(countlongruns) {
			count = 0;
			for (BlockRunCounter b : brc) {
				b.close();
				int x = b.getHistogram().sizeOfFrequentIceberg(blocksize);
				System.out.print(x+ " ");
				count += x;
			}
			System.out.print(count);		
			System.out.println();
		}

		if (useHistograms)
			for (BlockRunCounter b : brc) 
				if (minHistoSize == 0)
					b.getHistogram().dump();
				else
					b.getHistogram().dump(minHistoSize);

	}
	
    public static Vector<BlockRunCounter> countRuns(String binaryfilename, int blocksize, boolean owencostmodel, 
    		boolean useHistograms, boolean verbose) throws IOException {
		FlatBinaryReader fbr = new FlatBinaryReader(binaryfilename);
		Vector<BlockRunCounter> brc = new Vector<BlockRunCounter>();
		for(int k = 0; k <fbr.columns; ++k) {
			BlockRunCounter newbrc = new BlockRunCounter(blocksize,useHistograms,owencostmodel);
			brc.add(newbrc);
		}
		int rowcounter = 0;
		try {
			while (true) {
				Row r = fbr.read();
				++rowcounter;
				for(int k = 0; k< r.values.length;++k) {
					brc.get(k).newValue(r.values[k]);
				}
			}
		} catch (EOFException oef) {
			fbr.close();
		}
		if(verbose) System.out.println("read "+rowcounter+"rows");
		return brc;
	}
}


