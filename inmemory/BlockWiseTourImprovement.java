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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/*
 * this basically composes two row reordering heuristic, but in such
 * a way that the second one is applied over blocks only (not the whole thing)
 */
public class BlockWiseTourImprovement extends AbstractRowReordering {
	public AbstractRowReordering mPrimary;
	public AbstractRowReordering mSecondary;
	public int mBlocksize;

	public BlockWiseTourImprovement(AbstractRowReordering primary, AbstractRowReordering secondary, int blocksize) {
		mPrimary = primary;
		mSecondary = secondary;
		mBlocksize = blocksize;
	}

	@Override
	public List<Row> solve(List<Row> table) {
		List<Row> firstanswer = mPrimary.solve(table);
		List<Row> secondanswer = new Vector<Row>(); 
		int pos = 0;
		final int numberofworkers = Runtime.getRuntime().availableProcessors();
		final ExecutorService es = Executors.newFixedThreadPool(numberofworkers);
		final Queue<Future<List<Row>>> q = new LinkedList<Future<List<Row>>>();
		while (pos < firstanswer.size()) {
			final List<Row> block = new Vector<Row>();
			int end = Math.min(pos+mBlocksize,firstanswer.size());
			block.addAll(firstanswer.subList(pos,end ));
			q.add(es.submit(new Callable<List<Row>>() {
				public List<Row> call() {
					return mSecondary.solve(block);
				}
			}));
			if(q.size()==numberofworkers) {
				Future<List<Row>> f = q.remove();
				try {
				  secondanswer.addAll(f.get());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			pos+=mBlocksize;
		}
		while(q.size()>0) {
			Future<List<Row>> f = q.remove();
			try {
			  secondanswer.addAll(f.get());
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
		es.shutdownNow();
		return secondanswer;
	}
	
	@Override
	public String toString() {
		return mPrimary.toString()+"+"+mSecondary.toString()+"("+mBlocksize+")";
	}
	

}
