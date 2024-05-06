#!/bin/bash

# Compile SecureChat.java
echo "Compiling SecureChat.java..."
javac SecureChat.java
echo "Compilation complete."

# Start SecureChat for Alice
echo "Starting SecureChat for Alice..."
java SecureChat alice &

# Start SecureChat for Bob in a new terminal window
echo "Starting SecureChat for Bob in a new window..."
java SecureChat bob &
