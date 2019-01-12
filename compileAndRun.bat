@echo off
rem cd to source code dir
cd src
rem use utf-8 encoding, and output classes to outer bin dir
javac -encoding UTF-8 *.java -d ../bin
rem cd to bin dir
cd ../bin
rem wrap all contents to java archive and language file
jar cvfe ../FinalTerm.jar FinalTerm * ../src/lang.xml ../src/template.xml
rem back to root dir
cd ../
rem execute java archive
java -jar FinalTerm.jar