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

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectProcedure;

import java.util.*;

import jdbm.RecordManager;
import jdbm.btree.BTree;

import util.MersenneTwisterFast;
import util.Pair;
import util.Util;

public class MultipleLists extends AbstractRowReordering {
	boolean useReflectedGrayCode;double p;
	public MultipleLists() {useReflectedGrayCode = false; p = Integer.MAX_VALUE;}
	public MultipleLists(boolean graycode) {useReflectedGrayCode=graycode;p = Integer.MAX_VALUE;}
	public MultipleLists(boolean graycode, int maxnumberoftrees) {useReflectedGrayCode=graycode;p = 1.0;p=maxnumberoftrees;}
	public List<Row> solve(List<Row> table) {
		List<Comparator<int[]>> comps = getAllComparators(table,useReflectedGrayCode);
		while(comps.size()>p) comps.remove(comps.size()-1);
		TreeMap<int[],Integer> tm = new TreeMap<int[],Integer>(comps.get(0));
		for(Row r : table) {
			if(! tm.containsKey(r.values)) {
			  tm.put(r.values, 1);
			} else {
				tm.put(r.values, tm.get(r.values)+1);
			}
		}
		List<Row> answer = new Vector<Row>();// fast append
		MersenneTwisterFast rand = new MersenneTwisterFast();
		Row currentlastelement = table.get(rand.nextInt(table.size()));// ad hoc choice
		{
			int count = tm.get(currentlastelement.values);
			for(int k = 0; k<count;++k)
			  answer.add(currentlastelement);
			tm.remove(currentlastelement.values);
		}
		Vector<TreeSet<int[]>> lists = new Vector<TreeSet<int[]>>();
		for(Comparator<int[]> c : comps ) {
			//Collections.sort(table,c);
			final TreeSet<int[]> newlist = new TreeSet<int[]>(c);// will use memory like crazy, can be improved with RowIDs
			newlist.addAll(tm.keySet());
			lists.add(newlist);
		}
		while(tm.size()>0) {
			Row r = new Row(popNearestNeighbor(answer.get(answer.size()-1).values, lists));
			int count = tm.remove(r.values); 
			for(int k = 0; k<count;++k)
			  answer.add(r);
		}
		return answer;
	}
	
	public int[] popNearestNeighbor(int[] target, Vector<TreeSet<int[]>> lists) {
		int[] answer = null;
		int besthamming = Integer.MAX_VALUE;
		int whichbtree = -1;
		for (int k = 0; k < lists.size(); ++k) {
			TreeSet<int[]> ts = lists.get(k);
			int[] candidate1 = null, candidate2 = null;
			SortedSet<int[]> head = ts.headSet(target);
			if(!head.isEmpty()) candidate1 = head.last();
			SortedSet<int[]> tail = ts.tailSet(target);
			if(!tail.isEmpty()) candidate2 = tail.first();
			if(candidate1 != null) {
				int h1 = Row.Hamming(candidate1,target);
				if(h1<=besthamming) {
					besthamming = h1;
					answer = candidate1;
					whichbtree = k;
				}
			}
			if(candidate2 != null) {
				int h2 = Row.Hamming(candidate2,target);
				if(h2<=besthamming) {
					besthamming = h2;
					answer = candidate2;
					whichbtree = k;
				}
			}			
		}
		// now we found the best candidate
		for(TreeSet<int[]> ts : lists) {
			if(!ts.remove(answer)) System.err.println("something is wrong! "+answer);
		}
		if(true) {// goal here is to encourage long runs
			  // we reorder the btree so that the selected one comes on last which makes it more likely to get picked again
			TreeSet<int[]> guy = lists.remove(whichbtree);
			lists.add(guy);
		}
		return answer;
	}
	
	
	public static int[] rotatedMapping(int[] map) {
		int[] mapping = new int[map.length];
		for(int k = 0; k<mapping.length;++k) {
			mapping[k] = map[k] + 1;
			if(mapping[k] >= map.length) mapping[k] = 0;
		}
		return mapping;
		
	}
	
	public static  int[] getBestMapping(List<Row> table) {
		int[] cards = Util.getCardinalities(table.iterator());
		List<Pair<Integer,Integer>> cardinalities = new Vector<Pair<Integer,Integer>>();
		for(int x: cards)
			cardinalities.add(new Pair<Integer,Integer>(x,cardinalities.size()));
		Collections.sort(cardinalities);
		final int[] mapping = new int[cardinalities.size()];
		for (int k = 0; k <cardinalities.size(); ++k)
			mapping[k] = cardinalities.get(k).second();
		return mapping;
	}
	
	public static List<Comparator<int[]>> getAllComparators(List<Row> table, boolean graycode) {
		int[] map =  getBestMapping(table);
		//System.out.println("detected "+map.length+" dimensions");
		Vector<Comparator<int[]>> comps = new Vector<Comparator<int[]>>();
		if(graycode) comps.add(createReflectedGrayComparator(map)); else comps.add(createComparator(map));
		for(int k = 0; k< map.length-1;++k) {
			map = rotatedMapping(map);
			if(graycode) comps.add(createReflectedGrayComparator(map)); else comps.add(createComparator(map));
		}
		return comps;
	}
	public static Comparator<int[]> createComparator(final int[] mapping) {
		return new Comparator<int[]>() {public int compare(int[] r1, int[] r2){
			for(int k = 0; k<mapping.length;++k)
				if(r1[mapping[k]]<r2[mapping[k]])
					return -1;
				else if (r1[mapping[k]]>r2[mapping[k]])
					return 1;
			return 0;}};
	}

	public static Comparator<int[]> createReflectedGrayComparator(final int[] mapping) {
		return new Comparator<int[]>() {public int compare(int[] r1, int[] r2){
			int order = 1;
			for(int k = 0; k<mapping.length;++k) {
				if(r1[mapping[k]]<r2[mapping[k]])
					return -order;
				else if (r1[mapping[k]]>r2[mapping[k]])
					return order;
				if(r1[mapping[k]] % 2 == 1) 
					order *= (-1);
			}
			return 0;}};
	}

}
