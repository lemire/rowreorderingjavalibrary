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

import java.util.List;

public class ComposedRowReordering extends AbstractRowReordering{
	AbstractRowReordering R1, R2;
	public ComposedRowReordering(AbstractRowReordering r1, AbstractRowReordering  r2) {
		R1= r1; R2=r2;
	}
	@Override
	public List<Row> solve(List<Row> table) {
		return R1.solve(R2.solve(table));
	}
	
	@Override
	public String toString() {
		return R1.toString()+" + "+R2.toString();
	}

}
