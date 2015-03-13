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

import java.util.List;
import java.util.Vector;

import util.MersenneTwisterFast;

// this is greedy or multifragment from Bentley, 
public class MultiFragment extends AbstractRowReordering {
	public MultiFragment() {}
	@Override
	public List<Row> solve(List<Row> table) {
		//System.out.println("number of rows : "+table.size());
		List<List<Row>> fragments = new Vector<List<Row>>();
		fragments.add(new Vector<Row>());
		List<Row> answer = fragments.get(fragments.size()-1);
		MersenneTwisterFast rand = new MersenneTwisterFast();
		Row currentlastelement = table.get(rand.nextInt(table.size()));// ad hoc choice
		table.remove(currentlastelement);
		answer.add(currentlastelement);
		while(table.size()>0) {
			boolean foundmatch = false;
			for(int k = 0; k<table.size();++k)
				if(currentlastelement.Hamming(table.get(k)) == 0) {
					currentlastelement = table.remove(k);
					answer.add(currentlastelement);
					foundmatch = true;
					break;
				}
			if(!foundmatch) {
				//System.out.println("total fragments "+fragments.size());
				fragments.add(new Vector<Row>());
				answer = fragments.get(fragments.size()-1);
				currentlastelement = table.get(rand.nextInt(table.size()));
				table.remove(currentlastelement);
				answer.add(currentlastelement);
			}
		}
		//System.out.println("fragments (hamming 0, first pass)= "+fragments.size());
		int sum = 0;
		for(List<Row> l : fragments) sum+= l.size();
		//System.out.println("rows (hamming 0)= "+sum);
		// ok, at this point, my fragments all have nice Gray codes
		int cols = currentlastelement.size();
		for(int h = 0; h<=cols;++h) {
			merge(fragments,h);
			//System.out.println("fragments (hamming "+h+")= "+fragments.size());
			sum = 0;
			for(List<Row> l : fragments) sum+= l.size();
			//System.out.println("rows (hamming "+h+")= "+sum);
		}
		//System.out.println("number of fragments : "+fragments.size());
		//System.out.println("number of rows after : "+fragments.get(0).size());
		return fragments.get(0);
	}
	
	public static void merge(List<List<Row>> fragments, int hamming) {
		boolean didmerge = true;
		while((fragments.size()>1) && didmerge) {
			//System.out.println(fragments.size());
			didmerge = false;
			for(int k = 0;k<fragments.size(); ++k ) {
				List<Row> part1 = fragments.get(k);
				for(int k2 = k+1;k2<fragments.size(); ++k2 ) {
					List<Row> part2 = fragments.get(k2);
					if(part1.get(part1.size()-1).Hamming(part2.get(0))<=hamming) {
						part1.addAll(part2);
						fragments.remove(k2);
						didmerge = true;
					} else if (part2.get(part2.size()-1).Hamming(part1.get(0))<=hamming) {
						part2.addAll(part1);
						part1 = part2;
						fragments.set(k, part1);
						fragments.remove(k2);
						didmerge = true;
					}
				}
			}
		}
		
	}
}
