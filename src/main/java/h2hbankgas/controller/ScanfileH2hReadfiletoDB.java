package h2hbankgas.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import h2hbankgas.service.DecryptFileOpenPGP;

@Controller
public class ScanfileH2hReadfiletoDB {
	@Autowired
    private DataSource dataSource;
	
	static String  ketqua = "200: Hoàn thành";
	static String directoryEncrypted = "";// "/home/UAT/shared_data";
	static String vbankcode = "";
	
	@RequestMapping(value = "/scanfilefeadfile", method = RequestMethod.POST)
	public ResponseEntity<String> xulyscanfile(HttpServletRequest httpRequest) {
		  vbankcode = httpRequest.getHeader("bank_code");
		//   System.out.println("vHeader = " + vbankcode);
	       String foldermahoa =  httpRequest.getHeader("foldermahoa"); //"D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\mahoa";
	       String folderbackup = httpRequest.getHeader("folderbackup"); //"D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\backup";
	       String foldergiaima = httpRequest.getHeader("foldergiaima"); //D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\mahoa\\decrypt";
	       File folder = new File(foldermahoa);
	       try {
	    	   Connection conn = dataSource.getConnection();
	             if (folder.exists() && folder.isDirectory()) {
	                         File[] files = folder.listFiles();
	                      //   System.out.println ("Den day 1 = ");
	                         if (files != null) {
	                             for (File file : files) {
	                            	 
	                            	 // truong hop khong phai la file thì lặp tiếp
	                            	 if (!file.isFile()) {
	                     				continue;
	                     				}
	                            	//  ignore none ACK,.NACK,PSR   file
	                            	 if (file.getName().startsWith("ACK") || file.getName().startsWith("NACK") || file.getName().startsWith("PSR")) {
	                            		// goi lop ma hoa viet  h2hbankgas.service.DecryptFileOpenPGP;
		                            	 String fileName = file.getName();
		                         //   	 System.out.println ("fileName fileName = " + fileName);
		                            	
		                            	 // tên file sẽ giải mã hóa
	                                    String decryptFilePath = foldergiaima + "/" + fileName.replace(".encrypt", "");
	                             //       System.out.println ("decryptFilePath decryptFilePath = " + decryptFilePath);
	                                  // đường dẫn folder mã hóa.
				                      File directorynew = new File(foldergiaima);
				                         if (!directorynew.exists()) {
				                        	 directorynew.mkdirs(); // Tạo thư mục nếu chưa tồn tại
				                         }
				                    	 
				                         // Lấy đường dẫn đầy đủ của file mã hóa để giải mã.
				                    	 String encryptedFilePath = foldermahoa + "/" + fileName;
				            //        	 System.out.println ("encryptedFilePath = " + encryptedFilePath);
				                    	 // lay password key
				                         String passwpublickey = "nangcapgas2024" ; //getpasswordpublickey (conn);
				                         String pgpPrivateKey = "D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\VCB\\nangcapgas2024_0xB3F18325_SECRET.asc" ;  //getpgpPrivateKey (conn);
		                            	 
				                    //     System.out.println ("Goi giai ma");
				                         DecryptFileOpenPGP.AppdecryptFile(encryptedFilePath,decryptFilePath, passwpublickey, pgpPrivateKey);
				                    //     System.out.println ("Ket thuc");
		                            	 // di chuyển file về thư mục backup sau đó xóa file trong thư mục mã hóa đi
		                             	 movefile(file,folderbackup);
		                                 // thuc hien goi ham doc du lieu file ghi vao trong database.
		                            	 //System.out.println("decryptFilePath=" + decryptFilePath);
	                                      readfileinsertdatabase(conn, decryptFilePath);
	                            	 }
	                            	 else  {
	                            		        System.out.println("File khong co ");
	                            		        ketqua="2011: thu muc rong.";
	                            		        continue; // Bỏ qua xử lý các tệp không phù hợp
	                            		    }
	                             }
	                         } else {
	                        	 ketqua="201: thu muc rong.";
	                         }
	                     } else {
	                         //System.out.println("duong dan ko ton tai.");
	                    	 ketqua="202: duong dan ko ton tai.";
	                     } 
	               } catch (Exception e) {
	                   e.printStackTrace();
	                   ketqua = "203: Loi khac: " + e.getMessage();
	               }
	       return ResponseEntity.ok(ketqua); 

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
               
               if (!file.delete()) {
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
           }catch(Exception e) {
               e.printStackTrace();
                //System.out.println ("bi loi ");
               ketqua = "206: loi khac trong class movefile";
            }
   }   

	//  lay thong tin duong dan file private key
	    static String getpgpPrivateKey (Connection conn) {
	        String vpassword = null;
	        String  vsql =  "{ ? = call apps.VNA_SERVICE_API.getprivateKeyFilePath}";
	        try {
	        	CallableStatement cstmt = conn.prepareCall(vsql);
	            cstmt.registerOutParameter(1,Types.CHAR);
	            cstmt.execute();
	            vpassword = cstmt.getString(1);
	        }catch(Exception e) {
	            e.printStackTrace();
	           //  System.out.println ("Loi lay vpassword trong class checkdalayvechua = " + e.getMessage());
	            ketqua = "207: Loi lay Privateky trong class getpgpPrivateKey" + e.getMessage();
	         } 
	        return vpassword;
	       }
	    
	    
	 // lay thong tin password publickey
	    static String getpasswordpublickey (Connection conn) {
	        String vpassword = null;
	        String  vsql =  "{ ? = call apps.VNA_SERVICE_API.getpassphrase}";
	        try {
	        	CallableStatement cstmt = conn.prepareCall(vsql);
	            cstmt.registerOutParameter(1,Types.CHAR);
	            cstmt.execute();
	            vpassword = cstmt.getString(1);
	        }catch(Exception e) {
	            e.printStackTrace();
	             //System.out.println ("Loi lay vpassword trong class checkdalayvechua = " + e.getMessage());
	            ketqua = "208: Loi lay Pass trong class getpasswordpublickey" + e.getMessage();
	         } 
	        return vpassword;
	       }
	
  static void readfileinsertdatabase (Connection conn, String decryptFile ) {
		try  {   
			       BufferedReader br = new BufferedReader(new FileReader(decryptFile)); 
			       String line;
		            while ((line = br.readLine()) != null) {
		              /*  String[] data = line.split(","); // Assuming the data is separated by commas
		
		                String callpackage = "{call VNA_SERVICE_API.ServiceScanfileReadfile(?, ?, ?)}";
		                
		                CallableStatement stmt = conn.prepareCall(callpackage);
		                
		                stmt.setString(1, data[0]); // Assuming the data is in order
		                stmt.setString(2, data[1]);
		                stmt.setString(3, data[2]);
		              */
		            	 String callpackage = "{call apps.VNA_SERVICE_API.ServiceScanfileReadfile(?, ? )}";
			                
			            CallableStatement stmt = conn.prepareCall(callpackage);
			            stmt.setString(1,vbankcode);
			            stmt.setString(2,line);
		                stmt.execute();
		                stmt.close();
		            }
		            br.close();
		            //System.out.println("Data inserted successfully.");
		            ketqua = "200: Hoàn thành";
				} catch (Exception e) {
		            e.printStackTrace();
		            //System.out.println("Loi." +  e.getMessage());
		            ketqua = "209: Lỗi trong readfileinsertdatabase: " +e.getMessage();
		        }
    }
}
