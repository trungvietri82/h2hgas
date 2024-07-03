package h2hbankgas.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import h2hbankgas.service.DecryptFileOpenPGP;

@Controller
public class ScanfileCopysFtptoTemGAS {
	@Autowired
    private DataSource dataSource;
	
	static String  ketqua = "200: Hoàn thành";
	static String  checkloi = "";
	static String vbankcode = "";
	
	@RequestMapping(value = "/scanfilesftptogas", method = RequestMethod.POST)
	public ResponseEntity<String> xulyscanfile(HttpServletRequest httpRequest) {
		  vbankcode = httpRequest.getHeader("bank_code");
		
	     try {
		   // ket noi toi database
	    	 Connection conn = dataSource.getConnection();
		 //thu hien goi lenh scan file trong thu muc server qua phuong thuc sFTP.
		    scanfile(conn,vbankcode);
		   //
		 return ResponseEntity.ok(ketqua);
	     }catch (SQLException e) {
	            e.printStackTrace();
	           // return new ResponseEntity<>("Loi thuc hien", HttpStatus.NOT_FOUND);
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
	        }
	}
	
	static void scanfile (Connection conn, String nganhangcode){
		String host = ""; // "192.168.21.11";
        String username = "";// "applmgr";
        String password = "";//"iERP@11042013";
        String sourceDirectory ="";// "/home/UAT/shared_data";
        int port = 1;
        
        //thu muc keo file tu sftp ve 
        String vnafolderEncrypted = "";//"D:\\IERP\\PROJECT\\VIETNAMAIRLINE\\Nangcap_GAS\\Code\\SERCICEAPI\\";
        String vnafolderDecrypt = "";
     // doan lenh lay thong tin trong tu database
        String  vsql =  "{ call apps.VNA_SERVICE_API.GetsftpNganHang (?, ?, ?, ?, ?, ?, ?, ?) }";
        try {
          CallableStatement cstmt = conn.prepareCall(vsql);
            cstmt.setString(1, nganhangcode);  
            cstmt.registerOutParameter(2,Types.CHAR);
            cstmt.registerOutParameter(3,Types.CHAR);
            cstmt.registerOutParameter(4,Types.CHAR);
            cstmt.registerOutParameter(5,Types.CHAR);
            cstmt.registerOutParameter(6,Types.CHAR);
            cstmt.registerOutParameter(7,Types.CHAR);
            cstmt.registerOutParameter(8,Types.INTEGER);
            cstmt.execute();
            host = cstmt.getString(2);
            username = cstmt.getString(3);
            password = cstmt.getString(4);
            sourceDirectory = cstmt.getString(5);
            vnafolderEncrypted = cstmt.getString(6);
            vnafolderDecrypt = cstmt.getString(7);
            port = cstmt.getInt(8);
        }catch(Exception e) {
            e.printStackTrace();
             //System.out.println ("Loi lay thong tin sFTPs scanfile = " + e.getMessage());
            ketqua = "Loi lay thong tin sFTPs scanfile = " + e.getMessage();
         } 
        
        //////////////////////////
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;
        
        String Checkfiletontai ="";
        
        try {
            session = jsch.getSession(username, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            channelSftp.cd(sourceDirectory);
            
            @SuppressWarnings("unchecked")
			Vector<ChannelSftp.LsEntry> files = channelSftp.ls("*"); // Scan all files in the directory

            for (ChannelSftp.LsEntry file : files) {
                if (!file.getAttrs().isDir()) {
                    // lay ten file
                	 String fileName = file.getFilename();
                	 Checkfiletontai = checkdalayvechua(conn,fileName);
                if (Checkfiletontai.endsWith("CHUATONTAI"))  {
                	// Nếu chưa từng lấy về thì sẽ thực hiện lấy file về
                	// thuc hien copy tu sftp ve thu muc server
                     String destinationFilePath = vnafolderEncrypted + '/' + fileName;   
                     InputStream fileStream = channelSftp.get(fileName);
                     movefile(fileStream,vnafolderEncrypted,destinationFilePath);
                     
                     
                  /* Neu thu tuc movefile tu sftp ve server gas thanh cong 
                   * neu thanh cong là "NONE", nguoc lai la "LOI"
                   * Thanh cong thi thuc hien goi ham luu logfile file ghi vao trong database.
                   */
	                     if (checkloi.endsWith("NONE")) {
			                    	 
			                    	// System.out.println("FIle du lieu ma hoa" + destinationFilePath);
			                    	 // thuc hien goi thu tuc ma hoa de giai ma file       
			                    	 // lay file vua keo ve server de gia ma
			                    	 String decryptFilePath = vnafolderDecrypt + "/" + fileName.replace(".pgp", "");
			                    	 
			                    	 File directorynew = new File(vnafolderDecrypt + "/");
			                         if (!directorynew.exists()) {
			                        	 directorynew.mkdirs(); // Tạo thư mục nếu chưa tồn tại
			                         }
			                    	 
			                    	 String encryptedFilePath = destinationFilePath;
			                    	 // lay password key
			                         String passwpublickey = getpasswordpublickey (conn);
			                         String pgpPrivateKey = getpgpPrivateKey (conn);
			                    	 // goi lop ma hoa viet o  h2hbankgas.service.DecryptFileOpenPGP;
			                    	 DecryptFileOpenPGP.AppdecryptFile(encryptedFilePath,decryptFilePath, passwpublickey, pgpPrivateKey);
			                    	 
			                    	 insertlogtodatabase(conn, fileName);
		                        }
	                }else {
	                	//System.out.println("File dã lay về "); 
	                	ketqua ="201: File dã lay về " + fileName;
	                }// if (Checkfiletontai.endsWith("CHUATONTAI"))
                }  //if (!file.getAttrs().isDir())
            }  //for (ChannelSftp.LsEntry file : files)
        } catch (JSchException | SftpException | IOException e) {
            e.printStackTrace();
            ketqua = "999: loi khong ket noi duoc sftp server";
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
	
	// check file do co ton tai trong database khong
    static String checkdalayvechua (Connection conn, String filename) {
        String giatricheck = null;
        String  vsql =  "{ ? = call apps.VNA_SERVICE_API.checkfiledalay (?) }";
        try {
        	CallableStatement cstmt = conn.prepareCall(vsql);
            cstmt.registerOutParameter(1,Types.CHAR);
            cstmt.setString(2, filename);  
            cstmt.execute();
            giatricheck = cstmt.getString(1);
        }catch(Exception e) {
            e.printStackTrace();
             //System.out.println ("Loi lay vgetpassphrase trong class getpassphrase = " + e.getMessage());
            ketqua = "202: Loi lay vgetpassphrase trong class getpassphrase = " + e.getMessage();
         } 
        return giatricheck;
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
             //System.out.println ("Loi lay vpassword trong class checkdalayvechua = " + e.getMessage());
            ketqua = "203: Loi lay PrivateKey trong class getpgpPrivateKey = " + e.getMessage();
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
            ketqua = "204: Loi lay vpassword trong class getpasswordpublickey = " + e.getMessage();
         } 
        return vpassword;
       }
    
  static void insertlogtodatabase (Connection conn, String fileName ) {
		try  {  
			 String callpackage = "{call VNA_SERVICE_API.insertlogfile(?, ?)}"; 
		                CallableStatement stmt = conn.prepareCall(callpackage); 
		                stmt.setString(1, fileName);   
		                stmt.setString(2, "ScanfileCopysFtptoTemGAS");   
		                stmt.execute();
		                stmt.close();
		            //System.out.println("Data inserted successfully.");
		                ketqua = "200: Hoàn thành";
				} catch (Exception e) {
		            e.printStackTrace();
		        }
    }
  
  static void movefile(InputStream fileStream,String destinationDirectory, String destinationFilePath ) {

      FileOutputStream outputStream = null;
      File directory = new File(destinationDirectory);
	      if (!directory.exists()) {
	          directory.mkdirs(); // Tạo thư mục nếu chưa tồn tại
	      }
	     
      try{
    	   outputStream = new FileOutputStream(destinationFilePath);
    	    // Ghi nội dung file từ InputStream vào file mới
    	    byte[] buffer = new byte[4096];
    	    int bytesRead;
    	    while ((bytesRead = fileStream.read(buffer)) != -1) {
    	        outputStream.write(buffer, 0, bytesRead);
    	    }
    	    outputStream.close();
    	    //System.out.println("File saved to: " + destinationFilePath);
    	} catch (IOException e) {
    	    //System.err.println("Error saving file to: " + destinationFilePath);
    		checkloi = "LOI";
    	    e.printStackTrace();
    	} finally {
		    	    try {
		    	        if (outputStream != null) {
		    	            outputStream.close();
		    	            checkloi = "NONE";
		    	        }
		    	    } catch (IOException e) {
		    	        e.printStackTrace();
		    	        checkloi = "LOI";
		    	    }
    	           }
      }
   
}
