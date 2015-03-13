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
import inmemory.Row;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;


/*
 * Idea is to remap the values per frequency. For example, the most
 * frequent value becomes 0, the least frequent because the largest
 * integer.
 */
public class RemapValues {
	public static void main(String args[]) throws IOException {
		if(args.length<2) {
			System.out.println("please provide input and an output binary flat files");
			return;
		}
		int cmdcounter = 0;
		String binaryfilename = args[cmdcounter++];
		System.out.println("running a first pass through the input file");
	    /***
	     * Really ugly code doing something simple in a complicated way: 
	     */
		TIntIntHashMap[] maps;
		{
			TIntIntHashMap[] freqs = getFrequencies(new FlatBinaryReader(binaryfilename).iterator());
			maps = new TIntIntHashMap[freqs.length];
			for(int k = 0; k<maps.length; ++k) {
				maps[k] =  fromFrequenciesToMap(freqs[k]);	freqs[k] = null;
			}
		}
		System.out.println("generating the output file");
		String outbinaryfilename = args[cmdcounter++];
		FlatBinaryReader fbr = new FlatBinaryReader(binaryfilename);
		FlatBinaryWriter fbw = new FlatBinaryWriter(outbinaryfilename,fbr.columns);
		Iterator<Row> i = fbr.iterator();
		try {
			while(i.hasNext()) {
				Row r = i.next();
				for(int k = 0; k<r.size();++k)
					r.values[k] = maps[k].get(r.values[k]);
				fbw.write(r);
			}
		} finally{
			fbw.close();
			fbr.close();
		}
	}
	
	public static TIntIntHashMap fromFrequenciesToMap(TIntIntHashMap mymap) {
		final Vector<int[]> tmp = new Vector<int[]>();
		mymap.forEachEntry(new TIntIntProcedure() {
			public boolean execute(int arg0, int arg1) {
				int[] x = new int[2];
				x[0] = arg1;
				x[1] = arg0;
				tmp.add(x);
				return true;
			}});
		Collections.sort(tmp,new Comparator<int[]>() {
			public int compare(int[] o1, int[] o2) {
				return  -(o1[0]-o2[0]); 
			}});
		TIntIntHashMap answer = new TIntIntHashMap();
		int val = 0;
		for(int[] freqvalpair : tmp) {
			//System.out.println("val "+freqvalpair[1]+" with freq "+freqvalpair[0]+" is mapped to "+val);
			answer.put(freqvalpair[1],val++);
		}
		return answer;
	}
	
	public static TIntIntHashMap[] getFrequencies(Iterator<Row> i ) {
		TIntIntHashMap[] ans;
		if(i.hasNext()) {
			Row r = i.next();
			ans = new TIntIntHashMap[r.size()];
			for(int k = 0 ; k < r.size(); ++k) {
				ans[k] = new TIntIntHashMap();
				ans[k].put(r.get(k), 1);
			}
		} else return new TIntIntHashMap[0];
		while(i.hasNext()) {
			Row r = i.next();
			for(int k = 0 ; k < r.size(); ++k) {
				int val = r.get(k);
				if(ans[k].contains(val))
					ans[k].put(val, ans[k].get(val)+1);
				else 
					ans[k].put(val, 1);
			}
		}
		return ans;
	}
}
