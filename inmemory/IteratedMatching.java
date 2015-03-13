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

/*** Implements the bottom-up, matching-based expensive EWAH heuristic,
 * adapted for general run minimization. 
 **/

import java.util.*;


public class IteratedMatching extends AbstractRowReordering {
    public IteratedMatching() {}
    @Override
	public List<Row> solve(List<Row> table) {
        Matching m = new Matching(table);
        
        // I would really like to know the end of the previous block....
        // suggest DL changes solve params
        // for now, use the first element.
        
        return m.matchReorder( table.get(0));
    }

}