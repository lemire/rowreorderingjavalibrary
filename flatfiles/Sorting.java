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

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntProcedure;
import gnu.trove.TObjectIntHashMap;
import inmemory.AttributeValue;
import inmemory.BlockRunCounter;
import inmemory.FreqWeigthedAttributeValue;
import util.MutableInteger;
import inmemory.Row;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import util.Pair;
import util.Util;
/*
 * Using the Unix sort command is nice, but not very flexible.
 * There are many external-memory sorting programs written in Java,
 * but I could not find one that was satisfying, so I wrote my own.
 * No doubt, it will have flaws.
 * 
 * -Daniel Lemire, January 15th 2010
 */

public class Sorting {

	public static final int verbose = 0;

	public static final int MAXNUMBEROFTEMPFILES=256;// we don't want more than 256 temporary files.
	public static final int DEFAULTBLOCKSIZE=32 * 1024 * 1024; //16MB
	/**
	 * This will simply load the file by blocks of x rows, then
	 * sort them in-memory, and write the result to a bunch of 
	 * temporary files that have to be merged later.
	 * 
	 * @param file some flat binary file
	 * @return a list of temporary flat binary files
	 */
	public static List<File> sortInBatch(File file, Comparator<Row> cmp) throws IOException {
		List<File> files = new Vector<File>();
		if(verbose>1) System.out.println("opening "+file.getAbsolutePath());
		FlatBinaryReader fbr = new FlatBinaryReader(file.getAbsolutePath());
		long totalrowread = 0;
		try{
			int BlockSizeInBytes = DEFAULTBLOCKSIZE;
			//final long reportedsize = file.length();
			final int memoryusageperrow = fbr.computePhysicalSizeOfRowsInBytes() * 2; // the 2 is an heuristic
			long estimatednumberofrows = fbr.getNumberOfRows();//  reportedsize / bytesperrow;
			if(verbose>1) System.out.println("expect "+estimatednumberofrows +" rows");
			if(verbose>1) System.out.println("might use "+(int) Math.ceil(estimatednumberofrows * 1.0 *  memoryusageperrow / BlockSizeInBytes)+" tmp files");
			if(estimatednumberofrows *  memoryusageperrow / BlockSizeInBytes > MAXNUMBEROFTEMPFILES - 1) {
				BlockSizeInBytes = (int) Math.round(1.0*estimatednumberofrows *  memoryusageperrow / (MAXNUMBEROFTEMPFILES-1));
				if(verbose>1) System.out.println("need about  "+Math.ceil(1.0*estimatednumberofrows*  memoryusageperrow/BlockSizeInBytes)+" blocks");
			}
			int BlockSize = BlockSizeInBytes/  memoryusageperrow;
			if(verbose>1) System.out.println("using blocks of "+BlockSize+" rows");
			List<Row> tmplist =  new Vector<Row>();
			try {
				while(true) {
					if(verbose>1) System.out.println("loading up to "+BlockSize+" rows");
					tmplist = new Vector<Row>();
					for(int k = 0; k< BlockSize; ++k) {
						tmplist.add(fbr.read());
					}
					totalrowread += tmplist.size();
					if(verbose>1) System.out.println("sorting "+tmplist.size()+" rows");
					files.add(sortAndSave(tmplist,cmp));
					tmplist.clear();
				}
			} catch(EOFException oef) {
				if(tmplist.size()>0) {
					if(verbose>1) System.out.println("sorting "+tmplist.size()+" rows");
					totalrowread += tmplist.size();
					files.add(sortAndSave(tmplist,cmp));
					tmplist.clear();
				}
			}
		} finally {
			fbr.close();
		}
		if(verbose>1) System.out.println("I created "+files.size()+" temporary files");
		if(verbose>1) System.out.println("read "+totalrowread+" rows");
		return files;
	}


