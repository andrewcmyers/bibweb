# bibweb

Formatted bibliography generator that can read from BibTeX input files but also overlays additional information
from an auxiliary input file and supports user-defined macros ala TeX.

To parse BibTeX, uses the jbibtex package (https://github.com/jbibtex/jbibtex).

For an example of (one possible) output, see http://www.cs.cornell.edu/andru/pubs.html

## Building bibweb

Use "make" in the top-level directory to build the source.
Use "make install" to build a JAR file that can be run using the script "bibweb".

## Running bibweb

Run with a bibweb script file as an argument, for example:

    ./bibweb simple.bibweb

Run with the `--help` option to see usage options and a summary of script syntax.

    ./bibweb --help

## Documentation

More documentation can be found at https://andrewcmyers.github.io/bibweb/

Written by Andrew Myers, June 2015.
