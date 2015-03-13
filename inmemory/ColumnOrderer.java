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
// provides a method to list columns by a number of criteria
// presumes a normalized table.

import java.util.*;

public class ColumnOrderer {
    public  enum ColOrder {IncreasingCardinality, DecreasingCardinality, Random};

    List<Row> table;
    int ncols;

    public ColumnOrderer(List<Row> tab) {
        table= tab;
        // assume nonempty table
        ncols = table.get(0).size();
    }

    public List<Integer> listBy( ColOrder order) {

        final int [] card = new int[ncols];

        // I think the table has already been normalized, so just do a "max" computation
        // cardinalities could be gotten via the Est class we use in another project
        
        // for orderings that need to know cardinalities, compute them
        if ((order == ColOrder.IncreasingCardinality) || (order == ColOrder.DecreasingCardinality) ){
            for (Row r : table)
                for (int i=0; i < r.values.length; ++i)
                    if (card[i] < r.values[i]) card[i] = r.values[i];
        } 
         
        // now, for every column we know its cardinality-1

        List<Integer> ans = new ArrayList<Integer>();
        for (int i=0; i < ncols; ++i) ans.add(i);

       // System.out.println("b4 sort by cardinality, cols");
        //for (int i : ans) System.out.println(i+": "+card[i]);

        if(order == ColOrder.IncreasingCardinality) {
           Collections.sort(ans, new Comparator<Integer>() {
                public int compare(Integer i1, Integer i2) {
                    return  ((Integer) card[i1]).compareTo( card[i2]);
                }
            });
        } else if (order == ColOrder.DecreasingCardinality) {
            Collections.sort(ans, new Comparator<Integer>() {
                public int compare(Integer i1, Integer i2) {
                    return  ((Integer) card[i2]).compareTo( card[i1]);
                }
            });        	
        } else if(order == ColOrder.Random) {
        	Collections.shuffle(ans);
        }

        //System.out.println("by cardinality, cols");
        //for (int i : ans) System.out.println(i+": "+card[i]);

        return ans;
    }
    
    public static String toString(ColOrder order) {
        if(order == ColOrder.IncreasingCardinality) {
        	return "IncreasingCardinality";
         } else if (order == ColOrder.DecreasingCardinality) {
        	 return "DecreasingCardinality";        	
         } else if(order == ColOrder.Random) {
         	return "Random";
         }
        return "unknown";
    }
}
