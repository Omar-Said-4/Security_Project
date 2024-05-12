@echo off
set port=%1
echo Compiling SecureChat.java...
javac SecureChat.java
echo Compilation complete.

echo Starting SecureChat for Alice...
start cmd /k java SecureChat alice %port%

echo Starting SecureChat for Bob in a new window...
start cmd /k java SecureChat bob %port%
