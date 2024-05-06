# SecureChat Script

The `RunSecureChat.sh` and `RunSecureChat.bat` scripts compile and run the SecureChat.java program for two users: Alice and Bob.

## Usage

1. Compile SecureChat.java:

   - Ensure that SecureChat.java is compiled using the following command:
   - `javac SecureChat.java`

2. Run the SecureChat program for Alice and bob in separate terminal windows:
   - Execute the following command:
   - `./RunSecureChat.sh PORT` for Linux and `./RunSecureChat.bat PORT` for Windows
   - The scripts run the SecureChat using "alice" and "bob" arguments.
   - `java SecureChat alice PORT` and `java SecureChat bob PORT`

## Notes
- Make sure you have Java installed on your system.
- Pick a port that is not in use.
