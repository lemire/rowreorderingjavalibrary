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

/**
 * Classical Tour-construction heuristic.
 */

import java.util.List;
import java.util.Vector;

import util.MersenneTwisterFast;
import util.Util;


public class NearestNeighbor extends AbstractRowReordering {
	public NearestNeighbor() {}
	@Override
	public List<Row> solve(List<Row> table) {
		List<Row> answer = new Vector<Row>();// fast append
		MersenneTwisterFast rand = new MersenneTwisterFast();
		Row currentlastelement = table.get(rand.nextInt(table.size()));// ad hoc choice
		table.remove(currentlastelement);
		answer.add(currentlastelement);
		while(table.size()>0) {
			currentlastelement=Util.popNearestNeighbor(table,currentlastelement);
			answer.add(currentlastelement);
		}
		return answer;
	}
}
