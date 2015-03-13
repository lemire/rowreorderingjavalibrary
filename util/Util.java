package util;

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

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectIntHashMap;
import inmemory.AttributeValue;
import inmemory.BlockRunCounter;
import inmemory.Row;

import java.util.*;
public class Util {
	
	// can be made faster with table look-ups.
	public static int integerlog(int x) {
		int r= 0;
		if(x<0) x=-x;
		while(x>0) {
			x>>=1;
			++r;
		}
		return r;
	}
	
	public static  long sum(long[] x) {
		long a = 0;
		for (long xx : x) a+= xx;
		return a;
	}
	
	public static  void print(long[] x) {
		for (long xx : x) System.out.print(xx+" ");
		System.out.println();
	}

	public static List<Row> randomTable(int N, int M, int n){
		Set<Row> table = new HashSet<Row>();
		MersenneTwisterFast rand = new MersenneTwisterFast();
		for(int k=0;k<n;++k) {
			int[] val = new int[N];
			for(int m=0;m<N;++m)
				val[m] = rand.nextInt(M);
			Row r= new Row(val);
			table.add(r);
		}
		List<Row> finalanswer = new Vector<Row>();
		finalanswer.addAll(table);
		return finalanswer;
	}
	

	public static int numberOfRuns(Iterator<Row> i) {
		int count = 0;
		Row pre = i.next();
		count += pre.values.length;
		while(i.hasNext()) {
			Row r = i.next();
			count += pre.Hamming(r);
			pre=r;
		}
		return count;
	}

	public static int[] numberOfRunsPerColumn(Iterator<Row> i) {
		Row pre = i.next();
		int[] count = new int[pre.size()];
		for(int k = 0; k<count.length; ++k)
			count[k]++;
		while(i.hasNext()) {
			Row r = i.next();
			for(int k = 0; k<count.length; ++k)
				if(pre.get(k)!= r.get(k)) count[k]++;
			pre=r;
		}
		return count;
	}

	public static int numberOfRuns(Iterator<Row> i, int blocksize) {
		if(!i.hasNext()) return 0;
		Row pre = i.next();
		int c = pre.size();
		Vector<BlockRunCounter> brc = new Vector<BlockRunCounter>();
		for( int k = 0;k<c;++k) {
			brc.add(new BlockRunCounter(blocksize));
			brc.get(k).newValue(pre.get(k));
		}
		while(i.hasNext()) {
			Row r = i.next();
			for( int k = 0;k<c;++k) {
				brc.add(new BlockRunCounter(blocksize));
				brc.get(k).newValue(r.get(k));
			}
		}
		int count = 0;
		for( int k = 0;k<c;++k) {
			brc.get(k).close();
			count += brc.get(k).NumberOfRuns;
		}
		return count;
	}

	public static List<TIntIntHashMap> getHistogram(Iterator<Row> i) {
		Row r = i.next();
		List<TIntIntHashMap> answer = new Vector<TIntIntHashMap>();
		for(int k = 0; k < r.values.length;++k) {
			answer.add(new TIntIntHashMap ());
			answer.get(k).put(r.values[k], 1);
		}
		while(i.hasNext()) {
			r = i.next();
			for(int k = 0; k < r.values.length;++k) {
				TIntIntHashMap histo = answer.get(k);
				if(histo.containsKey(r.values[k])) {
					int previouscounter = histo.get(r.values[k]);
					histo.put(r.values[k], previouscounter+1);
				} else {
					histo.put(r.values[k], 1);
				}
			}
		}
		return answer;
	}
	public static TObjectIntHashMap<AttributeValue> getMixedHistogram(Iterator<Row> i) {
		TObjectIntHashMap<AttributeValue> answer = new TObjectIntHashMap<AttributeValue>();
		while(i.hasNext()) {
			Row r = i.next();
			//System.out.println(r);
			for(int k = 0; k < r.values.length;++k) {
				AttributeValue ab = new AttributeValue(k,r.values[k]);
				answer.putIfAbsent(ab, 0);
				answer.increment(ab);
				//System.out.println("stored "+answer.get(ab)+" in "+ab);
			}
		}
		return answer;
	}
	public static int[] getCardinalities(Iterator<Row> i) {
		Row r = i.next();
		int[] answer = new int[r.size()];
		for(int k = 0; k < r.values.length;++k) {
			answer[k]= r.values[k]< answer[k] ? answer[k] : r.values[k];
		}
		while(i.hasNext()) {
			r = i.next();
			for(int k = 0; k < r.values.length;++k) {
				answer[k]= r.values[k] < answer[k] ? answer[k] : r.values[k];
			}
		}
		for(int k = 0; k<answer.length;++k)
			answer[k]++;
		return answer;
	}	
	public static int[] scanformax(Iterator<Row> i) {
		Row r = i.next();
		int[] answer = new int[r.size()];
		for(int k = 0; k < r.values.length;++k) {
			answer[k]= r.values[k]< answer[k] ? answer[k] : r.values[k];
		}
		while(i.hasNext()) {
			r = i.next();
			for(int k = 0; k < r.values.length;++k) {
				answer[k]= r.values[k] < answer[k] ? answer[k] : r.values[k];
			}
		}
		return answer;
	}
	public static int[] getCardinalitiesTheSlowWay(Iterator<Row> i) {
		Row r = i.next();
		int[] answer = new int[r.size()];
		TIntHashSet[] counters = new TIntHashSet[r.size()];
		for(int k = 0; k < r.values.length;++k) {
			counters[k]= new TIntHashSet();
			counters[k].add(r.values[k]);
		}
		while(i.hasNext()) {
			r = i.next();
			for(int k = 0; k < r.values.length;++k) {
				counters[k].add(r.values[k]);
			}
		}
		return answer;
	}
	
