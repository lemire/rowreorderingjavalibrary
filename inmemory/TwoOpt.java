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
import java.util.*;


/*
 * This is the classical 2-opt heuristic. 
 * I tried to follow the  implementation which pretends to run in quadratic time according to Johnson, but I guess I didn't in the end.
 * http://www.research.att.com/~dsj/papers/TSPchapter.pdf
 * 
 * 2-opt is from G. A. CROES (1958). A method for solving traveling salesman problems. Operations Res. 6 (1958) , pp., 791-812..
 */
public class TwoOpt extends AbstractRowReordering {
	public TwoOpt() {
	}

	@Override
	public List<Row> solve(List<Row> table) {
		List<Row> answer = table;
		table = null;
		boolean canimprove = true;
		int n = answer.size();
		if(n <=2 ) return answer;
		// of course, I could loop through this many times, but if one time is not enough...
		// assuming that i<j is enough to visit all pairs
		for(int i = 0; i<n ; ++i) {
				for(int j = i+3; j<n;++j) {
					int oldcost = answer.get(i).Hamming(answer.get(i+1))+ answer.get(j).Hamming(answer.get(j-1));
					int newcost = answer.get(i).Hamming(answer.get(j-1))+ answer.get(i+1).Hamming(answer.get(j));
					if(newcost<oldcost) {
						// must flip 
					    List<Row> l2 = answer.subList(i+1, j);
						Collections.reverse(l2);
						// of course, now we have changed the data we were looping over...
					}
				}
		}
		return answer;
	}
}
