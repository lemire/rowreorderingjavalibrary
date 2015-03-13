package util;

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
@SuppressWarnings("unchecked")




// Until the Java library improves? Or is there one already?
public  class Pair<T extends Comparable<? super T>,U extends Comparable<? super U>> implements Comparable<Pair<T,U>> {
    public T fst;
    public U snd;
    public Pair(T a, U b) { fst = a; snd = b;}
    public String toString() { return "('" + fst + "','" + snd + "')";}
    public T first() {return fst;}
    public U second() {return snd;}
    public int hashCode() {
	return fst.hashCode() ^ snd.hashCode();}
    public boolean equals (  Object oo) { 
	if (oo instanceof Pair) { //un-pretty
	    Pair o = (Pair) oo;  
	    return fst.equals(o.fst) && 
		snd.equals(o.snd);
	} else return false;
    }
    
    public int compareTo(Pair<T,U> o) {
    	Pair<T,U> x = (Pair<T,U>) o;
    	int ans;
    	if((ans=fst.compareTo(x.fst))!=0)
    	  return ans;
    	return snd.compareTo(x.snd);
    }
}
    