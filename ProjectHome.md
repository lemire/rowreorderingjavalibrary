**PURPOSE**

This is a set of algorithms implemented in Java. The goal of these
algorithms is to minimize the number of column runs, to improve
run-length encoding compression in database tables.


**IS THIS FOR YOU?**

This library is strictly for researchers with a working knowledge of
Java who are interested in studying our implementation.

For a simple Java demo of the Vortex order described in one of our paper, please see https://github.com/lemire/SimpleVortex

**REFERENCES**

  * Daniel Lemire and Owen Kaser, Reordering Columns for Smaller Indexes  http://arxiv.org/abs/0909.1346
  * Daniel Lemire, Owen Kaser, Eduardo Gutarra, Reordering Rows for Better Compression: Beyond the Lexicographic Order, to appear ACM Transactions on Database Systems. http://arxiv.org/abs/1207.2189


**USAGE**

We expect CSV input files (comma-separated values). They must be
first transformed into a custom binary format with the flatfiles/CSVtoBinary
program. Then, for example, you can use the flatfiles/Sorting, flatfiles/Shuffle,
or flatfiles/MultipleLists programs to sort the table in different row orders.