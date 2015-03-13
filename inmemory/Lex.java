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

public class Lex extends AbstractRowReordering {
	ColumnOrderer.ColOrder mCO; 
    public Lex(ColumnOrderer.ColOrder co) {mCO=co;}
    @Override
	public  List<Row> solve(List<Row> table) {
        // final int nCols = table.get(0).values.length;
        ColumnOrderer co = new ColumnOrderer(table);
        final List<Integer> ascendingCols = co.listBy(mCO);//ColumnOrderer.ColOrder.IncreasingCardinality


        Collections.sort(table,	new Comparator<Row>() {
		public int compare(Row r1, Row r2){
		    for (int col : ascendingCols) {
			if (r1.get(col) < r2.get(col)) return -1;
                        if (r1.get(col) > r2.get(col)) return +1;
		    }
		    return 0;
		}});

        /*        System.out.println("!!!");
        for (Row r : table)
            System.out.println(""+r);
        */

        return table;
    }
    
	public String toString() { return this.getClass().getCanonicalName()+" "+ColumnOrderer.toString(mCO);}

}