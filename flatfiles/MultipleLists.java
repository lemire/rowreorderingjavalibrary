package flatfiles;

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

import flatfiles.Sorting.Ordering;
import inmemory.Row;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import util.MersenneTwisterFast;
import util.Pair;
import jdbm.*;
import jdbm.btree.*;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.recman.*;

// this is the external memory version
public class MultipleLists {

	public static void main(String[] args) throws IOException {
		if(args.length<2) {
			System.out.println("please give my a binary input file and a binary output file");
			return;
		}
		//Random rand = new Random();
		String binaryfilename = args[0];
		//
		List<Comparator<int[]>> comps = getAllComparators(binaryfilename);
		Vector<BTree> lists = new Vector<BTree>();
		Vector<RecordManager> rmlists = new Vector<RecordManager>();
		for(Comparator<int[]> c : comps ) {
			Vector v = getTempBTree(c);
			BTree bt = (BTree) v.get(0);
			RecordManager rm = (RecordManager) v.get(1);
			rmlists.add(rm);
			lists.add(bt);
			loadData(bt,binaryfilename,rm);
		}
		// we initiate by picking a random row
		int[] r = pickRandomRow(binaryfilename).values;
		Integer number = new Integer(0);
		for(BTree bt : lists) {
			number = (Integer) bt.remove(r);
		}
		FlatBinaryWriter fbw = new FlatBinaryWriter(args[1],r.length);
		for(int k = 0; k<number.intValue();++k)
			fbw.write(r);
		// main work
		System.out.println("starting main work...");
		while(lists.get(0).size()>0) {
			Tuple t = popNearestNeighbor(r,  lists, rmlists);
			r= (int[]) t.getKey();
			number = (Integer) t.getValue();
			for(int k = 0; k<number.intValue();++k)
				fbw.write(r);
		}
		System.out.println("starting main work... done!");
		fbw.close();
	}
	
	public static Tuple popNearestNeighbor(int[] target, Vector<BTree> lists, Vector<RecordManager> rmlists) throws IOException {
		Tuple answer = null;
		int besthamming = Integer.MAX_VALUE;
		int whichbtree = -1;
		for(int k = 0; k<lists.size(); ++k) {
			BTree bt = lists.get(k);
		//for(BTree bt : lists) {
			TupleBrowser tb = bt.browse(target);
			Tuple candidate1 = new Tuple();
			Tuple candidate2 = new Tuple();
			boolean c1 = tb.getPrevious(candidate1);
			boolean c2 = tb.getNext(candidate2);
			if(c1) {
				int h1 = Row.Hamming(target,(int[])candidate1.getKey());
				if(h1<=besthamming) {
					besthamming = h1;
					answer = candidate1;
					whichbtree = k;
				}
			}
			if(c2) {
				int h2 = Row.Hamming(target,(int[])candidate2.getKey());
				if(h2<=besthamming) {
					besthamming = h2;
					answer = candidate2;
					whichbtree = k;
				}
			}			
		}
		for(BTree bt : lists) {
			int[] row = (int[]) answer.getKey();
			bt.remove(row);
		}
		for(RecordManager rm : rmlists) {
			rm.commit();
		}
		if(true) {// goal here is to encourage long runs
		  // we reorder the btree so that the selected one comes on last which makes it more likely to get picked again
		  BTree guy = lists.remove(whichbtree);
		  lists.add(guy);
		  RecordManager rmguy = rmlists.remove(whichbtree);
		  rmlists.add(rmguy);
		}
		return answer;
	}
	
	public static Row pickRandomRow(String binaryfilename) throws IOException {
		MersenneTwisterFast rand = new MersenneTwisterFast();
		FlatBinaryReader fbr = new FlatBinaryReader(binaryfilename);
		fbr.getNumberOfRows();
		fbr.goToRow(rand.nextLong(fbr.getNumberOfRows()));
		Row r = fbr.read();
		fbr.close();
		return r;
	}
	
	public static void loadData(BTree bt, String binaryfilename, RecordManager rm) throws IOException {
		System.out.println("creating one b-tree");
		FlatBinaryReader fbr = new FlatBinaryReader(binaryfilename);
		Iterator<Row> i = fbr.iterator();
		int t = 0;
		while(i.hasNext()) {
			Row r = i.next();
			if(((++t) % 1000) == 0) rm.commit();
			Object v = bt.find(r.values);
			if(v == null) {// not already there
			  bt.insert(r.values,new Integer(1),true);
			} else { // otherwise, it is an Integer
			  Integer x = (Integer) v;
			  bt.insert(r.values,new Integer(x.intValue()+1),true);
			}
		}
		rm.commit();
		fbr.close();
		System.out.println("done with the b-tree");
	}
	
	public static Vector getTempBTree(Comparator<int[]> c) throws IOException {
		File f = File.createTempFile("multiplelists", "");
		f.deleteOnExit();// assuming this file is created
		BaseRecordManager rm = new BaseRecordManager(f.getCanonicalPath());
		//rm.getTransactionManager().setMaximumTransactionsInLog(64);
		rm.disableTransactions();
		File dbfile = new File(f.getCanonicalPath()+".db");
		dbfile.deleteOnExit();
		File logfile = new File(f.getCanonicalPath()+".lg");
		logfile.deleteOnExit();
		BTree b = BTree.createInstance(rm, c);
		Vector ans = new Vector();
		ans.add(b);
		ans.add(rm);
		return ans;
		//return new Pair<BTree,RecordManager>(b,rm);
	}
	
	public static List<Comparator<int[]>> getAllComparators(String binaryfilename) throws IOException {
		int[] map =  Sorting.getColumnReordered(binaryfilename,true,false);
		Vector<Comparator<int[]>> comps = new Vector<Comparator<int[]>>();
		comps.add(new SerializableComparator(map));
		for(int k = 0; k< map.length-1;++k) {
			map = inmemory.MultipleLists.rotatedMapping(map);
			comps.add(new SerializableComparator(map));
		}
		return comps;
	}
}


class SerializableComparator implements Comparator<int[]>, Serializable {
	public final int[] mapping;
	public SerializableComparator(int[] m) {
		mapping = m;
	}
	public int compare(int[] r1, int[] r2){
		for(int k = 0; k<mapping.length;++k)
			if(r1[mapping[k]]<r2[mapping[k]])
				return -1;
			else if (r1[mapping[k]]>r2[mapping[k]])
				return 1;
		return 0;}
}
