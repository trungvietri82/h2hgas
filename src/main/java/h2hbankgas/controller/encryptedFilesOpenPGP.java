package h2hbankgas.controller;


import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.Iterator;

import javax.sql.DataSource;

import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import h2hbankgas.model.GetResponseBody;

@Controller
public class encryptedFilesOpenPGP {
	@Autowired
    private DataSource dataSource;
	
	static String ketqua ="";
	
	//@RequestMapping(value = "/encryptedfiles", method = RequestMethod.POST)
	@PostMapping("/encryptedfiles")
    public ResponseEntity<String> giaima(@RequestBody GetResponseBody requestBody) {
	
	String pgpPublicKey = "";// "D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\nangcapgas2024_0xB3F18325_public.asc";
    String vfilename = requestBody.getvFileName();
    System.out.println("vfilename: " + vfilename);
    String originalFile = requestBody.getdecryptedFilePath() +'/'+ vfilename;  //"D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\" + vfilename;
   System.out.println("originalFile: " + originalFile);
    String vfolderbackup = requestBody.getfolderBackupPath();
    System.out.println("folderbackup: " + vfolderbackup);
    // duong dan file ma hoa
    String encryptFilepath  = requestBody.getencryptFilePath() + '/' + vfilename;// "D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\" + vfilename;
    System.out.println("encryptFilepath: " + encryptFilepath);
	String vBankcode =  requestBody.getvBankcode();
	System.out.println("vBankcode: " + vBankcode);
	String paymentsheetDetailid = requestBody.getpaymentsheetDetailid();
	System.out.println("paymentsheetDetailid: " + paymentsheetDetailid);
    try {
				// thuc hien inset database
                // luu file vao trong database
				Connection conn = dataSource.getConnection();
				pgpPublicKey = getpgpPublicKey (conn,vBankcode);
				
				System.out.println("pgpPublicKey: " + pgpPublicKey);
				
                File filedaky = new File(originalFile);
                
			//	InputStream inputStream = new FileInputStream(filedaky);
                System.out.println("goi saveFileToDatabase : " );
                saveFileToDatabase(conn, filedaky,paymentsheetDetailid);
                
                System.out.println("goi ket thuc goi saveFileToDatabase : " );
                
                conn.close();
                 ////
                // Thuc hien ma hoa file
                encryptFile(originalFile, pgpPublicKey, encryptFilepath);
		        //System.out.println("Encrypt Result: " + encryptedFile.getAbsolutePath());
		        
		        movefile (filedaky, vfolderbackup);
		    return ResponseEntity.ok(ketqua);
			} catch (Exception e) {
		        e.printStackTrace();
		        return ResponseEntity.ok("Lỗi." + e.getMessage());
		    }
	}
	 void  encryptFile(String reconfilePath, String pgpPubKeyPath, String encryptFilepath) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        // build reconcile file
        PGPPublicKey pgpPubKey = null;
        try {
            InputStream keyIn = PGPUtil.getDecoderStream(new FileInputStream(pgpPubKeyPath));
            pgpPubKey = readPublicKey(keyIn);
            keyIn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lôi ơ day : " + e.getMessage());
        }
        File reconFile = new File(reconfilePath);
        File encryptFile = new File(encryptFilepath + ".encrypt");

        // do encrypt file
        boolean success = processEncryptReconfiles(reconFile, encryptFile, pgpPubKey);
        if (!success) {
        	ketqua = "Lỗi khi mã hóa";
        }
        ketqua = "Hoàn thành";
    }
	
	static PGPPublicKey readPublicKey(InputStream input) throws IOException, PGPException {
        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(input),
                new JcaKeyFingerprintCalculator());

        Iterator<PGPPublicKeyRing> keyRingIter = pgpPub.getKeyRings();
        while (keyRingIter.hasNext()) {
            PGPPublicKeyRing keyRing = (PGPPublicKeyRing) keyRingIter.next();

            Iterator<PGPPublicKey> keyIter = keyRing.getPublicKeys();
            while (keyIter.hasNext()) {
                PGPPublicKey key = (PGPPublicKey) keyIter.next();

                if (key.isEncryptionKey()) {
                    return key;
                }
            }
        }

        throw new IllegalArgumentException("Can't find encryption key in key ring.");
    }
	
	static boolean processEncryptReconfiles(File signedFile, File encryptFile, PGPPublicKey pgpPubKey) throws Exception {
        boolean withIntegrityCheck = true;

        // Create PGP encrypted data generator (using AES-256)
        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_256).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("BC"));

        // Set partner public key
        encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(pgpPubKey).setProvider("BC"));

        OutputStream encryptFileOut = null;
        OutputStream cOut = null;
        try {
            // Load and compress signed file
            final byte[] compressedData = compressFile(signedFile, CompressionAlgorithmTags.ZIP);

            // Create output stream to write encrypted data to encrypt folder
            encryptFileOut = new BufferedOutputStream(new FileOutputStream(encryptFile));
            cOut = encGen.open(encryptFileOut, compressedData.length);
            cOut.write(compressedData);
            cOut.close();
            cOut = null;

            encryptFileOut.close();
            encryptFileOut = null;

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // finally= success"
            if (cOut != null) {
                // retry close PGP out stream
                try {
                    cOut.close();
                } catch (Exception e) {
                }
            }

            if (encryptFileOut != null) {
                // retry close file output stream
                try {
                    encryptFileOut.close();
                } catch (Exception e) {
                }
            }
        }

        return false;
    }
	
	static byte[] compressFile(File file, int algorithm) throws IOException {
        try {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ;
            PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(algorithm);
            PGPUtil.writeFileToLiteralData(comData.open(bOut), PGPLiteralData.BINARY, file);
            comData.close();
            return bOut.toByteArray();
        } catch (Exception ex) {
            return null;
        }
    }
   
