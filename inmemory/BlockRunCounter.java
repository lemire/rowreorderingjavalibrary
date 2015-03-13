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

import gnu.trove.TIntArrayList;

/* The idea is to generalize the notion of "value" run by "block of values" runs.
 * For example, in base 2, this is a single run: ababab
 * 
 * 
 * The -m3 cost model assigns a cost of 1 to a run of length 1,
 * and a cost of 3 to every run of length 2 or more
 * based on <value><samevalue><counter>, ignoring counter range limits
 */
public class BlockRunCounter {
	TIntArrayList lastblock = new TIntArrayList();
	TIntArrayList currentblock = new TIntArrayList();
	final int mBlockSize;
	public int NumberOfRuns = 0;
	public int lengthoflastrun = 0;
	public final boolean m3model;// = false;
	Histogrammer<Integer> h = null;

	// public final boolean RunsMustBeSameValue;// by default abab is a single
	// run when using 2-blocks,
	// setting RunsMustBeSameValue to true forces us to counts abab as two runs,
	// even using the 2-block
	// because blocks contain different values.

	public BlockRunCounter(int blocksize) {
		this(blocksize, false, false);// ,false);
	}

	/*
	 * private boolean belongToSameRun() { if(!RunsMustBeSameValue) return
	 * lastblock.equals(currentblock); else return
	 * containsJustOneValue(lastblock) & lastblock.equals(currentblock); }
	 */

	public static boolean containsJustOneValue(TIntArrayList v) {
		int candidate = v.get(0);
		for (int k = 1; k < v.size(); ++k)
			if (v.get(k) != candidate)
				return false;
		return true;
	}

	public BlockRunCounter(int blocksize, boolean useHisto, boolean m3/*
																	 * , boolean
																	 * runsmustbesamevalue
																	 */) {
		mBlockSize = blocksize;
		m3model = m3;
		// RunsMustBeSameValue = runsmustbesamevalue;
		if (useHisto)
			h = new Histogrammer<Integer>();
	}

	public Histogrammer<Integer> getHistogram() {
		return h;
	}

	public void close() {
		if (currentblock.size() > 1) {
			++NumberOfRuns;
			if (h != null)
				h.incr(lengthoflastrun);
			lengthoflastrun = 0;
		}
	}

	public void newValue(int v) {
		if (lastblock.size() < mBlockSize) {
			lastblock.add(v);
			if (NumberOfRuns < 1) {
				NumberOfRuns = 1;
				lengthoflastrun = 1;
			}
		} else {
			currentblock.add(v);
			if (currentblock.size() == mBlockSize) {
				if (!lastblock.equals(currentblock)) {
					if ((m3model) && (lengthoflastrun > 1))
						NumberOfRuns += 3;
					else
						++NumberOfRuns;
					if (h != null)
						h.incr(lengthoflastrun);
					lengthoflastrun = 1;
					TIntArrayList tmp = lastblock;
					lastblock = currentblock;
					currentblock = tmp;
				} else {
					++lengthoflastrun;
				}
				currentblock.clear();
			}
		}
	}
}
