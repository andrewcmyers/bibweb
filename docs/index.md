# Bibweb

Bibweb is a utility for generating HTML bibliographies from BibTeX source files.
What it adds over similar tools is the ability to overlay BibTeX entries with
additional information. It is quite flexible because it uses TeX-like macros to
define all generated output.

## Usage

To run bibweb, it is given a bibweb script as an argument:

    bibweb example.bibweb

A minimal script that generates output from a BibTeX database looks like the
following:

```
pubs: input.bib
generate {
    output: output.html
    section:
}
```

A script may read input from multiple BibTeX databases, and may add additional
information to entries it has read. If a database is read using the `pubs`
command, its entries are automatically selected for inclusion in generated
output. The `bibfile` command can be used to read entries with selecting them.

A short summary of available commands is readily available:

    bibweb --help

## Syntax

The script is a sequence of attributes, some of which are recognized as commands.
An attrbute can be declared on a single line:

```
bibfile: input.bib
```
or on multiple lines, delimited by braces.

```
bibfile {
    input.bib
}
```
Most attributes work with either syntax.
