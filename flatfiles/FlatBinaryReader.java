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

import inmemory.Row;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.Iterator;

public class FlatBinaryReader {
	//DataInputStream dis;
	File myfile;
	public FileChannel fc;
	//IntBuffer rowbuf;
	ByteBuffer bytebuf;
	RandomAccessFile raf;
	public int columns;
	public static final int HEADERSIZEINBYTES = 3*4;
	
	public  FlatBinaryReader(String filename) throws IOException {
		myfile = new File(filename);
		raf = new RandomAccessFile(myfile,"r");
				//DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(myfile)));
		if(raf.readInt()!= FlatBinaryWriter.magic)
			throw new IllegalArgumentException("bad magic number with binary file "+filename);
		if(raf.readInt()!= FlatBinaryWriter.version)
			throw new IllegalArgumentException("bad version number with binary file "+filename);				
		columns = raf.readInt();
		//close();
		fc = raf.getChannel();
		bytebuf = ByteBuffer.allocateDirect(computePhysicalSizeOfRowsInBytes());
		//rowbuf = bytebuf.asIntBuffer();
	}
	
	public int computePhysicalSizeOfRowsInBytes() {
		return 4*columns;
	}

	public static int computePhysicalSizeOfRowsInBytes(String filename) throws IOException {
		File myfile = new File(filename);
		DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(myfile)));
		int columns;
		try{
			if(dis.readInt()!= FlatBinaryWriter.magic)
				throw new IllegalArgumentException("bad magic number with binary file "+filename);
			if(dis.readInt()!= FlatBinaryWriter.version)
				throw new IllegalArgumentException("bad version number with binary file "+filename);				
			columns = dis.readInt();
		} finally {
			dis.close();
		}
		return 4*columns;
	}	
	
	public void reset()  throws IOException  {
		fc.position(HEADERSIZEINBYTES);
		//dis =  new DataInputStream(new BufferedInputStream(new FileInputStream(myfile)));
		//dis.skip(HEADERSIZEINBYTES);
	}
	
	
	public long getNumberOfRows() {
		long size = myfile.length();
		size -= HEADERSIZEINBYTES; // headersize
		return size / computePhysicalSizeOfRowsInBytes();
	}
	
	public static long getNumberOfRows(String filename) throws IOException {
		long size = new File(filename).length();
		size -= HEADERSIZEINBYTES; // headersize
		return size / computePhysicalSizeOfRowsInBytes(filename);
		
	}
	
	public Iterator<Row> iterator() {
		return new FlatBinaryReaderIterator(this);
	}
	

	public Row read()  throws IOException {
		bytebuf.clear();
		int howmanyread = fc.read(bytebuf);
		if(howmanyread == -1) throw new java.io.EOFException();
		int[] val = new int[columns];
		bytebuf.rewind();
		bytebuf.asIntBuffer().get(val);
		return new Row(val);
	}
	public void readInto(Row r)  throws IOException {
		bytebuf.clear();
		int howmanyread = fc.read(bytebuf);
		if(howmanyread == -1) throw new java.io.EOFException();
		int[] val = new int[columns];
		bytebuf.rewind();
		bytebuf.asIntBuffer().get(val);
		r.values = val;
	}
	
	public void skipRows(int numberofrows)  throws IOException {
		long newpos = fc.position()+numberofrows * computePhysicalSizeOfRowsInBytes();
		fc.position(newpos);
		if(fc.position()!=newpos) throw new IOException("could not skip rows");
		//return dis.skipBytes(numberofrows * computePhysicalSizeOfRowsInBytes()) / computePhysicalSizeOfRowsInBytes();
	}
	
	public void goToRow(long index) throws IOException {
		fc.position(HEADERSIZEINBYTES+index*computePhysicalSizeOfRowsInBytes());
		if(fc.position()!=HEADERSIZEINBYTES+index*computePhysicalSizeOfRowsInBytes())
			throw new IOException("couldn't move at row "+index);
	}
	
	public void close()  throws IOException {
		raf.close();
		//fc.close();
//		dis.close();
	}
	
	public static int[] scanformax(String file) throws IOException {
		FlatBinaryReader fbr = new FlatBinaryReader(file);
		Iterator<Row> i = fbr.iterator();
		int[] ans = new int[fbr.columns];
		while(i.hasNext()) {
			Row r = i.next();
			for(int k = 0; k<r.size();++k) 
				ans[k] = ans[k] <r.get(k) ? r.get(k) : ans[k];
		}
		return ans;
	}

}

class FlatBinaryReaderIterator implements Iterator<Row> {

	Row nextrow;
	FlatBinaryReader parent;
	public FlatBinaryReaderIterator(FlatBinaryReader  p) {
		parent = p;
		try {
			nextrow = parent.read();
		} catch(IOException ioe) {
			try {parent.close();} catch(IOException ioe2) {}
			nextrow = null;
		}
	}
	public boolean hasNext() { 
		return nextrow != null;
	}

	public Row next() {
		Row oldrow = nextrow;
		try {
			nextrow = parent.read();
		} catch(IOException ioe) {
			try {parent.close();} catch(IOException ioe2) {}
			nextrow = null;
		}
		return oldrow;
	}

	public void remove() {
		throw new RuntimeException("not implemented");
	}

}