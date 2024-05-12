#!/bin/bash

# Compile SecureChat.java
echo "Compiling SecureChat.java..."
javac SecureChat.java
echo "Compilation complete."

# Start SecureChat for Alice
echo "Starting SecureChat for Alice in a new window..."
gnome-terminal -- java SecureChat alice "$1"

# Start SecureChat for Bob in a new terminal window
echo "Starting SecureChat for Bob here..."
java SecureChat bob "$1"
