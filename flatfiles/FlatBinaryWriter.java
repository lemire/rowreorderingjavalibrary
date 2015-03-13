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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FlatBinaryWriter {
	//DataOutputStream dos;
	int columns;
	public static final int magic = 0x76;
	public static final int version = 1;
	RandomAccessFile raf;
	ByteBuffer bytebuf;
	FileChannel fc;
	
	
	public FlatBinaryWriter(String filename, int numberofcols) throws IOException {
		//dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
		raf = new RandomAccessFile(filename,"rw");
		raf.setLength(0);
		columns = numberofcols;
		raf.writeInt(magic);
		raf.writeInt(version);
		raf.writeInt(numberofcols);
		fc = raf.getChannel();
		bytebuf = ByteBuffer.allocateDirect(computePhysicalSizeOfRowsInBytes());
	}
	
	public void write(Row r) throws IOException {
		write(r.values);
	}

	public void write(int[] vals) throws IOException {
		bytebuf.clear();
		bytebuf.asIntBuffer().put(vals);
		bytebuf.rewind();
		fc.write(bytebuf);
	}
	
	public void close() throws IOException {
		raf.close();
	}

	
	public long getNumberOfRows() throws IOException {
		return  (raf.length() - FlatBinaryReader.HEADERSIZEINBYTES) /  computePhysicalSizeOfRowsInBytes();
	}

	public int computePhysicalSizeOfRowsInBytes() {
		return 4*columns;
	}
	public Row getRow(long i ) throws IOException {
		raf.seek(FlatBinaryReader.HEADERSIZEINBYTES + i* computePhysicalSizeOfRowsInBytes());
		bytebuf.clear();
		int howmanyread = fc.read(bytebuf);
		if(howmanyread == -1) throw new java.io.IOException();
		int[] val = new int[columns];
		bytebuf.rewind();
		bytebuf.asIntBuffer().get(val);
		raf.seek(raf.length());// reset the write position
		return new Row(val);
	}
	public void putRow(Row r, long i ) throws IOException {
		long pos = FlatBinaryReader.HEADERSIZEINBYTES + i* computePhysicalSizeOfRowsInBytes();
		if(pos > raf.length()) throw new IOException("Row "+i+" does not exist yet!");
		raf.seek(pos);
		//if(fc.position() != pos) throw new RuntimeException("channel not synced?");
		write(r);
		raf.seek(raf.length());// reset the write position
	}	
	
}
