package h2hbankgas.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.util.io.Streams;

public class DecryptFileOpenPGP {

    public static void AppdecryptFile(String encryptedFilePath, String decryptFilePath, String passwordpublickey, String pgpPrivateKey) throws IOException {
        //System.out.println("Inside AppdecryptFile method");

        final char[] passwd = passwordpublickey.toCharArray();

        try {
         //   System.out.println("Decrypting file: " + encryptedFilePath);
            final File decryptFile = new File(decryptFilePath);

            // Open streams for encrypted file and private key
            InputStream dataIn = new FileInputStream(encryptedFilePath);
            InputStream keyIn = new FileInputStream(pgpPrivateKey);

            boolean success = false;
            try {
                success = ActiondecryptFile(dataIn, keyIn, passwd, decryptFile);
                if (success) {
                    System.out.println("Decrypted file saved to: " + decryptFile.getAbsolutePath());
                } else {
                    System.out.println("Decryption failed for file: " + encryptedFilePath);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("IOException during decryption: " + e.getMessage());
            } catch (PGPException e) {
                e.printStackTrace();
                System.out.println("PGPException during decryption: " + e.getMessage());
            } finally {
                // Close streams
                dataIn.close();
                keyIn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception during decryption: " + e.getMessage());
        }
    }

    static boolean ActiondecryptFile(InputStream dataIn, InputStream keyIn, char[] passwd, File decryptFile)
            throws IOException, PGPException {
      //  System.out.println("Inside ActiondecryptFile method");

        try {
            JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(PGPUtil.getDecoderStream(dataIn));
            PGPEncryptedDataList enc;

            Object o = pgpF.nextObject();
            if (o instanceof PGPEncryptedDataList) {
                enc = (PGPEncryptedDataList) o;
            } else {
                enc = (PGPEncryptedDataList) pgpF.nextObject();
            }

            // Find the secret key
            Iterator<?> it = enc.getEncryptedDataObjects();
            PGPPrivateKey sKey = null;
            PGPPublicKeyEncryptedData pbe = null;

            PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyIn),
                    new JcaKeyFingerprintCalculator());

            while (sKey == null && it.hasNext()) {
                pbe = (PGPPublicKeyEncryptedData) it.next();
                long keyID = pbe.getKeyID();
            //    System.out.println("Trying to decrypt with key ID: " + Long.toHexString(keyID));
                sKey = findSecretKey(pgpSec, keyID, passwd);
            }

            if (sKey == null) {
                throw new IllegalArgumentException("Secret key for message not found.");
            }

            InputStream clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(sKey));
            JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clear);
            Object message = plainFact.nextObject();

            if (message instanceof PGPCompressedData) {
                PGPCompressedData cData = (PGPCompressedData) message;
                JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(cData.getDataStream());
                message = pgpFact.nextObject();
            }

            if (message instanceof PGPLiteralData) {
                PGPLiteralData ld = (PGPLiteralData) message;
                InputStream unc = ld.getInputStream();
                OutputStream fOut = new BufferedOutputStream(new FileOutputStream(decryptFile));
                Streams.pipeAll(unc, fOut);

                fOut.close();
            } else if (message instanceof PGPOnePassSignatureList) {
                throw new PGPException("Encrypted message contains a signed message - not literal data.");
            } else {
                throw new PGPException("Message is not a simple encrypted file - type unknown.");
            }

            if (pbe.isIntegrityProtected() && !pbe.verify()) {
                throw new PGPException("Message failed integrity check.");
            }

            return true;
        } catch (PGPException e) {
            if (e.getUnderlyingException() != null) {
                e.getUnderlyingException().printStackTrace();
                System.out.println("Underlying PGPException: " + e.getUnderlyingException().getMessage());
            }
            throw e; // Rethrow exception for caller to handle
        }
    }

    static PGPPrivateKey findSecretKey(PGPSecretKeyRingCollection pgpSec, long keyID, char[] pass)
            throws PGPException {
        PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);

        if (pgpSecKey == null) {
            return null;
        }
        return pgpSecKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(pass));
    }
}
