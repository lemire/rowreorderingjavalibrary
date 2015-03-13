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

import gnu.trove.TObjectIntHashMap;

public class FreqWeigthedAttributeValue implements Comparable<FreqWeigthedAttributeValue>{
		public AttributeValue av;// = new AttributeValue();
		public int value;
		public int freq;
		public FreqWeigthedAttributeValue(int d, int v, TObjectIntHashMap<AttributeValue> map) {
			av = new AttributeValue(d,v);
			freq = map.get(av);
		}
		
		public String toString() {
			return " freq: "+freq+ " dim: "+av.dim+" value: "+av.value; 
		}
		
		

		
		/*public static 	Comparator<AttributeValue> valuethendimcomp = new Comparator<AttributeValue>() {
			public int compare(AttributeValue o1, AttributeValue o2) {
				if(o1.value-o2.value !=0)
					return o1.value-o2.value;
				return o1.dim-o2.dim;
			}
		};*/

		@Override
		public int compareTo(FreqWeigthedAttributeValue o) {
			//if(freq - o.freq != 0)
			if(freq < o.freq) return -1;
			else if(freq > o.freq) return 1;
			if(av.dim - o.av.dim != 0)
				return av.dim - o.av.dim;
			return av.value - o.av.value;
			/*if(av.value - o.av.value != 0)
				return av.value - o.av.value;
			return av.dim - o.av.dim;*/
		}
		
}