	public static File sortAndSave(List<Row> tmplist, Comparator<Row> cmp) throws IOException  {
		Collections.sort(tmplist,cmp);
		File newtmpfile = File.createTempFile("sortInBatch", "binaryflatfile");
		newtmpfile.deleteOnExit();
		FlatBinaryWriter fbw = new FlatBinaryWriter(newtmpfile.getAbsolutePath(),tmplist.get(0).values.length);
		try {
			for(Row r : tmplist)
				fbw.write(r);
		} finally {
			fbw.close();
		}
		return newtmpfile;
	}
	/**
	 * This merges a bunch of temporary binary flat files 
	 * @param files
	 * @param outputfile
	 * @return number of rows written
	 */
	public static int mergeSortedFiles(List<File> files, File outputfile, Comparator<Row> cmp) throws IOException {
		PriorityQueue<BinaryFileBuffer> pq = new PriorityQueue<BinaryFileBuffer>();
		for (File f : files) {
			BinaryFileBuffer bfb = new BinaryFileBuffer(f,cmp);
			if(bfb.empty())
				System.out.println("we are in trouble, empty file?"+f.getAbsolutePath());
			pq.add(bfb);
		}
		int numberofcolumns = pq.peek().fbr.columns;
		if(verbose>1) System.out.println("writing to  "+outputfile.getAbsolutePath());
		if(verbose>1) System.out.println("I have   "+pq.size()+" temporary files");
		FlatBinaryWriter fbw = new FlatBinaryWriter(outputfile.getAbsolutePath(),numberofcolumns);
		int rowcounter = 0;
		try {
			while(pq.size()>0) {
				BinaryFileBuffer bfb = pq.poll();
				//if(bfb == null) // should never be null!
				Row r = bfb.pop();
				fbw.write(r);
				++rowcounter;
				if(bfb.empty()) {
					bfb.fbr.close();
					bfb.originalfile.delete();// we don't need you anymore
				} else {
					pq.add(bfb); // add it back
				}
			}
		} finally {
			fbw.close();
		}
		if(verbose>1) System.out.println("I have  written "+rowcounter+" rows");
		return rowcounter;
	}
	
	enum Ordering {LEXICO,REFLECTEDGRAY,VORTEX,FREQ,BINVORTEX,GRAYCODECURVE,BITINTERLEAVE}

	public static void main(String[] args) throws IOException {
		Comparator<Row> comparator = null;// = simplelexicocomparator;
		int commandcounter = 0;
		boolean reordercolumns = false;
		boolean backwardcolumnorder = false;
		Ordering order = Ordering.LEXICO;
		boolean reversevalueorder = true;
		while (args[commandcounter].startsWith("-")) {
			if (args[commandcounter].equals("-reorder")) { 
				// this will reorder the columns
				reordercolumns = true;
			}else if (args[commandcounter].equals("-backwardcolumnorder") || args[commandcounter].equals("-backwardreorder")) { 
				// this will reorder the columns in backward column order (highest card. first)
				reordercolumns = true;
				backwardcolumnorder = true;
			} else if (args[commandcounter].equals("-lexicographic")) {
				order = Ordering.LEXICO;
			} else if (args[commandcounter].equals("-reflectedgray")) {
				order = Ordering.REFLECTEDGRAY;
			} else if (args[commandcounter].equals("-vortex")) {
				order = Ordering.VORTEX;
			} else if (args[commandcounter].equals("-graycodecurve")) {
				order = Ordering.GRAYCODECURVE;
			} else if (args[commandcounter].equals("-bitinterleave")) {
				order = Ordering.BITINTERLEAVE;
			} else if (args[commandcounter].equals("-freq")) {
				order = Ordering.FREQ;
			} else if (args[commandcounter].equals("-dontreversevalueorder")) {
				reversevalueorder = false;
			}  else
				throw new RuntimeException("unknown argument "+args[commandcounter]);
			commandcounter++;
		}
		String binaryfilename = args[commandcounter++];
		if(((reordercolumns) && (order==Ordering.LEXICO)) || (order==Ordering.REFLECTEDGRAY) ){
			final int[] mapping = getColumnReordered(binaryfilename,reordercolumns,backwardcolumnorder);
			if(order == Ordering.LEXICO) {
				/***
				 * lexicographical comparator with column reordering
				 */
				comparator = new Comparator<Row>() {public int compare(Row r1, Row r2){
					for(int k = 0; k<mapping.length;++k)
						if(r1.values[mapping[k]]<r2.values[mapping[k]])
							return -1;
						else if (r1.values[mapping[k]]>r2.values[mapping[k]])
							return 1;
					return 0;}};
			} else if(order == Ordering.REFLECTEDGRAY) {
				if(!reordercolumns) System.out.println("[Warning] For reflected gray, we always reorder the columns.");
				/***
				 * reflected Gray code comparator
				 */
				comparator = new Comparator<Row>() {public int compare(Row r1, Row r2){
					int order = 1;
					for(int k = 0; k<mapping.length;++k) {
						if(r1.values[mapping[k]]<r2.values[mapping[k]])
							return -order;
						else if (r1.values[mapping[k]]>r2.values[mapping[k]])
							return order;
						if(( r1.values[mapping[k]] & 1 ) == 1) order *= -1;
					}
					return 0;}};
			}
		} else if(order == Ordering.GRAYCODECURVE) {
			comparator = getGrayCodeCurveComparator(new FlatBinaryReader(binaryfilename).iterator());
		} else if(order == Ordering.BITINTERLEAVE) {
			comparator = getBitInterleaveComparator(new FlatBinaryReader(binaryfilename).iterator());
		} else if(order == Ordering.VORTEX) {
			comparator = getVortexComparator(binaryfilename,reordercolumns,backwardcolumnorder,reversevalueorder);
		} else if(order == Ordering.LEXICO){
			comparator =  new Comparator<Row>() {public int compare(Row r1, Row r2){
				for(int k = 0; k<r1.values.length;++k)
					if(r1.values[k]<r2.values[k])
						return -1;
					else if (r1.values[k]>r2.values[k])
						return 1;
				return 0;}
			};// default choice is just silly sort;
		} else if (order == Ordering.FREQ) {
			FlatBinaryReader fbr = new FlatBinaryReader(binaryfilename);
			comparator = getFrequencyComparator(fbr.iterator(),fbr.columns);
		} else {
			throw new RuntimeException("unknown order");
		}
		List<File> l = sortInBatch(new File(binaryfilename), comparator) ;
		String binaryoutputfilename = args[commandcounter++];
		mergeSortedFiles(l, new File(binaryoutputfilename), comparator);
	}

