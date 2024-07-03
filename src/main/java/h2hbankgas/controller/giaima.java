package h2hbankgas.controller;

import java.io.*;
import java.security.Security;
import java.util.Collection;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class giaima {
    String v_privateKeyFilePath = "D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\VCB\\nangcapgas2024_0xB3F18325_SECRET.asc";
    String v_encryptedFile = "D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\VCB\\OUT\\ACK_TMP111979_00001105_0000110526_MD_25052021_Vinatex_160314.csv.encrypt";
    String v_decryptedFile = "D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\VCB\\OUT\\ACK_TMP111979_00001105_0000110526_MD_25052021_Vinatex_160314.csv";
    String trungpass = "nangcapgas2024";

    @RequestMapping(value = "/giaima", method = RequestMethod.POST)
    public ResponseEntity<String> giaima() {
        try {
            InputStream encryptedFile = new FileInputStream(v_encryptedFile);
            InputStream privateKeyStream = new FileInputStream(v_privateKeyFilePath);
            char[] passPhrase = trungpass.toCharArray();
            OutputStream outputFile = new FileOutputStream(v_decryptedFile);
            decryptFile(encryptedFile, privateKeyStream, passPhrase, outputFile);
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.ok("Có lỗi xảy ra: " + ex.getMessage());
        }
    }

    public static void decryptFile(InputStream encryptedFile, InputStream privateKeyStream, char[] passPhrase, OutputStream outputFile) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // Read the encrypted file
        InputStream in = PGPUtil.getDecoderStream(encryptedFile);
        ArmoredInputStream armoredInputStream = new ArmoredInputStream(in);

        // Create PGP object factory
        PGPObjectFactory pgpF = new PGPObjectFactory(armoredInputStream, null);
        Object o = pgpF.nextObject();

        // Initialize variables for key extraction
        PGPSecretKeyRingCollection pgpSec = null;
        PGPPrivateKey sKey = null;

        // Iterate through the PGP objects
        while (o != null) {
            if (o instanceof PGPEncryptedDataList) {
                PGPEncryptedDataList encList = (PGPEncryptedDataList) o;
                Iterator<?> it = encList.getEncryptedDataObjects();
                while (sKey == null && it.hasNext()) {
                    PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) it.next();
                    if (pgpSec == null) {
                        pgpSec = new PGPSecretKeyRingCollection((Collection<PGPSecretKeyRing>) PGPUtil.getDecoderStream(privateKeyStream));
                    }
                    PGPSecretKey pgpSecKey = pgpSec.getSecretKey(encData.getKeyID());
                    if (pgpSecKey != null) {
                        sKey = pgpSecKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passPhrase));
                    }
                }
            }
            o = pgpF.nextObject();
        }

        if (sKey == null) {
            throw new IllegalArgumentException("Secret key for message not found.");
        }

        // Decrypt data
        PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) o;
        InputStream clear = encData.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(sKey));
        BufferedOutputStream out = new BufferedOutputStream(outputFile);

        byte[] buff = new byte[1024];
        int read;
        while ((read = clear.read(buff)) != -1) {
            out.write(buff, 0, read);
        }

        out.close();
        clear.close();

        // Close streams
        armoredInputStream.close();
        in.close();
    }
}
