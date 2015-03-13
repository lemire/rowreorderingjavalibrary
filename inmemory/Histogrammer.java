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

// builds a histogram (currently, positive ints)

public class Histogrammer<T extends Comparable<? extends T>> {

    TreeMap<T,Integer> ctrs;
    
    public Histogrammer() {
        ctrs = new TreeMap<T,Integer>();
    }
   
    // increment a count

    public void incr( T v) {
        if (ctrs.get(v) == null) ctrs.put(v,1);
        else ctrs.put( v, ctrs.get(v)+1);
    }
    
    public int sizeOfFrequentIceberg(int threshold) {
    	int counter = 0;
    	for(int i : ctrs.values())
    		if(i>=threshold) ++counter;
    	return counter;
    }

    // print
    
    public void dump() {
        System.out.println("Histogram");
        for (T k: ctrs.keySet()) 
            System.out.println("\t"+k+" \t"+ctrs.get(k));
    }

    public void dump(int minHistoSize) {
        dump();
        // count and print the number of cells in a run of minHistoSize or more
        int acc=0;
        for (T k: ctrs.keySet()) {
            // this is a bit horrid: dynamically handle integer keys
            Object o=k;
            if ((o instanceof Integer) && ((Integer) o) >= minHistoSize)
                acc +=  (Integer) o;
        }
        System.out.println("Total cells in long runs "+acc);
        
    }

}