	public static Comparator<Row> getGrayCodeCurveComparator(Iterator<Row> i)  {
		int[] max = Util.scanformax(i);
		final int[] shape= new int[max.length];
		int seekmaxmax = 0;
		for(int k =0; k<shape.length;++k) {
			shape[k] = Integer.toBinaryString(max[k]).length();
			if(seekmaxmax < shape[k]) seekmaxmax = shape[k];
		}
		final int maxmax = seekmaxmax;
		//System.out.println("max max = "+maxmax);
			return new Comparator<Row>() {
				public int compare(Row r1, Row r2) {
					int order = -1;
					for (int bit = maxmax; bit >= 0; --bit) {
						for (int c = 0; c < r1.size(); ++c) {
							if (shape[c] < bit)
								continue;
							int mask = 1 << bit;
							if ((r1.get(c) & mask) < (r2.get(c) & mask))
								return order;
							else if ((r1.get(c) & mask) > (r2.get(c) & mask))
								return -order;
							if ((r1.get(c) & mask) != 0)
								order *= -1;
						}
					}
					return 0; 
				}
			};
	}
	public static int graytransform(int x) {
		int z = Integer.toBinaryString(x).length();
		int answer = 0;
		boolean flip = false;
		for(int c = z; c>=0;--c) {
			int mask = 1 << c;
			boolean onedetected = (x & mask) !=0;
			if(onedetected ^ flip) {
				answer += mask;
				flip = ! flip;
			}
		}
		return answer;
	}
	
	public static Comparator<Row> getBitInterleaveComparator(Iterator<Row> i)  {
		int[] max = Util.scanformax(i);
		final int[] shape= new int[max.length];
		int seekmaxmax = 0;
		for(int k =0; k<shape.length;++k) {
			shape[k] = Integer.toBinaryString(max[k]).length();
			if(seekmaxmax < shape[k]) seekmaxmax = shape[k];
		}
		final int maxmax = seekmaxmax;
		//System.out.println("max max = "+maxmax);
			return new Comparator<Row>() {
				public int compare(Row r1, Row r2) {
					int[] tmpr1 = new int[r1.size()];
					int[] tmpr2 = new int[r2.size()];
					for(int k = 0; k<tmpr1.length;++k) {
						tmpr1[k] = graytransform(r1.values[k]);
						tmpr2[k] = graytransform(r2.values[k]);
					}	
					int order = -1;
					for (int bit = maxmax; bit >= 0; --bit) {
						for (int c = 0; c < r1.size(); ++c) {
							if (shape[c] < bit)
								continue;
							int mask = 1 << bit;
							if ((tmpr1[c] & mask) < (tmpr2[c] & mask))
								return order;
							else if ((tmpr1[c] & mask) > (tmpr2[c] & mask))
								return -order;
							if ((tmpr1[c] & mask) != 0)
								order *= -1;
						}
					}
					return 0; 
				}
			};
	}

