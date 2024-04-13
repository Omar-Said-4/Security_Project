@echo off
echo Compiling SecureChat.java...
javac SecureChat.java
echo Compilation complete.

echo Starting SecureChat for Alice...
start cmd /k java SecureChat alice

echo Starting SecureChat for Bob in a new window...
start cmd /k java SecureChat bob
