import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.io.FileReader;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class SecureChat {

    private static BigInteger q; // DH and ElGamal prime modulus
    private static BigInteger alpha; // DH and ElGamal generator

    private static final int PORT = 7777; // Port number for the socket connection

    // Generate a private key within the range [1, q-2]
    private static BigInteger generatePrivateKey(BigInteger q) {
        SecureRandom random = new SecureRandom();
        BigInteger privateKey;
        do {
            privateKey = new BigInteger(q.bitLength() - 1, random);
        } while (privateKey.compareTo(BigInteger.ONE) <= 0 || privateKey.compareTo(q.subtract(BigInteger.TWO)) >= 0);
        return privateKey;
    }

    // Generate the corresponding public key
    private static BigInteger generatePublicKey(BigInteger privateKey, BigInteger alpha, BigInteger q) {
        return alpha.modPow(privateKey, q);
    }

    // Read q and alpha from a file
    private static void readParamsFromFile(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            q = new BigInteger(br.readLine().trim());
            alpha = new BigInteger(br.readLine().trim());
        }
    }

    // Perform SHA-1 hashing
    private static BigInteger sha1Hash(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger number = new BigInteger(1, messageDigest);
        return number;
    }

    // Select LSB such that 0 < m < q-1
    public static BigInteger selectLSBs(BigInteger m, BigInteger q) {
        BigInteger selected = BigInteger.ZERO;
        while (m.compareTo(BigInteger.ZERO) > 0 && m.compareTo(q.subtract(BigInteger.ONE)) < 0) {
            BigInteger lsb = m.and(BigInteger.ONE);
            selected = selected.shiftLeft(1).or(lsb);
            m = m.shiftRight(1);
        }

        return selected;
  }

    // Calculate the multiplicative inverse modulo (q-1)
    private static BigInteger modInverse(BigInteger a, BigInteger m) {
        return a.modInverse(m);
    }

    // Perform ElGamal signtaure
    private static BigInteger[] elGamalSign(BigInteger message, BigInteger privateKey, BigInteger alpha, BigInteger q) throws NoSuchAlgorithmException {
        BigInteger m = sha1Hash(message.toString()); // Hash the message
        m = selectLSBs(m, q); // Pick N LSB such that 0 < m < q-1

        SecureRandom random = new SecureRandom();
        BigInteger k; // Choose k such that 1 < k < q-1 and gcd(k, q-1) = 1
        do {
            k = new BigInteger(q.bitLength() - 1, random);
        } while (k.compareTo(BigInteger.ONE) <= 0 || k.compareTo(q.subtract(BigInteger.ONE)) >= 0 || !k.gcd(q.subtract(BigInteger.ONE)).equals(BigInteger.ONE));

        BigInteger s1 = alpha.modPow(k, q); // S1 = alpha^k mod q
        BigInteger s2 = k.modInverse(q.subtract(BigInteger.ONE)).multiply(m.subtract(privateKey.multiply(s1))).mod(q.subtract(BigInteger.ONE)); // S2 = k^-1 (m - Xa_2 * S1) mod (q-1)

        return new BigInteger[]{s1, s2}; // Return the signature (S1, S2)
    }

    // Verify ElGamal signature
    public static boolean verifySignature(BigInteger q, BigInteger alpha, BigInteger Ya, BigInteger message, BigInteger S1, BigInteger S2) throws NoSuchAlgorithmException {
        BigInteger m = sha1Hash(message.toString()); // Hash the message
        m = selectLSBs(m, q); // Pick N LSB such that 0 < m < q-1
        BigInteger V1 = alpha.modPow(m, q); // V1 = alpha^m mod q
        BigInteger V2 = Ya.modPow(S1, q).multiply(S1.modPow(S2, q)).mod(q); // V2 = (Ya^S1 * S1^S2) mod q

        return V1.equals(V2); // Signature is valid if V1 equals V2
    }

    // Derive AES key using SHA256
    public static byte[] deriveAESKey(BigInteger keyBigInt) throws NoSuchAlgorithmException {
        byte[] keyBytes = keyBigInt.toByteArray();
        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
        return sha256Digest.digest(keyBytes);
    }

    public static String AESEncrypt(String data, byte[] key) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String AESDecrypt(String encryptedData, byte[] key) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java SecureChat [role]");
            System.out.println("role: 'alice' or 'bob'");
            return;
        }

        String role = args[0].toLowerCase();
        try {
            // Read q and alpha from params.txt
            readParamsFromFile("params.txt");

            // Generate private and public keys for DH and ElGamal
            BigInteger X_DH = generatePrivateKey(q);
            BigInteger Y_DH = generatePublicKey(X_DH, alpha, q);

            BigInteger X_Elgamal = generatePrivateKey(q);
            BigInteger Y_Elgamal = generatePublicKey(X_Elgamal, alpha, q);
            
            BufferedReader input;
            PrintWriter output;
            Socket socket;
            byte[] AESKey;

            if (role.toLowerCase().equals("alice")) {
                // Start server socket to listen for Bob's connection
                ServerSocket serverSocket = new ServerSocket(PORT);
                System.out.println("Alice is listening on port " + PORT);

                // Accept connection from Bob
                socket = serverSocket.accept();
                System.out.println("Bob has connected!");

                // Set up input and output streams
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                // Send Alice's public key to Bob
                output.println(Y_Elgamal.toString());

                // Read Bob's public key
                BigInteger Y_received_Elgamal = new BigInteger(input.readLine().trim());

                BigInteger[] elGamalS = elGamalSign(Y_DH, X_Elgamal, alpha, q);

                output.println(Y_DH);
                output.println(elGamalS[0].toString().toString());
                output.println(elGamalS[1].toString().toString());

                BigInteger Y_received_DH = new BigInteger(input.readLine().trim());
                BigInteger S1_received_Elgamal = new BigInteger(input.readLine().trim());
                BigInteger S2_received_Elgamal = new BigInteger(input.readLine().trim());

                boolean isValidSignature = verifySignature(q, alpha, Y_received_Elgamal, Y_Elgamal, S1_received_Elgamal, S2_received_Elgamal);
                if (!isValidSignature) {
                    throw new Exception("Digital Signature is Invalid!");
                }
                BigInteger keyInt = Y_received_DH.modPow(X_DH, q);
                AESKey = deriveAESKey(keyInt);
            } else if (role.toLowerCase().equals("bob")) {
                // Connect to Alice's server
                socket = new Socket("localhost", PORT);
                System.out.println("Bob connected to Alice on port " + PORT);

                // Set up input and output streams
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                // Read Alice's public key
                BigInteger Y_received_Elgamal = new BigInteger(input.readLine().trim());

                // Send Bob's public key to Alice
                output.println(Y_Elgamal.toString());

                BigInteger Y_received_DH = new BigInteger(input.readLine().trim());
                BigInteger S1_received_Elgamal = new BigInteger(input.readLine().trim());
                BigInteger S2_received_Elgamal = new BigInteger(input.readLine().trim());

                boolean isValidSignature = verifySignature(q, alpha, Y_received_Elgamal, Y_Elgamal, S1_received_Elgamal, S2_received_Elgamal);
                if (!isValidSignature) {
                    throw new Exception("Digital Signature is Invalid!");
                }

                BigInteger[] elGamalS = elGamalSign(Y_DH, X_Elgamal, alpha, q);

                output.println(Y_DH);
                output.println(elGamalS[0].toString().toString());
                output.println(elGamalS[1].toString().toString());

                BigInteger keyInt = Y_received_DH.modPow(X_DH, q);
                System.out.println("Key is " + keyInt);
                AESKey = deriveAESKey(keyInt);
            } 
            else {
                throw new Exception("Invalid role. Use 'alice' or 'bob'.");
            }

            Scanner sc = new Scanner(System.in);

            Thread sender = new Thread(new Runnable(){
                String msg;
                @Override
                public void run(){
                    while(true){
                        msg = sc.nextLine();
                        try{
                            String encrypted = AESEncrypt(msg, AESKey);
                            output.println(encrypted);
                            output.flush();
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            });
            sender.start();

            Thread receiver = new Thread(new Runnable(){
                String msg;
                @Override
                public void run(){
                    try{
                        msg = input.readLine();
                        while(msg != null){
                            System.out.println("Received Message: " + AESDecrypt(msg, AESKey));
                            msg = input.readLine();
                        }
                        System.out.println("Bob is disconnected");
                        socket.close();
                    }
                    catch(IOException e){
                        e.printStackTrace();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            });
            receiver.start();
        } catch (IOException e) {
            System.err.println("Error in " + role + ": " + e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e){
            System.err.println(e);
        }
    }
}
