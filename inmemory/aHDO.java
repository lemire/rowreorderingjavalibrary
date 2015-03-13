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

import java.util.Collections;
import java.util.List;

// this is a heuristic described in Optimizing Frequency Queries for Data Mining Applications

public class aHDO extends AbstractRowReordering  {
	public aHDO() {}
	@Override
	public List<Row> solve(List<Row> table) {
		//int howmanyiters = (int) Math.ceil(Math.log(table.size())/Math.log(2));
		boolean keepgoing = true;
		while(keepgoing) {
			keepgoing=false;
			//for(int iters = 0; iters<howmanyiters;++iters) {
				for(int i = 1; i<table.size()-2;++i) {
					//int before = table.get(i-1).Hamming(table.get(i)) +table.get(i).Hamming(table.get(i+1)) +table.get(i+1).Hamming(table.get(i+2)) ;
					int currentcost = table.get(i-1).Hamming(table.get(i)) + table.get(i+1).Hamming(table.get(i+2));
					int flippedcost = table.get(i-1).Hamming(table.get(i+1)) + table.get(i).Hamming(table.get(i+2));
					if(flippedcost < currentcost) {
						Collections.reverse(table.subList(i, i+2));
						/*Row tmp = table.get(i);
						table.set(i, table.get(i+1));
						table.set(i+1, tmp);*/
						keepgoing = true;
					}
					//if(table.get(i-1).Hamming(table.get(i)) +table.get(i).Hamming(table.get(i+1)) +table.get(i+1).Hamming(table.get(i+2)) > before)
					//	throw new RuntimeException("bug");
				}
			//}
		}
		return table;
	}
}
