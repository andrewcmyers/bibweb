default: build

BIN=$(HOME)/bin

sinclude Makefile.local

jbibtex_build: jbibtex/pom.xml
	cd jbibtex && mvn compile

easyIO_build: easyIO/Makefile
	cd easyIO && $(MAKE)

$(SUBMODULES):
	git submodule update --init

jbibtex_build:

.PHONY: jbibtex_build easyIO_build

bibweb.jar:
	jar --create --file bibweb.jar -e bibweb.Main -C bin bibweb -C easyIO/bin easyIO -C jbibtex/target/classes org

build: jbibtex_build easyIO_build
	javac -d bin -sourcepath src -classpath easyIO/bin:jbibtex/target/classes:bin: src/bibweb/Main.java

install: bibweb.jar
	cp bibweb bibweb.jar $(BIN)
