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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

// external-memory equivalent to the inmemory.DCSort
public class DCSort {



	public static void main(String[] args) throws IOException {
		if(args.length<3) {
			System.out.println("please give my a binary input file, a binary output file and a block size");
			return;
		}
		String inputfile = args[0];
		String outputfile = args[1];
		int blocksize = Integer.parseInt(args[2]);
		//System.out.println("scanning for column cardinalities...");
		int[] columnmapping = Sorting.getColumnReordered(inputfile, true, false);
		String in = inputfile;
		boolean dontdelete = true;
		for(int k = 0; k<columnmapping.length;++k) {
			File f = File.createTempFile("dcsort", "bin");
			f.deleteOnExit();
			String out = f.getCanonicalPath();
			//System.out.println("sorting "+in+ " to newly created "+out);
			dcsortOnFirstColumns(in, out, columnmapping, k+1);
			if(dontdelete) {
				dontdelete = false;
			} else {
				new File(in).delete();
				//System.out.println("deleting "+in);
			}
			File f2 = File.createTempFile("dcsort", "bin");
			f2.deleteOnExit();
			String out2 = f2.getCanonicalPath();
			//System.out.println("created "+out2);
			markShortRunsAsDC(out,out2,columnmapping[k], blocksize);
			f.delete();
			//System.out.println("deleted "+out);
			in = out2;
			//System.out.println("new input is "+in);
		}
		makeAllValuesPositiveAgain(in,outputfile);

	}
	
	public static void makeAllValuesPositiveAgain(String inbinaryfilename,String outbinaryfilename) throws IOException {
		FlatBinaryReader fbr= new FlatBinaryReader(inbinaryfilename);
		FlatBinaryWriter fbw = new FlatBinaryWriter(outbinaryfilename,fbr.columns);	
		Iterator<Row> i = fbr.iterator();
		while(i.hasNext()) {
			Row r = i.next();
			for(int k = 0; k<r.size(); ++k) {
				if(r.get(k)<0)
					r.values[k] += (1<<31);
			}
			fbw.write(r);
		}
		fbw.close();
	}
	
	/**
	 * This would be Owen's version, but I think it does not work as well.
	 * 
	 * @param inbinaryfilename
	 * @param outbinaryfilename
	 * @param whichcolumn
	 * @param blocksize
	 * @throws IOException
	 */
	public static void owenmarkShortRunsAsDC(String inbinaryfilename,String outbinaryfilename,final int whichcolumn, int blocksize) throws IOException {
		FlatBinaryReader fbr= new FlatBinaryReader(inbinaryfilename);
		FlatBinaryWriter fbw = new FlatBinaryWriter(outbinaryfilename,fbr.columns);
		ArrayList<Row> al = new ArrayList<Row>(blocksize);
		Iterator<Row> i = fbr.iterator();
		int lastvalue = 0;
		int lastvaluelength = 0;
		while(i.hasNext()) {
			Row r = i.next();
			al.add(r);
			if(al.size()==blocksize) {
				int v = al.get(0).get(whichcolumn);
				int j = 0;
				while(++j < blocksize) {
					if(al.get(j).get(whichcolumn)!= v) break;
				}
				if(lastvalue == v) {
					lastvaluelength += j;
				} else {
					lastvalue = v;
					lastvaluelength = j;
				}
				if(lastvaluelength<blocksize) {
					for(int k = 0;k<j;++k)
						al.get(k).values[whichcolumn] -= (1<<31);
				}
				for(int k = 0;k<j;++k) {
					fbw.write(al.get(k));
				}
				//if(j==blocksize) throw new IllegalArgumentException("ffff");
				if(j == blocksize)
					al.clear();
				else 
				    al = new ArrayList<Row>(al.subList(j, blocksize));
			}
		}
		while(al.size()>0) {
			int v = al.get(0).get(whichcolumn);
			int j = 0;
			while(++j < al.size()) {
				if(al.get(j).get(whichcolumn)!= v) break;
			}
			if(lastvalue == v) {
				lastvaluelength += j;
			} else {
				lastvalue = v;
				lastvaluelength = j;
			}
			if(lastvaluelength < blocksize) {
				for(int k = 0;k<j;++k)
					al.get(k).values[whichcolumn] -= (1<<31);
			}
			for(int k = 0;k<j;++k) {
				fbw.write(al.get(k));
			}
			al = new ArrayList<Row>(al.subList(j, al.size()));
		}
		fbw.close();
	}
	
	/*
	 * This is Daniel's way of marking the DC values. I think it works better than Owen's.
	 */
	public static void markShortRunsAsDC(String inbinaryfilename,String outbinaryfilename,final int whichcolumn, int blocksize) throws IOException {
		FlatBinaryReader fbr= new FlatBinaryReader(inbinaryfilename);
		FlatBinaryWriter fbw = new FlatBinaryWriter(outbinaryfilename,fbr.columns);
		ArrayList<Row> al = new ArrayList<Row>(blocksize);
		Iterator<Row> i = fbr.iterator();
		while(i.hasNext()) {
			Row r = i.next();
			al.add(r);
			if(al.size()==blocksize) {
				int v = al.get(0).get(whichcolumn);
				int j = 0;
				while(++j < blocksize) {
					if(al.get(j).get(whichcolumn)!= v) break;
				}
				if(j!=blocksize) {
					for(int k = 0;k<j;++k)
						al.get(k).values[whichcolumn] -= (1<<31);
				}
				for(int k = 0;k<j;++k) {
					fbw.write(al.get(k));
				}
				al = new ArrayList<Row>(al.subList(j, blocksize));
			}
		}
		if(al.size()>0) {
			int v = al.get(0).get(whichcolumn);
			int j = 0;
			while(++j < al.size()) {
				if(al.get(j).get(whichcolumn)!= v) break;
			}
			if(j!=blocksize) {
				for(int k = 0;k<al.size();++k)
					al.get(k).values[whichcolumn] -= (1<<31);
			}
			for(int k = 0;k<al.size();++k) {
				fbw.write(al.get(k));
			}
		}
		fbw.close();
	}
	
	public static void dcsortOnFirstColumns(String binaryfilename, String binaryoutputfilename, final int[] columnmapping, final int howmanycolumns) throws IOException {
		Comparator<Row> c = new Comparator<Row>() {
			public int compare(Row r1, Row r2){
				for(int k = 0; k<howmanycolumns-1; ++k) {
					if (r1.get(columnmapping[k])< 0 &&
							r2.get(columnmapping[k]) >= 0) return -1;
					if (r1.get(columnmapping[k]) >= 0 &&
							r2.get(columnmapping[k]) < 0) return +1;
					// now, both short or long run members
					if (r1.get(columnmapping[k])< 0) continue;
					// both are long runs 
					if (r1.get(columnmapping[k]) <  r2.get(columnmapping[k])) return -1;
					if (r1.get(columnmapping[k]) >  r2.get(columnmapping[k])) return +1;
				}
				for(int k = howmanycolumns-1; k<columnmapping.length;++k) {
					if(r1.get(columnmapping[k])-r2.get(columnmapping[k]) != 0)
						return r1.get(columnmapping[k])-r2.get(columnmapping[k]);
				}
				return 0;
			}};
			List<File> l = Sorting.sortInBatch(new File(binaryfilename), c) ;
			Sorting.mergeSortedFiles(l, new File(binaryoutputfilename), c);
	}
}
