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
import java.io.EOFException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import util.Util;


/**
 * Reorder the rows of a binary flat file, block by block.
 * The blocksize is set to 1000 by default
 * 
 * @author lemire
 *
 */
public class BlockReorderBinary {

	public static void blockReorder(String inputbinaryfilename, String outputbinaryfilename, 
			int blocksize, final AbstractRowReordering rr, int maxnumberofthreads) throws IOException,ExecutionException, InterruptedException {
		FlatBinaryReader fbr = new FlatBinaryReader(inputbinaryfilename);
		FlatBinaryWriter fbw = new FlatBinaryWriter(outputbinaryfilename,fbr.columns);
		//System.out.println(fbr.getNumberOfRows());
		int numberofworkers = Runtime.getRuntime().availableProcessors();
		if(numberofworkers > maxnumberofthreads) numberofworkers = maxnumberofthreads;
		//System.out.println("using "+numberofworkers+" workers");
		final ExecutorService es = Executors.newFixedThreadPool(numberofworkers);
		final Queue<Future<List<Row>>> q = new LinkedList<Future<List<Row>>>();
		while(true) {
			final List<Row> buffer = new Vector<Row>();
			try{
				for(int k = 0; k<blocksize;++k) {
					buffer.add(fbr.read());
				}
			} catch(EOFException oef) {	
			}
			//System.out.println("loaded a buffer");
			if(buffer.size() == 0) break;
			q.add(es.submit(new Callable<List<Row>>() {
				public List<Row> call() {
					return rr.solve(buffer);
				}
			}));

			if(q.size()==numberofworkers) {
				Future<List<Row>> f = q.remove();
				List<Row>  newbuffer = f.get();
				for(Row r : newbuffer)
					fbw.write(r);
			}
			/*
			List<Row>  newbuffer = rr.solve(buffer);
			for(Row r : newbuffer)
				fbw.write(r);*/
		}
		while(q.size()>0) {
			Future<List<Row>> f = q.remove();
			List<Row>  newbuffer = f.get();
			for(Row r : newbuffer)
				fbw.write(r);
		}
		es.shutdownNow();
		fbr.close();
		fbw.close();
	}

	public static void main(String[] args) throws IOException,ExecutionException, InterruptedException{
		if(args.length<2) {
			System.out.println("please provide an input binary file name as well an ouput file name");
			return;
		}
		int counter = 0;
		int maxnumberofthreads = 10;
		while(args[counter].startsWith("-")) {
			if(args[counter].equals("-onethread")) maxnumberofthreads =1;
			else System.err.println("unknown flag: "+args[counter]);
			++counter;
		}
		String inputbinaryfilename = args[counter++];
		String outputbinaryfilename = args[counter++];
		int blocksize = 1000;
		if(args.length>counter) {
			blocksize = Integer.parseInt(args[counter++]);
			if(blocksize <=0) {
				System.out.println("Invalid block size "+blocksize);
				return;
			}
				
		}
		final AbstractRowReordering rr;
		if(args.length<=counter) {
		    rr = new NearestNeighbor();
		} else if (args[counter].equals("aHDO")) {
			rr = new aHDO();
		} else if (args[counter].equals("NearestNeighbor")) {
			rr = new NearestNeighbor();
		} else if (args[counter].equals("Savings")) {
		    rr= new Savings();
		} else if (args[counter].equals("TwoOpt")) {
			rr = new TwoOpt();
		} else if (args[counter].equals("NearestInsertion")) {
			rr = new NearestInsertion();
		} else if (args[counter].equals("NearestNeighbor")) {
			rr = new NearestNeighbor();
		} else if (args[counter].equals("RandomShuffle")) {
			rr = new RandomShuffle();
		} else if (args[counter].equals("FarthestInsertion")) {
			rr = new FarthestInsertion();
		} else if (args[counter].equals("RandomInsertion")) {
			rr = new RandomInsertion();
		}  else if (args[counter].equals("MultipleLists")) {
			rr = new inmemory.MultipleLists(false); 
		} else if (args[counter].equals("IteratedMatching")) {
			rr = new IteratedMatching();
		} else if (args[counter].equals("BruteForce")) {
			rr = new BruteForce();
		} else if (args[counter].equals("BruteForcePeephole")) {
			rr = new BruteForcePeephole();
		} else if (args[counter].startsWith("DCSort")) {
			int bs = Integer.parseInt(args[counter].substring(6));
			boolean owenmode = false;
		    rr = new inmemory.DCSort(bs,owenmode); 
		} else if (args[counter].equals("Lex")) {
		    rr = new Lex(ColumnOrderer.ColOrder.IncreasingCardinality);
     		} else {
			System.out.println("Falling back on NearestNeighbor");
			rr = new NearestNeighbor();
		}
		blockReorder(inputbinaryfilename, outputbinaryfilename,blocksize, rr,maxnumberofthreads);	
	}
}
