package inmemory;

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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import util.MutableInteger;
import util.Pair;
import util.Util;
import util.Zipfian;

public class Vortex  extends AbstractRowReordering {
	ColumnOrderer.ColOrder mCO;
	public Vortex(ColumnOrderer.ColOrder co) {mCO=co;}
	public Vortex() {mCO=ColumnOrderer.ColOrder.IncreasingCardinality;}


	public Comparator<Row> getVortexComparator(List<Row> table) {
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
		Iterator<Row> i = table.iterator();
		final List<TIntIntHashMap> histograms = Util.getHistogram(i); 
		/*List<Pair<Integer,Integer>> cardinalities = new Vector<Pair<Integer,Integer>>();;
		
		
		//System.out.println(histograms);
		for(TIntIntHashMap x: histograms)
			cardinalities.add(new Pair<Integer,Integer>(x.size(),cardinalities.size()));
		if(reordercolumns) Collections.sort(cardinalities, Collections.reverseOrder());
		else Collections.sort(cardinalities);
		final int[] mapping = new int[cardinalities.size()];
		for (int k = 0; k <cardinalities.size(); ++k)
			mapping[k] = cardinalities.get(k).second();*/
		ColumnOrderer co = new ColumnOrderer(table);
		final List<Integer> ascendingCols = co.listBy(mCO);
		final Integer[] mapping = ascendingCols.toArray(new Integer[0]);
		final Vector<int[]> reorderattributevalues= new Vector<int[]>();
		for(TIntIntHashMap h: histograms) {
			final Vector<Pair<Integer,Integer>> freqvaluevec = new Vector<Pair<Integer,Integer>>();
			//System.out.println(h.size());
			final MutableInteger maxkey = new MutableInteger();
			maxkey.content= 0;
			TIntIntProcedure proc = new TIntIntProcedure() {
				//public int maxkey = 0;
				public boolean execute(int key, int value) {
					freqvaluevec.add(new Pair<Integer,Integer>(value,key));
					if(key>maxkey.content) maxkey.content = key;
					return true;
				}};
			h.forEachEntry(proc);
			Collections.sort(freqvaluevec,Collections.reverseOrder());//Collections.sort(freqvaluevec);//,Collections.reverseOrder());
			int[] freqmapping = new int[maxkey.content+1];
			//System.out.println("\n"+maxkey.content+" "+);
			for(int k = 0; k< freqvaluevec.size();++k) {
				freqmapping[freqvaluevec.get(k).second()] = k;
			}
			reorderattributevalues.add(freqmapping);
		}
		//cardinalities = null; // recover the memory
		// next bit is just some buffer
		final Vector<AttributeValue> x1 = new Vector<AttributeValue>(); 
		final Vector<AttributeValue> x2 = new Vector<AttributeValue>();
		for(int k = 0; k< histograms.size();++k) {
			x1.add(new AttributeValue(0,0));
			x2.add(new AttributeValue(0,0));
		}
		//Vector<int[]> xx= reorderattributevalues;
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

	@Override
	public List<Row> solve(List<Row> table) {
		Comparator<Row> c =  getVortexComparator(table);
		Collections.sort(table,c);
		// TODO Auto-generated method stub
		return table;
	}
	
	public static void main(String [] args) {
		Vector<Row> r = Zipfian.generateZipfianTable(2, 20);
		Vortex v = new Vortex();
		List<Row>rr=v.solve(r);
		System.out.println(rr);
		
	}

}

