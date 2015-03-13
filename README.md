# Row Reordering Java Library

Copyright 2009-2010 Daniel Lemire and Owen Kaser

# PURPOSE

This is a set of algorithms implemented in Java. The goal of these
algorithms is to minimize the number of column runs, to improve
run-length encoding compression in database tables.


# IS THIS FOR YOU?

This library is strictly for researchers with a working knowledge of
Java.

# REFERENCEs

Daniel Lemire and Owen Kaser, Reordering Columns for Smaller Indexes, Information Sciences 181 (12), 2011.
http://arxiv.org/abs/0909.1346

Daniel Lemire, Owen Kaser, Eduardo Gutarra, Reordering Rows for Better Compression: Beyond the Lexicographic Order, ACM Transactions on Database Systems 37 (3), 2012.
http://arxiv.org/abs/1207.2189


# USAGE

We expect CSV input files (comma-separated values). They must be
first transformed into a custom binary format with the flatfiles/CSVtoBinary
program. Then, for example, you can use the flatfiles/Sorting, flatfiles/Shuffle,
or flatfiles/MultipleLists programs to sort the table in different row orders. 


# LICENSE

Redistribution and use in source and binary forms, with or without modification, are
permitted provided that the following conditions are met:
 
    1. Redistributions of source code must retain the above copyright notice, this list of
       conditions and the following disclaimer.
 
    2. Redistributions in binary form must reproduce the above copyright notice, this list
       of conditions and the following disclaimer in the documentation and/or other materials
      provided with the distribution.