	public static int[] getColumnReordered(String binaryfilename, boolean reordercolumns, boolean backwardcolumnorder) 
	throws IOException{
		List<Pair<Integer,Integer>> cardinalities = new Vector<Pair<Integer,Integer>>();
		if(verbose>1) System.out.println("scanning the file to establish cardinalities");
		Iterator<Row> i = new FlatBinaryReader(binaryfilename).iterator();
		final int[] histograms = Util.getCardinalities(i);
		//System.out.println(histograms);
		for(int x: histograms)
			cardinalities.add(new Pair<Integer,Integer>(x,cardinalities.size()));
		if(verbose>1) System.out.println("processing cardinalities");
		if(reordercolumns) 
			if(backwardcolumnorder) Collections.sort(cardinalities, Collections.reverseOrder());
			else Collections.sort(cardinalities);
		final int[] mapping = new int[cardinalities.size()];
		for (int k = 0; k <cardinalities.size(); ++k)
			mapping[k] = cardinalities.get(k).second();
		return mapping;
	}	

	
	public static Comparator<Row> getFrequencyComparator(Iterator<Row> i, int c/*String binaryfilename*/)  {
		//FlatBinaryReader fbr = new FlatBinaryReader(binaryfilename);
		//Iterator<Row> i = fbr.iterator();
		//final TObjectIntHashMap<AttributeValue>
		//final HashMap<AttributeValue,Integer>
		final TObjectIntHashMap<AttributeValue> mixedattributevaluehisto = Util.getMixedHistogram(i);// unavoidably, this eats up memory!
		final FreqWeigthedAttributeValue[] avs1 = new FreqWeigthedAttributeValue[c];		
		final FreqWeigthedAttributeValue[] avs2 = new FreqWeigthedAttributeValue[c];
		for(int k = 0; k< c; ++k) {
			avs1[k] = new FreqWeigthedAttributeValue(0,0,mixedattributevaluehisto);
			avs2[k] = new FreqWeigthedAttributeValue(0,0,mixedattributevaluehisto);
		}
		return new Comparator<Row>() {public int compare(Row r1, Row r2){
			for(int k = 0; k<r1.size();++k) {
				avs1[k].av.dim=k;
				avs2[k].av.dim=k;
				avs1[k].av.value=r1.values[k];
				avs2[k].av.value=r2.values[k];
				avs1[k].freq = mixedattributevaluehisto.get(avs1[k].av);
				avs2[k].freq = mixedattributevaluehisto.get(avs2[k].av);
			}
			Arrays.sort(avs1,Collections.reverseOrder());
			Arrays.sort(avs2,Collections.reverseOrder());
			//System.out.print(r1+" becomes ");
			//for(int k = 0; k<r1.size();++k) {
			//	System.out.print(avs1[k] +" | ");
			//}
			//System.out.println();
			//if(avs1[0].freq>avs1[1].freq) System.out.println("fuck");
			for(int k = 0; k<r1.size();++k) {
				int cmp = avs1[k].compareTo( avs2[k]);
				if(cmp != 0) return cmp;
			}
			return 0;
		}};
	}

