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
/* Class to handle matching, as used in the bottom-up-matching heuristics 
   for EWAH cost minimization
 Converted from Owen's Ruby code (bu-matching.rb), which has comment
# It requires the "Rothberg"
# implementation of the cubic-time general max matching algorithm
# (pointers available via Skiena's website).
# 
# Owen: to build the Rothberg code on x86_64 with recent gcc
# it is necessary to change the type of parameter "gptr" to
# void *  (from int) in readgraph.c.  There are other probably harmless
# warnings 

* Now being revised to also allow the matching implementation from Jicos.
* Rather than have some fancy class design to permit either, I will hack
* in either for now, then once Jicos is known to work well, remove the
* Rothberg stuff... -ofk 13 jan 2010
*
*/

import java.util.*;
import java.io.*;

import util.Pair;
import util.PropertyReader;
import edu.ucsb.cs.jicos.applications.utilities.graph.*;

public class Matching {

    public static final int verbosity = 0;
    public static String rothbProgram; 
    public static final int TOO_BIG_FOR_MATCHING = 1001;  // need to determine this experimentally
    public static final int BIG_ENOUGH_TO_TALK = 50;
    public static final String propFileName= "Matching.properties";
    boolean useJicos = true; 
    boolean noFixup = false;
  
    List<RowTree> forest;

    public Matching(List<Row> rows) {
        forest = new ArrayList<RowTree>();
        for (Row r : rows) 
            forest.add( new RowTree(r));
        Properties matchProp = null;
        try {
            matchProp = PropertyReader.read(propFileName);
        }
        catch (RuntimeException rw) {
        	// presumably, this means that the file cannot be found!
        	// we'll do the sensible thing and set some defaults
        	matchProp = new Properties();
        	matchProp.setProperty("useJicos", "true");
                matchProp.setProperty("noFixup","false");
        }
        catch (IOException ioe) {
            throw new RuntimeException("Problem reading " + propFileName);
        }

	if (matchProp.getProperty("useJicos") != null)
	    useJicos = Boolean.parseBoolean(matchProp.getProperty("useJicos"));

	if (matchProp.getProperty("noFixup") != null)
	    noFixup = Boolean.parseBoolean(matchProp.getProperty("noFixup"));

        if (noFixup)
            System.out.println("**** noFixup !!! ***");
	
	if (useJicos) return; // skip the rothberg check

        rothbProgram = matchProp.getProperty("rothProg");
        if (rothbProgram == null 
            || rothbProgram.equals("") 
            || ! (new File(rothbProgram)).exists()) {
            throw new RuntimeException("Need matching executable named in "+
                                       propFileName);}
    }
    public static int distance(Row r1, Row r2) {
        assert r1.values.length == r2.values.length;
        // like Row.Hamming except that it handles DIRTY

        int dist=0;
        for (int i = 0; i < r1.values.length; ++i)
            if (r1.values[i] == Row.DIRTY) ++dist;
            else if (r1.values[i] != r2.values[i]) ++dist;
        
        return dist;
    }

    public static Row combineRows( Row r1, Row r2) {
        assert r1.values.length == r2.values.length;
        int [] combo = new int[r1.values.length];
        for (int i=0; i < r1.values.length; ++i)
            if (r1.values[i] == r2.values[i]) combo[i] = r1.values[i];
            else combo[i] = Row.DIRTY;
        return new Row(combo);
    }

