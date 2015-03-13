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
/*
 * Implementation of the standard nearest insertion tour-construction heuristic for TSP.
 * 
 * There is a trivial variation, the double-ended nearest neighbor, but nearest neighbor is already very slow. 
 */


import java.util.*;

import util.MersenneTwisterFast;
import util.Util;


public class NearestInsertion extends AbstractRowReordering {
	public NearestInsertion() {}
	@Override
	public List<Row> solve(List<Row> table) {
		List<Row> answer = new LinkedList<Row>();// for fast inserts
		MersenneTwisterFast rand = new MersenneTwisterFast();
		Row firstelement = table.get(rand.nextInt(table.size()));// ad hoc choice
		table.remove(firstelement);
		answer.add(firstelement);
		Hashtable<int[],Integer> DistanceBuffer = new Hashtable<int[],Integer>();
		int LowestDistance = Integer.MAX_VALUE;
		Row nextcandidate = null;
		for(Row x : table) {
			int dist = firstelement.Hamming(x);
			DistanceBuffer.put(x.values,firstelement.Hamming(x) );
			if(dist < LowestDistance) {
				nextcandidate = x;
				LowestDistance = dist;
			}
		}
		while(table.size()>0) {
			Row NearestNeighbor = nextcandidate;
			table.remove(NearestNeighbor);
			DistanceBuffer.remove(NearestNeighbor);
			Util.insertCheaply(answer, NearestNeighbor);
			LowestDistance = Integer.MAX_VALUE;
			for(Row x : table) {
				int dist = Math.min(DistanceBuffer.get(x.values),x.Hamming(NearestNeighbor));
				DistanceBuffer.put(x.values, dist);
				if(dist < LowestDistance) {
					nextcandidate = x;
					LowestDistance = dist;
				}
			}			
		}
		return answer;
	}
}
