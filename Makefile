default: build

BIN=$(HOME)/bin

sinclude Makefile.local

bibweb.jar: build
	cd bin; jar cf ../bibweb.jar *

build:
	javac -d bin -sourcepath src -cp bin src/bibweb/Main.java

install: bibweb.jar
	cp bibweb bibweb.jar $(BIN)
