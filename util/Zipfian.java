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

import flatfiles.FlatBinaryWriter;
import inmemory.Row;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Zipfian {
	
	double[] probs;
	private MersenneTwisterFast rand;
	double sum;
	public Zipfian(int distinctvalues) {
		this(distinctvalues,-1, System.currentTimeMillis());
	}
	public Zipfian(int distinctvalues, double exponent,long seed) {
		rand = new MersenneTwisterFast(seed);
		probs = new double[distinctvalues];
		for (int i = 0; i < probs.length; i++) {
			probs[i] = Math.pow(i+1, exponent); 
		}
		sum = 0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
		}
	}
	
	public int nextInt() {
		double r = sum * rand.nextDouble();
		int i;
		for(i = 0; i <probs.length; ++i) {
			r -= probs[i];
			if(r<0) break;
		}
		return i;
	}
	
	public static void main(String args[]) throws IOException {
		// first parameter is number of columns, second one is number of rows
		if(args.length!=3) {
			System.out.println("Please specific the output (binary) file, the number of columns and the number of rows");
		}
		int c = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		Vector<Row> answer = generateZipfianTable(c,n);
		FlatBinaryWriter fbw = new FlatBinaryWriter(args[0],c);
		for (Row r : answer)
			fbw.write(r);
		fbw.close();
	}
	
	public static Vector<Row> generateZipfianTable(int c, int n) {
		int[] distinctvalues = new int[c];
		for(int k = 0; k<distinctvalues.length; ++k) distinctvalues[k] =n;
		return generateZipfianTable(distinctvalues, n);
	}
	public static Vector<Row> generateZipfianTable(int c, int n, int N) {
		int[] distinctvalues = new int[c];
		for(int k = 0; k<distinctvalues.length; ++k) distinctvalues[k] =N;
		return generateZipfianTable(distinctvalues, n);
	}
	public static Vector<Row> generateZipfianTable(int c, int n, int N, double exponent) {
		int[] distinctvalues = new int[c];
		for(int k = 0; k<distinctvalues.length; ++k) distinctvalues[k] =N;
		return generateZipfianTable(distinctvalues, n,exponent);
	}	
	public static Vector<Row> generateZipfianTable(int[] distinctvalues, int n) {
		return generateZipfianTable( distinctvalues,  n, -1);
	}
	public static Vector<Row> generateZipfianTable(int[] distinctvalues, int n, double exponent) {
		Zipfian[] zipf = new Zipfian[distinctvalues.length];
		int x = (int) System.currentTimeMillis();
		for(int k = 0; k<distinctvalues.length; ++k ) zipf[k] = new Zipfian(distinctvalues[k],exponent,k*x);
		Vector<Row> answer = new Vector<Row>();
		for(int r =0; r<n;++r) {
			int[] vals = new int[distinctvalues.length];
			for(int c = 0; c<vals.length;++c) vals[c] = zipf[c].nextInt();
			//System.out.println(new Row(vals));
			answer.add(new Row(vals));
		}
		return answer;
	}
	

}