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

// tries all reorderings that do not move the first or last rows in a block
// thus, when counting (all) runs, it can never degrade the (runcount) solution
// although it could mess up long runs.

// intended use: as a postprocessing step after some other approach (eg sorting)
// has already found a decent overall ordering.

// Blocksize has to be small.  13 or 15 is probably reasonable

public class BruteForcePeephole extends BruteForce {
    public BruteForcePeephole(){};
    public List<Row> solve(List<Row> table) {
        return solve(table,true);
    }

    public static void main( String [] argv) {
        ArrayList<Row> tbl = new ArrayList<Row>();
        tbl.add( new Row( new int [] {1,2,3}));
        tbl.add( new Row( new int [] {2,2,1}));
        tbl.add( new Row( new int [] {1,3,3}));
        tbl.add( new Row( new int [] {3,3,3}));

        BruteForce bf = new BruteForcePeephole();
        List<Row> solution = bf.solve(tbl);

        for (Row r : solution)
            System.out.println(""+r);

    }



}