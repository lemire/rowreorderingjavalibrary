package util;

import inmemory.Row;

import java.io.*;
import java.util.*;

import net.sf.csv4j.CSVReader;
import flatfiles.FlatBinaryReader;
import gnu.trove.*;

public class IOUtil {
   public static List<Row> loadAndNormalize(String csvfilename) throws IOException {
	   List<TObjectIntHashMap<String>> normalmaps = buildNormalMap(csvfilename);
	   CSVReader p = new CSVReader(new FileReader(csvfilename));
	   List<String> fields;
	   List<Row> answer = new Vector<Row>();
       while((fields = p.readLine()).size() != 0){
		   int[] values = new int[normalmaps.size()];
		   for(int k = 0; k < fields.size(); ++k) {
			   values[k] = normalmaps.get(k).get(fields.get(k));
		   }
		   answer.add(new Row(values));
	   }
	   p.close();
	   return answer;
   }
   

   public static TObjectIntHashMap<String> jointhistogram(String binfile, int[] whichcolumns) throws IOException {
	   TObjectIntHashMap<String> histogram = new TObjectIntHashMap<String>();
	   Iterator<Row> i = new FlatBinaryReader(binfile).iterator();
	   while(i.hasNext()) {
	   	Row r = i.next();
	   	int val1 = r.get(whichcolumns[0]);
	   	int val2 = r.get(whichcolumns[1]);
	   	String val = val1+"/"+val2;
	   	if(!histogram.containsKey( val ) ) {
	   		histogram.put(val,0);
	   		//System.out.println("not found "+p);
	   	} //else {System.out.println("already found");}
	   	histogram.put(val,histogram.get( val )+1);
	   }
	   return histogram;
   } 

   public static TIntArrayList[] histogram(String binfile, int[] whichcolums) throws IOException {
	   TIntArrayList[] histograms = new TIntArrayList[whichcolums.length];
	   for(int k = 0; k<histograms.length; ++k) histograms[k] = new TIntArrayList();
	   Iterator<Row> i = new FlatBinaryReader(binfile).iterator();
	   while(i.hasNext()) {
	   	Row r = i.next();
	   	for(int k = 0; k<histograms.length; ++k) {
	   		int value = r.get(whichcolums[k]);
	   		if(histograms[k].size()<value+1) {
	   			histograms[k].add(new int[value+1-histograms[k].size()]);
	   			histograms[k].set(value,1);
	   		} else {
	   			histograms[k].set(value,histograms[k].get(value)+1);
	   		}
	   	}
	   }
	   for(int k = 0; k<histograms.length; ++k) histograms[k].trimToSize();
	   return histograms;
   }   
   public static List<TObjectIntHashMap<String>> buildNormalMap(String csvfilename) throws IOException {
	   List<TObjectIntHashMap<String>> mapping = new Vector<TObjectIntHashMap<String>>();// = new TreeMap<String,Integer>();
	   CSVReader p = new CSVReader(new FileReader(csvfilename));
	   List<String> fields;
	   int linecounter = 0;
       if((fields = p.readLine()).size() != 0){
		   for(int k = 0; k < fields.size(); ++k) {
			   mapping.add( new TObjectIntHashMap<String>());
			   mapping.get(k).put(fields.get(k), 0);
		   }	   
		   ++linecounter;
       }
       while((fields = p.readLine()).size() != 0){
		   for(int k = 0; k < fields.size(); ++k) {
			   mapping.get(k).put(fields.get(k), 0);
		   }
		   ++linecounter;
	   }
       p.close();
       System.out.println("number of rows: "+linecounter);
       for( final TObjectIntHashMap<String> t: mapping) {
    	   System.out.println("column cardinality: "+t.size());
    	   int k = 0;
    	   t.forEachKey(new TObjectProcedure<String>() {
    		   int k = 0;
    		   public boolean execute(String arg) {
    			   t.put(arg, k++);
    			   return true;
    		   }});
	   }
	   return mapping;
   }
   
}
