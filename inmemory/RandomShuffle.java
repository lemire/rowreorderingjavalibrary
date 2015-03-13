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
 * Just a random shuffle
 * @author lemire
 *
 */

import java.util.*;


public class RandomShuffle extends AbstractRowReordering {
	public RandomShuffle() {}
	@Override
	public List<Row> solve(List<Row> table) {
		Collections.shuffle(table);
		return table;	
	}
}