//  lay thong tin duong dan file Public key
    static String getpgpPublicKey (Connection conn, String vBankcode) {
        String vgetpgpPublicKey = null;
        String  vsql =  "{ ? = call apps.VNA_SERVICE_API.getNHpublicKeyFilePath(?)}";
        try {
        	CallableStatement cstmt = conn.prepareCall(vsql);
            cstmt.registerOutParameter(1,Types.CHAR);
            cstmt.setString(2, vBankcode);  
            cstmt.execute();
            vgetpgpPublicKey = cstmt.getString(1);
        }catch(Exception e) {
            e.printStackTrace();
           //  System.out.println ("Loi lay vpassword trong class checkdalayvechua = " + e.getMessage());
            ketqua = "207: Loi lay Privateky trong class getpgpPrivateKey";
         } 
        return vgetpgpPublicKey;
       }
    static void movefile(File file,String folderbackup) {
        InputStream inStream = null;
        OutputStream outStream = null;
        
        try {
               
                String duongdandich = folderbackup + "/" + file.getName();
               //System.out.println ("file.getName() = " + file.getName());
                //System.out.println ("duongdandich = " + duongdandich);
                
                File filedich = new File(duongdandich);
                
                inStream = new FileInputStream(file);
                outStream = new FileOutputStream(filedich);   
                
                 int length;
                 byte[] buffer = new byte[1024];
                  // copy the file content in bytes
                  while ((length = inStream.read(buffer)) > 0) {
                      outStream.write(buffer, 0, length);
                  }
                    // delete the original file
                  outStream.close();
                  inStream.close();
                  
              /*    if (!file.delete()) {
                      //System.err.println("Failed to delete encrypted file: " + file.getAbsolutePath());
               	   ketqua = "204: Loi khong xoa file trong thu muc";
                      try {
                          Files.delete(file.toPath());
                      } catch (IOException ex) {
                         // System.err.println("Error deleting file: " + file.getAbsolutePath() + ". Exception: " + ex.getMessage());
                   	   ketqua = "205: Loi khac xoa file" + file.getAbsolutePath() + ". Exception: " + ex.getMessage();
                          ex.printStackTrace();
                      }
                  }
               */
              }catch(Exception e) {
                  e.printStackTrace();
                   //System.out.println ("bi loi ");
                  ketqua = "206: loi khac trong class movefile " + e.getMessage();
               }
      }     
   static void saveFileToDatabase(Connection con, File file, String paymentSheetDetailid) {
        String  vsql =  "{ call apps.VNA_SERVICE_API.insert_blob (?, ?, ?) }";
        System.out.println("trong file saveFileToDatabase : " );
        try {
                  FileInputStream inputStream = new FileInputStream(file);
               
                  CallableStatement  cstmt = con.prepareCall(vsql);
                  cstmt.registerOutParameter(1,Types.CHAR);
                  
                  cstmt.setString(1, file.getName());
                  
                  System.out.println("file.getName()= : " + file.getName() );
                  
                  cstmt.setBinaryStream(2, inputStream, (int) file.length());
                  cstmt.setString(3,paymentSheetDetailid);
                  
                  System.out.println("paymentSheetDetailid= : " + paymentSheetDetailid );
                  
                  cstmt.execute();
                  
                  inputStream.close();
              }catch(Exception e) {
                  e.printStackTrace();
                   System.out.println ("bị loi ");
               } 
      }   

}
