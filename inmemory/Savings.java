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
 * This heuristic for TSP reportedly works very well in practice.
 * It is due to Clarke and Wright
 * 
 * Scheduling of Vehicles from a Central Depot to a Number of 
 * Delivery Points
 *  G. Clarke and J. W. Wright
 *  Operations Research, Vol. 12, No. 4 (Jul. - Aug., 1964), pp. 568-581
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import util.MersenneTwisterFast;



public class Savings extends AbstractRowReordering {
	public Savings() {}
	@Override
	public List<Row> solve(List<Row> table) {
		List<Row> answer = new Vector<Row>();// fast append
		MersenneTwisterFast rand = new MersenneTwisterFast();
		Row c0 = table.get(rand.nextInt(table.size()));// ad hoc choice
		table.remove(c0);
		Row currentlastelement = c0;//table.get(rand.nextInt(table.size()));
		//table.remove(currentlastelement);
		//answer.add(currentlastelement);
		while(table.size()>0) {
			currentlastelement=Savings.popNearestNeighbor(table,currentlastelement,c0);
			answer.add(currentlastelement);
		}
		if(c0.Hamming(currentlastelement)<= c0.Hamming(answer.get(0))) {
			answer.add(c0);
		} else {
			answer.add(0,c0);
		}
		return answer;
	}
	public static Row popNearestNeighbor(Collection<Row> table, final Row target, final Row c0) {
		assert(table.size()>1);
		Iterator<Row> i = table.iterator();
		Row nearest = i.next();
		int targetc0 = target.Hamming(c0);
		int LowestCost = target.Hamming(nearest) - targetc0 - c0.Hamming(nearest);
		while(i.hasNext()) {
			Row r = i.next();
			//System.out.println(target);
			int ccost = target.Hamming(r)- targetc0 - c0.Hamming(r);
			if(LowestCost>ccost) {
				LowestCost = ccost;
				nearest = r;
				//if(LowestCost <= acceptedlowerbound) break;// stop looking
			}
		}
		table.remove(nearest);
		return nearest;
	}
}
