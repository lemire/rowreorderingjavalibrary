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
import java.util.Comparator;
import java.util.List;

import flatfiles.Sorting;

public class FrequentComponent extends AbstractRowReordering {

	public List<Row> solve(List<Row> table)  {
		if(table.size()==0) return table;
		int c = table.get(0).size();
		Comparator<Row> cc = Sorting.getFrequencyComparator(table.iterator(), c);
		Collections.sort(table,cc);
		return table;
	}

}