	public static Row popNearestNeighbor(Collection<Row> table, final Row target) {
		assert(table.size()>1);
		Iterator<Row> i = table.iterator();
		Row nearest = i.next();
		int LowestCost = target.Hamming(nearest);
		while(i.hasNext()) {
			Row r = i.next();
			//System.out.println(target);
			int ccost = target.Hamming(r);
			if(LowestCost>ccost) {
				LowestCost = ccost;
				nearest = r;
				//if(LowestCost <= acceptedlowerbound) break;// stop looking
			}
		}
		table.remove(nearest);
		return nearest;
	}
	
	
	public static Row popNearestNeighbor(Collection<Row> table, final List<Row> target) {
		assert(table.size()>1);
		Iterator<Row> i = table.iterator();
		Row nearest = i.next();
		int LowestCost = target.get(target.size()-1).Hamming(nearest);
		while(i.hasNext()) {
			Row r = i.next();
			//System.out.println(target);
			int ccost = target.get(target.size()-1).Hamming(r);
			if(LowestCost>ccost) {
				LowestCost = ccost;
				nearest = r;
				//if(LowestCost <= acceptedlowerbound) break;// stop looking
			} else if(LowestCost == ccost) {
				// we are going to break
				if(target.size()==1) continue;
				for(int j = target.size()-2;j>=0;--j) {
					int staysame = target.get(j).Hamming(nearest);
					int different = target.get(j).Hamming(r);
					if(staysame > different) {
						nearest = r;
						break;
					}
				}
			}
		}
		table.remove(nearest);
		return nearest;
	}
	public static int costToInsert(List<Row> table, Row newrow, int i) {
		if(i==0)
			return newrow.Hamming(table.get(i));
		if(i==table.size())
			return newrow.Hamming(table.get(i-1));
		return newrow.Hamming(table.get(i-1))+ newrow.Hamming(table.get(i))-table.get(i).Hamming(table.get(i-1));
	}


	public static Pair<Integer,Integer> bestInsert(List<Row> table, Row newrow) {
		int LowestCost = newrow.Hamming(table.get(0));
		int bestpos = 0;//
		for(int pos = 0; pos <table.size()-1; ++pos) {
			int oldcost = table.get(pos).Hamming(table.get(pos+1));
			int newcost = table.get(pos).Hamming(newrow) + newrow.Hamming(table.get(pos+1));
			int diffcost = newcost - oldcost;
			assert(diffcost>=0); // triangle inequality
			if(diffcost < LowestCost) {
				LowestCost = diffcost;
				bestpos= pos+1;
			}
		}	
		int lastposcost  = newrow.Hamming(table.get(table.size()-1));
		if(lastposcost < LowestCost) {
			bestpos=table.size();
			LowestCost = lastposcost;
		}
		return new Pair<Integer,Integer>(bestpos,LowestCost);
	}
	

	
	public static  Pair<Integer,Integer> insertCheaply(List<Row> table, Row newrow) {
		Pair<Integer,Integer> wheretoinsert = bestInsert(table, newrow);
		table.add(wheretoinsert.first().intValue(), newrow);
		return wheretoinsert;
	}
}
