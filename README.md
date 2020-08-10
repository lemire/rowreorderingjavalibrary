# Row Reordering Java Library

![Java13-CI](https://github.com/lemire/rowreorderingjavalibrary/workflows/Java13-CI/badge.svg)

Copyright 2009-2010 Daniel Lemire and Owen Kaser

# Purpose

This is a set of algorithms implemented in Java. The goal of these
algorithms is to minimize the number of column runs, to improve
run-length encoding compression in database tables.


# IS THIS FOR YOU?

This code is meant to help you implementing the algorithms from our papers (see references below).

This library is strictly for researchers with a working knowledge of
Java. It is not for production use!!!!

You must read the papers: this is not standalone code. It is meant to complement the papers!

# References

Daniel Lemire and Owen Kaser, Reordering Columns for Smaller Indexes, Information Sciences 181 (12), 2011.
http://arxiv.org/abs/0909.1346

Daniel Lemire, Owen Kaser, Eduardo Gutarra, Reordering Rows for Better Compression: Beyond the Lexicographic Order, ACM Transactions on Database Systems 37 (3), 2012.
http://arxiv.org/abs/1207.2189


# Usage

We expect CSV input files (comma-separated values). They must be
first transformed into a custom binary format with the flatfiles/CSVtoBinary
program. Then, for example, you can use the flatfiles/Sorting, flatfiles/Shuffle,
or flatfiles/MultipleLists programs to sort the table in different row orders. 


With a recent system with Java and bash (Linux, macOS, Windows WSL, FreeBSD), it is as simple as...

```
$ ./compile.sh
$ java -cp .:lib/trove-2.1.0.jar:lib/csv4j-0.4.0.jar  flatfiles/CSVtoBinary file.csv file.bin
$ java  flatfiles/BinarytoCSV filesorted.bin file_normalized.csv
$ java -cp .:lib/jdbm-1.0.jar:lib/trove-2.1.0.jar  flatfiles/MultipleLists file.bin filesorted.bin
$ java  flatfiles/BinarytoCSV filesorted.bin filesorted_normalized.csv
```

# License

Redistribution and use in source and binary forms, with or without modification, are
permitted provided that the following conditions are met:
 
    1. Redistributions of source code must retain the above copyright notice, this list of
       conditions and the following disclaimer.
 
    2. Redistributions in binary form must reproduce the above copyright notice, this list
       of conditions and the following disclaimer in the documentation and/or other materials
      provided with the distribution.