    // caller gets back matching as a bunch of index pairs, vertex indices start at 0 and match given list
    public List<Pair<Integer,Integer>> getMatching() {

        if (forest.size() >= TOO_BIG_FOR_MATCHING) 
            throw new RuntimeException("Matching on full adj matrix for " + 
                                       forest.size() + 
                                       " will probably take too long.");

        List<Row> vertices = new ArrayList<Row>();
        for (RowTree t : forest)
            vertices.add(t.getRow());


	if (useJicos) {

	    // go directly for a min-weight matching, which requires even
	    // number of nodes.

	    // add dummy vertex that is far from everyone.
	    boolean hasDummy = (vertices.size() % 2 == 1);
	    int n = vertices.size();
	    int paddedSize = hasDummy ? n+1 : n;

            // Jicos numbers vertices from 1 also
	    int [][] adjMatrix = new int [paddedSize][paddedSize];
	    
	    // javadoc seemed to hint that the adj matrix would ignore
	    // index 0, but I think maybe that index 0 is used to store
	    // info for the smallest numbered vertex (ie, 1)

	    for (int i = 1-1; i <= n-1; ++i)
		for (int j=1-1; j <= n-1; ++j)
                    // 1+  because jicos implementation does not work with
                    // cost 0 entries off the main diagonal
		    adjMatrix[i][j] = 1+distance( vertices.get(i),
                                                  vertices.get(j));

	    if (hasDummy)
		for (int i=0; i < paddedSize; ++i) 
		    adjMatrix[i][paddedSize-1] = adjMatrix[paddedSize-1][i]=
			Integer.MAX_VALUE/2;  // /2 so not close to wraparound

	    List<Pair<Integer,Integer>> answer = 
		new ArrayList<Pair<Integer,Integer>>();
	    
	    if (verbosity > 2 && forest.size() >=  BIG_ENOUGH_TO_TALK) 
		System.out.println("running Jicos matcher");

	    if (verbosity > 3) {
		// dump adj matrix
                System.out.println("Adj matrix:");
		for (int i=0; i < adjMatrix.length; ++i)  {
		    for (int j=0; j < adjMatrix.length; ++j)
			System.out.print(" "+adjMatrix[i][j]);
		    System.out.println();
		}
	    }
		
	    WeightedMatch wm = new WeightedMatch(adjMatrix);
	    int [] jicosMatching = wm.weightedMatch(WeightedMatch.MINIMIZE);

	    if (verbosity > 2 && forest.size() >=  BIG_ENOUGH_TO_TALK) 
		System.out.println("done Jicos matcher");

	    if (verbosity > 3) {
                System.out.println("Matching:");
		for (int ii : jicosMatching) 
		    System.out.print(" "+ii);
		System.out.println();
	    }

	    // okay so we know jicos returns an array that has a junk
	    // value at the end (experimentally)

	    // dummy is always largest vertex so if we only 
	    // list (large, small) pairs we won't output it

	    for (int i=1; i <= n; ++i)
		if (i > jicosMatching[i])
		    // must account for jicos counting from 1, but we from 0
		    answer.add( new Pair<Integer,Integer>(i-1, jicosMatching[i]-1));

            //            System.out.println("matching "+answer);
	    return answer;
	}
	else {
	    // Rothberg stuff, probably eventually to delete

	    try {
		if (verbosity > 2 && forest.size() >=  BIG_ENOUGH_TO_TALK) 
		    System.out.println("preparing rothberg input file for "+ forest.size() + " vertices.");
		File mfile = File.createTempFile("rothberg","in");
		PrintWriter pw = new PrintWriter( new FileOutputStream(mfile),true);
		int n = vertices.size();

		// determine max distance
		int maxwgt=0;
		for (Row v: vertices)
		    for (Row w: vertices) {
			int d = distance(v,w);
			if (d>maxwgt) maxwgt = d;
		    }
                    
		// output a complete adjacency matrix
		pw.println(n + " " + (n * (n-1))/2 + " U");
		int vctr = 1; // rothberg counts from 1
		for (Row v : vertices) {
		    pw.println(""+(n-1)+" v"+vctr+" 0 0");
		    int wctr = 1;
		    for (Row w : vertices) {
			if (vctr != wctr)
			    pw.println(""+wctr+" "+(1+maxwgt - distance(v,w)));
			++wctr;
                    
		    }
		    ++vctr;
		}

		pw.close();

		List<Pair<Integer,Integer>> answer = 
		    new ArrayList<Pair<Integer,Integer>>();

		if (verbosity > 2 && forest.size() >=  BIG_ENOUGH_TO_TALK) 
		    System.out.println("running "+ rothbProgram);

		Process rothProc = Runtime.getRuntime().exec(rothbProgram+" "+mfile);
		//BufferedReader in = new BufferedReader(rothProc.getInputStream());
		Scanner s = new Scanner(rothProc.getInputStream());
		while (s.hasNextInt()) {
		    int end1 = s.nextInt();
		    int end2 = s.nextInt();
		    if (end1 > end2) continue;  // get just one copy of each edge
		    --end1; --end2; // because Rothberg code numbers from 1 
		    answer.add(new Pair<Integer,Integer>(end1,end2));
		}

		if (verbosity > 2 && forest.size() >=  BIG_ENOUGH_TO_TALK) 
		    System.out.println("finished processing Rothberg output ");

                // System.out.println("matching "+answer);
		return answer;
	    } catch (IOException e) {
		System.err.println("error "+e);
		throw new RuntimeException("time to die");
	    }
	}
    }


