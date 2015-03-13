package inmemory;

/* Just looks at all permutations of some rows */

/* Could try to do some pruning, not done yet */

import java.util.*;
import util.*;

public class BruteForce extends AbstractRowReordering {
    // it appears all threads share this object.  Pain: so create BFWorker.
    public BruteForce() {}
    @Override
	public  List<Row> solve(List<Row> table) {
        return solve(table,false);
    }

    public List<Row> solve(List<Row> table, boolean keepEndpoints) {
        BFWorker bfw = new BFWorker(table);
        return bfw.solve(keepEndpoints);
    }


    public static void main( String [] argv) {
        ArrayList<Row> tbl = new ArrayList<Row>();
        tbl.add( new Row( new int [] {1,2,3}));
        tbl.add( new Row( new int [] {2,2,1}));
        tbl.add( new Row( new int [] {1,2,2}));
        tbl.add( new Row( new int [] {3,3,3}));

        BruteForce bf = new BruteForce();
        List<Row> solution = bf.solve(tbl);

        for (Row r : solution)
            System.out.println(""+r);

    }

}


// one object per call to solve, no threads sharing this object

class BFWorker {
    ArrayList<Row> al;
    int nrows;
    List<Row> bestSoln;
    int bestCost;

    public BFWorker( List<Row> table) {
        al = new ArrayList<Row>(nrows);
        // put the list into an ArrayList to guarantee fast random access
        al.addAll(table);
        nrows = table.size();
        if (nrows > 13)
            throw new RuntimeException("Brute force infeasible for "+nrows);
        if (nrows < 1)
            throw new RuntimeException("Block size was zero??");
    }



    // the Boolean parameter is a bit nasty.  It would be better to
    // make a general "all permutations of a subrange" and use it
    // over 0..max-1 [corresponding to "false"], or use it over
    // 1..max-2 [corresponding to "true"], with 0 prepended and max-1 appended

    // as is, I have hacked it until it seemed to work :)
    // however, I am not sure I trust it. -ofk

    public List<Row> solve(boolean keepEndpoints) {
        bestCost = Integer.MAX_VALUE;
        bestSoln = null;
        boolean [] unused = new boolean[nrows];
        for (int i=0; i < nrows; ++i) unused[i] = true;

        if (! keepEndpoints)
            go( new int [nrows/*-1*/], 0, unused, nrows-1);
        else {
            //System.out.println("solving holding endpoints");
            // we insist upon not permuting the first and last tuples
            // make sure we handle the boundary case of block size 1 or 2 properly
            if (nrows < 3) return al;
            
            // else, for 3 or more, we will evaluate at least one permutation 
            unused[0] = unused[nrows-1] = false;
            int [] a = new int [nrows];
            a[0]=0; a[nrows-1] = nrows-1;
            go( a, 1, unused, nrows-2);
            assert bestCost < Integer.MAX_VALUE;  // should have evaluated at least one permutation
        }
   
        return bestSoln;
    }


    void go( final int []  a, final int asize, final boolean [] whatsLeft, final int stopAt) {
        if (asize == stopAt) {
            // evaluate ordering according to some criterion (eg min num runs)
            // only min num runs for now

            // iterate according to permutation.
            int quality= Util.numberOfRuns( new Iterator<Row>(){
                    int pos = 0;

                    public void remove() { throw new UnsupportedOperationException();}
                    public boolean hasNext() { return pos <= nrows-1;}
                    public Row next() {
                        Row ans=null;
                        if (pos == stopAt) {
                            // see what is left, for the last chosen one
                            int left= -1;
                            for (int i=0; i < nrows; ++i)
                                if (whatsLeft[i]) {
                                    left = i; break;  // there's only one left
                                }
                            ans = al.get( left);

                        }
                        else ans = al.get(a[pos]);
                        ++pos;
                        return ans;
                    }
                });

            //System.out.print("trial ");
            //for (int i : a) System.out.print(i+" ");
            //System.out.println();

            if (quality < bestCost) {
                bestCost = quality;
                //System.out.println("better quality"+quality);
                bestSoln = new ArrayList<Row>();
                // see what is left (copy paste sin)
                int left= -1;
                for (int i=0; i < nrows; ++i)
                    if (whatsLeft[i]) {
                        left = i; break;
                    }
                
                for (int i = 0; i < nrows; ++i)  
                    if (i == stopAt)
                        bestSoln.add(al.get(left));
                    else
                        bestSoln.add( al.get(a[i]));

            }
        } 
        else {
            for ( int i = 0; i < nrows; ++i) {
                if (whatsLeft[i]) {
                    whatsLeft[i]=false;
                    a[asize]=i;
                    go(a,asize+1,whatsLeft,stopAt);
                    whatsLeft[i]=true;
                }
            }
        }
    }
}
