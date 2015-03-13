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

/* Owen has some complicated idea about using the Bottom-Up-Matching
 * heuristic (which, once upon a time, seemed to outperform early versions
 * of other bitmap heuristics.  It can be used with non-binary cases, but
 * it was designed for EWAH, not general run minimization (but can be fixed)
 */


/*** Internal tree nodes have degree 2.  They represent the grouping
 * of a bunch of Rows into a kind of "super-Row".  The Rows being
 * grouped correspond to the leaves that are descendants of the
 * internal node.  The super-Row itself has an aggregate Row value.
 * The ith component of the aggregate Row can take on the values found
 * in the ith components of ordinary Rows, plus a special "dirty"
 * value that means that the leaves do not agree on the value of the
 * ith component.
 *
 * Leaves in this tree correspond 1-1 with the Rows in the problem domain.
 ***/

public class RowTree {

    static final int verbosity = 3;

    static class RowTreeNode {

        RowTreeNode left, right, parent;
        Row aggregateValueRow;  // = regular row  for leaves 

        // constructor for leaves
        public RowTreeNode( Row r) {
            aggregateValueRow = r;
            left = right = parent = null; // default
        }

        // constructor for internal nodes
        public RowTreeNode( RowTreeNode left, RowTreeNode right) {
            this.left = left; this.right = right;
            parent = null;
            right.parent = left.parent = this;
            
            aggregateValueRow = 
                Matching.combineRows( left.aggregateValueRow,
                                      right.aggregateValueRow);
        }


        RowTreeNode leftmostLeaf() {
            RowTreeNode nn;
            for (nn = this; nn.left != null; nn = nn.left)
                continue;
            return nn;
        }


        RowTreeNode rightmostLeaf() {
            RowTreeNode nn;
            for (nn = this; nn.right != null; nn = nn.right)
                continue;
            return nn;
        }

        
        static private RowTreeNode bestSoFar;  // must be careful with multiple threads
        private static void findClosestMatch1(RowTreeNode n, Row target)
        { // look at all leaves
            if (n.left == null) { //leaf
                if (verbosity > 7)
                    System.out.println("avr = "+n.aggregateValueRow +" target="+target+" bestSoFar = "+bestSoFar);
                if (Matching.distance(n.aggregateValueRow,target) < 
                    Matching.distance(bestSoFar.aggregateValueRow,target))
                    bestSoFar = n;
            } else {
                findClosestMatch1( n.left,target);
                findClosestMatch1(n.right,target);
            }
        }

        RowTreeNode findClosestMatch( Row target)
        {
            synchronized(getClass()) {
                // set an arbitrary best-so-far
                bestSoFar = rightmostLeaf();
                if (verbosity > 7)
                    System.out.println("bsf = "+bestSoFar);
                findClosestMatch1(this,target);
                
                return bestSoFar;
            }
        }

        // initially applied to a leaf
        void makeChosenRightmost( RowTreeNode kid, RowTreeNode topmost){
            if (kid == topmost) return;
            RowTreeNode par = kid.parent;
            if (verbosity > 7)
                System.out.println("mcr kid = "+kid+" topmost="+topmost+" par="+par);

            if (kid == par.left) { // swap left and right
                RowTreeNode temp = par.left;
                par.left = par.right;
                par.right = temp;
            }
            //            if (par != topmost)
                makeChosenRightmost( par, topmost);
        }


        // tries to reduce seams. Odd that it works out to an inorder traversal?
        void fixup() {
            if (left == null) return; // no fixup for leaf

            right.fixup();  // work bottom up on a right spine
            
            RowTreeNode leftmostInRight = right.leftmostLeaf();
            // left's rightmost leaf needs to fit well with this one
            RowTreeNode bestMatch = left.findClosestMatch(leftmostInRight.aggregateValueRow);
            makeChosenRightmost(bestMatch, left);
            left.fixup();
        }

        
        // should be called just for root
        void fixupTopLevel( Row previousBlocksLast) {
            RowTreeNode bestMatch = findClosestMatch(previousBlocksLast);
            makeChosenRightmost(bestMatch,this);
            fixup();
        }
            

        /* not needed
        void computeParents( RowTreeNode n, RowTreeNode par) {
            if (n==null) return;
            n.parent = par;
            computeParents(n.left, n);
            computeParents(n.right, n);
        }
        */



        void sortedRows(List<Row> acc) {
            if (left == null) // leaf
                acc.add(aggregateValueRow);
            else {
                // oops, I want to list things first that are rightmost
                // in my tree setup.
                right.sortedRows(acc);
                left.sortedRows(acc);
            }
        }

        List<Row> sortedRows() {
            List<Row> acc = new ArrayList<Row>();
            sortedRows(acc);
            return acc;
        }

        void print(int depth) {
            for (int i=0; i < depth; ++i) System.out.print(" ");
            System.out.println(this+" "+aggregateValueRow);
            if (left != null) {
                left.print(depth+1);
                right.print(depth+1);
            }
        }


    }

    private RowTreeNode root;


    public RowTree( Row leafVal) {
        root = new RowTreeNode(leafVal);
    }

    public RowTree( RowTree child1, RowTree child2) {
        root = new RowTreeNode(child1.root,child2.root);
    }

    public List<Row> sortedRows() {
        return root.sortedRows();
    }


    public void fixup(Row prevRow) {
        root.fixupTopLevel(prevRow);
    }

    public Row lastRow() {
        return root.leftmostLeaf().aggregateValueRow;
    }

    public Row getRow() {
        return root.aggregateValueRow;
    }
    
    public void print() {
        root.print(0);
    }
    


}