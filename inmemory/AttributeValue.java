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

import java.util.Comparator;

/**
 * Please keep this class light, it should only have two attribute values.
 * 
 * (Feel free to add methods though.)
 * @author lemire
 *
 */
public class AttributeValue {
		public int dim;
		public int value;
		public AttributeValue(int d, int v) {
			dim = d; value=v;
		}

		
		public static 	Comparator<AttributeValue> valuethendimcomp = new Comparator<AttributeValue>() {
			public int compare(AttributeValue o1, AttributeValue o2) {
				if(o1.value-o2.value !=0)
					return o1.value-o2.value;
				return o1.dim-o2.dim;
			}
		};
		public static 	Comparator<AttributeValue> dimthenvaluecomp = new Comparator<AttributeValue>() {
			public int compare(AttributeValue o1, AttributeValue o2) {
				if(o1.dim-o2.dim !=0)
					return o1.dim-o2.dim;
				return o1.value-o2.value;
			}
		};		
		@Override
		public boolean equals(Object av) {
			//if(!(av instanceof AttributeValue)) return false;
			AttributeValue avv = (AttributeValue) av;
			return (avv.value == value) && (avv.dim == dim); 
		}
		@Override
		public int hashCode() {
			// assume that dim<=32, then dim << 26 should be smaller than 1<<31.
			return value + (dim << 26);
		}
		
		public String toString() {
			return "dim : "+dim+" value : "+value;
		}
}
