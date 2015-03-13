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
 * Implementation of the standard random insertion tour-construction heuristic for TSP.
 */


import java.util.*;

import util.MersenneTwisterFast;
import util.Util;


public class RandomInsertion extends AbstractRowReordering {
	public RandomInsertion() {}
	@Override
	public List<Row> solve(List<Row> table) {
		List<Row> answer = new LinkedList<Row>();// for fast inserts
		MersenneTwisterFast rand = new MersenneTwisterFast();
		Row firstelement = table.get(rand.nextInt(table.size()));// ad hoc choice
		table.remove(firstelement);
		answer.add(firstelement);
		while(table.size()>0) {
			Row nextelement = table.get(rand.nextInt(table.size()));// ad hoc choice
			table.remove(nextelement);
			Util.insertCheaply(answer, nextelement);			
		}
		return answer;
	}
}