    public static List<Integer> unmatchedVertices(List<Pair<Integer,Integer>> matching, int numVtx) {
        boolean [] seen = new boolean[numVtx];
        
        for (Pair<Integer,Integer> p : matching ) {
            if (seen[p.fst] || seen[p.snd])
                throw new RuntimeException("already seen member of edge "+p);
            seen[p.fst] = seen[p.snd] = true;
        }

        List<Integer> answer = new ArrayList<Integer>();
        for (int i=0; i < numVtx; ++i)
            if (!seen[i]) answer.add(i);

        if (answer.size() > 1) {
            System.err.println("hmmm, matching should have at most 1 unmatched");
        }
        return answer;
    }


    public void pairUp(List<Pair<Integer,Integer>> matching) {
        
        List<Integer> unmatched = unmatchedVertices(matching, forest.size());
        List<RowTree> newForest = new ArrayList<RowTree>();

        // form new superRow
        for (Pair<Integer,Integer> e : matching) {
            RowTree member1 = forest.get(e.fst);
            RowTree member2 = forest.get(e.snd);
            newForest.add( new RowTree( member1, member2));
        }

        // singleton guys go through unchanged
        for (int um : unmatched)
            newForest.add( forest.get(um));

        forest = newForest;

    }
    

    public int numSuperblocks() {
        return forest.size();
    }


    public List<Row>  matchReorder(Row precedingRow) {
        while (numSuperblocks() > 1)
            pairUp(getMatching());
       
        RowTree rt = forest.get(0); // the only one
        if (verbosity > 5) 
            rt.print();
        if (noFixup) {
            // this should be just for testing to see how much fixup helps
        } else {
            rt.fixup(precedingRow);
            if (verbosity > 5) {
                System.out.println("after");
                rt.print();
            }
        }
        return rt.sortedRows();
    }

    public static void main(String [] argv) {
        Row r1 = new Row( new int [] {1,1,2});
        Row r2 = new Row( new int [] {2,2,1});
        Row r3 = new Row( new int [] {1,2,2});
        
        List<Row> rs = new ArrayList<Row>();
        rs.add(r1); rs.add(r2); rs.add(r3);

        // test1
        Row fakePrev = new Row( new int [] {1,2,2});
        Matching m = new Matching(rs);
        List<Row> rearranged = m.matchReorder(fakePrev);
        for (Row r : rearranged) 
            System.out.println(""+r);

        
        //test2

        Row r4 = new Row( new int [] {3,1,2});
        Row r5 = new Row( new int [] {3,2,2});
        rs.add(r4); rs.add(r5);

        m = new Matching(rs);
        rearranged = m.matchReorder(fakePrev);
        for (Row r : rearranged) 
            System.out.println(""+r);

    }


}


