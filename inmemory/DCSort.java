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

public class DCSort extends AbstractRowReordering {
	int BLOCKSIZE;
    boolean SetLeftoverFree;
    public DCSort(int blocksize, boolean leftoverfree) {
    	BLOCKSIZE=blocksize;
    	SetLeftoverFree = leftoverfree;
    }
    @Override
	public  List<Row> solve(List<Row> table) {
        DCWorker dcw = new DCWorker(table,BLOCKSIZE,SetLeftoverFree);
        return dcw.solve();
    }
    

	public String toString() { return this.getClass().getCanonicalName()+" "+BLOCKSIZE+" "+SetLeftoverFree;}


    public static void main( String [] argv) {
        ArrayList<Row> tbl = new ArrayList<Row>();
        // assumed to be normalized from 0
        tbl.add( new Row( new int [] {1,2,2}));
        tbl.add( new Row( new int [] {1,0,2}));
        tbl.add( new Row( new int [] {1,0,3}));
        tbl.add( new Row( new int [] {3,0,3}));
        tbl.add( new Row( new int [] {3,0,4}));
        tbl.add( new Row( new int [] {0,1,0}));
        tbl.add( new Row( new int [] {0,1,0}));
        tbl.add( new Row( new int [] {0,1,1}));
        tbl.add( new Row( new int [] {2,1,1}));
        tbl.add( new Row( new int [] {2,1,2}));
        tbl.add( new Row( new int [] {1,2,2}));
        tbl.add( new Row( new int [] {1,2,2}));

        DCSort dc = new DCSort(5,false);
        List<Row> solution = dc.solve(tbl);

        for (Row r : solution)
            System.out.println(""+r);

    }

}


// Row of data, and we also know the length of the run
// to which each item belongs
class RLRow  {
    public int [] v;  // values
    public int [] rl; // run lengths  (computed later)
    
    public RLRow(int [] v) {
	this.v = v;
	rl = new int[ v.length];
    }

    public RLRow( Row r) {
	this(r.values);
    }

    @Override
	public String toString() {
	String s = "Row: ";
	for (int i=0; i < v.length; ++i)
	    s += "\t("+v[i]+","+rl[i]+")";
	return s;
    }

}



class DCWorker {
	//final static public boolean verbose = false;
    ArrayList<RLRow> work;
    int nRows;
    int nCols;
    ColumnOrderer co;
    List<Integer> ascendingCols;
    //public static final int BLOCKSIZE = 5;
    int BLOCKSIZE;
    boolean SetLeftoverFree;

    public DCWorker( List<Row> table, int blocksize, boolean leftoverfree) {
    	SetLeftoverFree = leftoverfree; 
    	BLOCKSIZE = blocksize;
        work = new ArrayList<RLRow>(nRows);
        // put the list into an ArrayList
	for (Row r : table)
	    work.add(new RLRow(r));

        nRows = table.size();
	nCols = table.get(0).size();
        co = new ColumnOrderer(table);
        ascendingCols = co.listBy(ColumnOrderer.ColOrder.IncreasingCardinality);
    }



    // just use the precomputed column order 4 now
    int chooseNextCol( List<Integer> currentCols) {
	for (int i : ascendingCols)
	    if ( ! currentCols.contains(i)) return i;
	throw new RuntimeException("oops1");
    }

    void dumpWork() {
	System.out.println("\nDump");
	for (RLRow row : work)
	    System.out.println(row);
    }


    public List<Row> solve() {

	final List<Integer> currentCols = new ArrayList<Integer>();
	final List<Integer> leftovercolumns = new ArrayList<Integer>();
	leftovercolumns.addAll(ascendingCols);

	// iterate over columns
	while (currentCols.size() < nCols) {
	    int workCol = chooseNextCol(currentCols);
	    currentCols.add(workCol);
	    leftovercolumns.remove(new Integer(workCol));
	    
	    // sort
	    Collections.sort(work, new Comparator<RLRow>() {
		public int compare(RLRow r1, RLRow r2){
		    for (int col: currentCols) {
			if (r1.rl[col] < BLOCKSIZE &&
			    r2.rl[col] >= BLOCKSIZE) return -1;
			if (r1.rl[col] >= BLOCKSIZE &&
			    r2.rl[col] < BLOCKSIZE) return +1;
			// now, both short or long run members
                        if (r1.rl[col] > 0 && r1.rl[col] < BLOCKSIZE) continue;
                        // both are long runs (or this is the column we are working on, and we don't know)
			if (r1.v[col] <  r2.v[col]) return -1;
			if (r1.v[col] >  r2.v[col]) return +1;
		    }
		    for (int col: leftovercolumns) {
				if (r1.v[col] <  r2.v[col]) return -1;
				if (r1.v[col] >  r2.v[col]) return +1;	
		    }
		    return 0;
		}});

	    // run lengths in earlier columns are still ok
	    // for non-flagged values (except that, by removing
	    // some short runs that separated two long runs of the
	    // same  item, an even longer run might have occurred.
	    // since these items are still involved in a big run, 
	    // it's okay even if the bookkeeping does not have the
	    // run quite as big as it ought to be.

	    // set the run lengths in the new column

	    int runlength = 0;
	    // make fake predecessor
	    int prev = -1;

	    int rowCtr=0;
	    for (RLRow r : work)  {
		if (r.v[workCol] != prev) {
		    // we know length of the run that just ended, so
		    // update this info for each row involved
			if(!SetLeftoverFree) {// this is what Owen had
		      for (int j= 0; j < runlength; ++j)
			    work.get(rowCtr-1-j).rl[workCol] = runlength;
			} else { // this is Daniel's variation on it
				int cutoff = runlength/BLOCKSIZE * BLOCKSIZE;
				int startofrun = rowCtr - runlength;
			    for (int j= 0; j < cutoff; ++j)
			    	work.get(startofrun+j).rl[workCol] = runlength; 
			    	//work.get(rowCtr-1-j).rl[workCol] = runlength;
			    for (int j = cutoff; j <runlength; ++j)// we set the rest free!
			    	work.get(startofrun+j).rl[workCol] = 1;
			    	//work.get(rowCtr-1-j).rl[workCol] = 1;
			      
			}
		    runlength = 1;
		    prev = r.v[workCol];
		} else ++runlength;
		++rowCtr;
	    }

	    // handle the final run
	    for (int j=0; j < runlength; ++j)
		work.get(rowCtr-1-j).rl[workCol] = runlength;

	    // debugging
            // dumpWork();
	}
	
	// convert back to table
	List<Row> answer = new ArrayList<Row>( work.size());
	for (RLRow r : work)
	    answer.add (new Row( r.v));

	return answer;
    }
}