	public static Comparator<Row> getVortexComparator(String binaryfilename, boolean reordercolumns, boolean backwardcolumnorder, boolean reversevalueorder) 
	throws IOException{
		/**
		 * exotic order: Vortex ordering.
		 */
		//if(reordercolumns) System.out.println("[Warning] For the Vortex order, the columns are not reordered in this implementation.");
		// this Daniel's secret project 
		// Vortex behaves like lexico. but the number of runs is more 
		// even accross columns.
		// 
		// the downside is significant: it does a lot more work.
		//
		// and the overall number of runs is not reduced.
		//
		// first compute a map so that the most frequent attribute values are first
		List<Pair<Integer,Integer>> cardinalities = new Vector<Pair<Integer,Integer>>();
		if(verbose>1) System.out.println("scanning the file to establish cardinalities");
		Iterator<Row> i = new FlatBinaryReader(binaryfilename).iterator();
		final List<TIntIntHashMap> histograms = Util.getHistogram(i);
		//System.out.println(histograms);
		for(TIntIntHashMap x: histograms)
			cardinalities.add(new Pair<Integer,Integer>(x.size(),cardinalities.size()));
		if(verbose>1) System.out.println("processing cardinalities");
		if(reordercolumns) 
			if(backwardcolumnorder) Collections.sort(cardinalities, Collections.reverseOrder());
			else Collections.sort(cardinalities);
		final int[] mapping = new int[cardinalities.size()];
		for (int k = 0; k <cardinalities.size(); ++k)
			mapping[k] = cardinalities.get(k).second();
		final Vector<int[]> reorderattributevalues= new Vector<int[]>();
		for(TIntIntHashMap h: histograms) {
			final Vector<Pair<Integer,Integer>> freqvaluevec = new Vector<Pair<Integer,Integer>>();
			//System.out.println(h.size());
			final MutableInteger maxkey = new MutableInteger();
			maxkey.content= 0;
			h.forEachEntry(new TIntIntProcedure() {
				public boolean execute(int key, int value) {
					freqvaluevec.add(new Pair<Integer,Integer>(value,key));
					if(key>maxkey.content) maxkey.content = key;
					return true;
				}});
			if(reversevalueorder)
				Collections.sort(freqvaluevec,Collections.reverseOrder());
			else
				Collections.sort(freqvaluevec);//,Collections.reverseOrder());
			int[] freqmapping = new int[maxkey.content+1];
			for(int k = 0; k< freqvaluevec.size();++k) {
				freqmapping[freqvaluevec.get(k).second()] = k;
			}
			reorderattributevalues.add(freqmapping);
		}
		cardinalities = null; // recover the memory
		// next bit is just some buffer
		final Vector<AttributeValue> x1 = new Vector<AttributeValue>(); 
		final Vector<AttributeValue> x2 = new Vector<AttributeValue>();
		for(int k = 0; k< histograms.size();++k) {
			x1.add(new AttributeValue(0,0));
			x2.add(new AttributeValue(0,0));
		}
		// this is the vortex comparator (a tad expensive, but could be streamlined)
		return new Comparator<Row>() {public int compare(Row r1, Row r2){
			for(int k = 0; k<r1.size();++k) {
				int[] reorder = reorderattributevalues.get(k);
				AttributeValue p1 = x1.get(k);
				AttributeValue p2 = x2.get(k);
				p1.dim = mapping[k];
				p2.dim = mapping[k];
				p1.value = reorder[r1.get(k)];
				p2.value = reorder[r2.get(k)];
			}
			Collections.sort(x1,AttributeValue.valuethendimcomp);
			Collections.sort(x2,AttributeValue.valuethendimcomp);				
			int order = 1;
			for(int k = 0; k<x1.size();++k) {
				int resultofcomp = AttributeValue.valuethendimcomp.compare( x1.get(k),x2.get(k));
				if(resultofcomp != 0)
					return resultofcomp *order;
				order *= -1;
			}
			return 0;}};
	}
	
}

class BinaryFileBuffer  implements Comparable<BinaryFileBuffer>{
	public static int BUFFERSIZE = 512;
	public FlatBinaryReader fbr;
	private List<Row> buf = new Vector<Row>();
	int currentpointer = 0;
	Comparator<Row> mCMP;
	public File originalfile;
	private static final int verbose = 0;
	
	public BinaryFileBuffer(File f, Comparator<Row> cmp) throws IOException {
		originalfile = f;
		mCMP = cmp;
		fbr = new FlatBinaryReader(f.getAbsolutePath());
		reload();
		if(verbose>1)
  		  System.out.println("loaded buffer on "+f.getAbsolutePath()+" -- got "+buf.size()+" rows");
	}
	
	public boolean empty() {
		return buf.size()==0;
	}
	
	private void reload() throws IOException {
		  buf.clear();
		  try {
	 		  while(buf.size()<BUFFERSIZE)
				buf.add(fbr.read());
			} catch(EOFException oef) {
			}		
	}
	
	
	public Row peek() {
		if(empty()) return null;
		return buf.get(currentpointer);
	}
	public Row pop() throws IOException {
	  Row answer = peek();
	  ++currentpointer;
	  if(currentpointer == buf.size()) {
		  reload();
		  currentpointer = 0;
	  }
	  return answer;
	}
	
	public int compareTo(BinaryFileBuffer b) {
		return mCMP.compare(peek(), b.peek());
	}
	

}



