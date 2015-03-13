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

/*
 * Row is just a convenient wrapper for
 * rows in tables.
 * We will assume that values were normalized
 * to nonnegative integers.  The value -1 has
 * a special meaning for Bottom Up Matching
 * and should not be used by other code.
 */
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class Row implements Comparable<Row> {
  public static final int DIRTY = -1; 

  public Row() {
	  values = new int[0];
  }
  public Row(int l) {
	  values = new int[l];
  }
  public Row(int[] v) {
	  values = v;
  }
  
  // convenience method (yes, I know about clone)
  public Row copy() {
	  int[] newval = new int[values.length];
	  System.arraycopy(values, 0, newval, 0, newval.length);
	  return new Row(newval);
  }
   
    // the semantics of DIRTY is not captured by the current
    // Hamming and equal (btw, why not "equals"?) and matching
    // code should not use these methods

  public static int Hamming(int[] r1, int[] r2) {
	  int answer = 0;
	  for(int k=0; k<r1.length; ++k)
		  if(r1[k] != r2[k])
			  ++answer;
	  return answer;
  }
  public int Hamming(Row r) {
	  int answer = 0;
	  assert(values.length == r.values.length);
	  for(int k=0; k<values.length; ++k)
		  if(r.values[k] != values[k])
			  ++answer;
	  return answer;
  }
  public int Hamming(int[] r) {
	  int answer = 0;
	  assert(values.length == r.length);
	  for(int k=0; k<values.length; ++k)
		  if(r[k] != values[k])
			  ++answer;
	  return answer;
  }
  public boolean equals(Object r) {
	  if(r instanceof Row) {
		  //return values.equals(((Row)r).values);
		  return Arrays.equals(((Row)r).values,values);
	  }
	  return false;
  }
  
  @Override
public int hashCode() {
	  return values.hashCode();
  }
  
  public int size() { 
	  return values.length;
  }

  public int get(int i) { 
	  return values[i];
  }
  @Override
public String toString() {
	  StringBuffer ans= new StringBuffer();//"row ";
	  for(int k = 0; k<values.length-1;++k) {
		  ans.append(values[k]);ans.append(",");
	  }
	  ans.append(values[values.length-1]);
	  return ans.toString();
  }
  
  public int values[];

@Override
public int compareTo(Row o) {
	for(int k = 0; k< o.size(); ++k)
		if(get(k)<o.get(k)) return 1;
		else if(get(k)>o.get(k)) return 1;
	return 0;
}
}
