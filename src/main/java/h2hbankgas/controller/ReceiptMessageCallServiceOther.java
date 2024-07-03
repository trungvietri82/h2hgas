package h2hbankgas.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import h2hbankgas.controller.TestSSLCCA;

@Controller
public class ReceiptMessageCallServiceOther {
	
	@Autowired
	private DataSource dataSource;
	
	//@RequestMapping(value = "/callservicenganhang", method = RequestMethod.POST)
	@PostMapping("/callservicenganhang")
	public ResponseEntity<String> callService(HttpServletRequest requestHeader,@RequestBody String requestBody) {
		String tokentcb  = "";
        try {
            // Lấy thông tin Tabpayment_id từ request header của service nhận
    		String vbankcode = requestHeader.getHeader("bank_code");
    		Connection conn = dataSource.getConnection();
    		
    		try {
				tokentcb = TestSSLCCA.CallAPItoken();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				tokentcb  = "loi: " + e.getMessage();
				e.printStackTrace();
			}
    		// lay thong tin header cua tung payment tab
   // 		String vheader2 = getHeaderApi(vbankcode,conn);
    //	    String url = geturlApi(vbankcode,conn); // Địa chỉ URL của service POST
   // 	    String vtokenJWT = gettokenJWT_TCB();
   // 	    System.out.println("vtokenJWT = " + vtokenJWT);
    	    
    	  /*  HttpClient httpClient = HttpClients.createDefault();
    	    HttpPost httpPost = new HttpPost(url);
    	    
    	    
    	    
    	    httpPost.setHeader("Authorization", vtokenJWT);
    	    // Đặt header cho request
    	    String[] headers = vheader2.toString().split(", ");
    	    for (String header : headers) {
    	        String[] headerParts = header.split(":");
    	        if (headerParts.length == 2) {
    	            httpPost.setHeader(headerParts[0].trim(), headerParts[1].trim());
    	           // System.out.println("setHeader =  " + headerParts[0] + headerParts[1]);
    	        }
    	    }
           
    	    // Đặt body cho request
    	    httpPost.setEntity(new StringEntity(requestBody));
    	    // Thực hiện request POST
    	    HttpResponse response = httpClient.execute(httpPost);
    	    // Đọc kết quả từ response
    	    HttpEntity entity = response.getEntity();
    	    String responseContent = EntityUtils.toString(entity);
             
    			
    	    return ResponseEntity.status(HttpStatus.OK).body(responseContent); // ResponseEntity.ok(responseContent); 
    	    */
    	    return ResponseEntity.status(HttpStatus.OK).body(tokentcb);
    	    //IOException |
    		} catch ( SQLException  e) {
      
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
            }
    	}

	 // lay thong tin url ngan hang
    public static String geturlApi (String  vtabpaymentId, Connection conn) {
        String vgeturl = null;
        String  vsql =  "{ ? = call apps.VNA_SERVICE_API.GetUrlAPInganhang (?) }";
        try {
        	CallableStatement cstmt = conn.prepareCall(vsql);
            cstmt.registerOutParameter(1,Types.CHAR);
            cstmt.setString(2, vtabpaymentId);
            cstmt.execute();
            vgeturl = cstmt.getString(1);
        }catch(Exception e) {
            e.printStackTrace();
             System.out.println ("Loi lay geturlApi trong class geturlApi = " + e.getMessage());
         } 
        return vgeturl;
       }
    
	 // lay thong tin chuoi Header theo tung ngan hang
    public static String getHeaderApi (String vtabpaymentId, Connection conn) {
        String vgetHeaderApi = null;
        String  vsql =  "{ ? = call apps.VNA_SERVICE_API.GetheaderAPInganhang (?) }";
        try {
        	CallableStatement cstmt = conn.prepareCall(vsql);
            cstmt.registerOutParameter(1,Types.CHAR);
            cstmt.setString(2, vtabpaymentId);
            cstmt.execute();
            vgetHeaderApi = cstmt.getString(1);
        }catch(Exception e) {
            e.printStackTrace();
             System.out.println ("Loi lay getHeaderApi trong class getHeaderApi = " + e.getMessage());
         } 
        return vgetHeaderApi;
       }
    
    public static String gettokenJWT_TCB() {
        try {
            String url = "https://partner-apigw-uat.techcombank.com.vn/common-security-auth-services/v1/jwt-token";
            String clientId = "98e5017a51f3429f93891c8786f18431";
            String clientSecret = "093Aa2412EdC41d18c08D3f7eCf53481";

            // Load TrustStore
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream instream = new FileInputStream("D:\\IERP\\Key_api\\trust_local.jks"); // Đường dẫn đến file TrustStore
            trustStore.load(instream, "vna2024".toCharArray()); // Mật khẩu mặc định của TrustStore là "changeit"
            instream.close();

            // Tạo TrustManagerFactory với TrustStore
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // Tạo SSLContext với TrustManager từ TrustStore
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            // Tạo HTTP client với SSLContext đã cấu hình
            HttpClient httpClient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .build();

            // Tạo POST request
            HttpPost httpPost = new HttpPost(url);

            // Thiết lập các header
            httpPost.setHeader("client_id", clientId);
            httpPost.setHeader("client_secret", clientSecret);
            httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");

            // Thực thi request và nhận response
            HttpResponse response = httpClient.execute(httpPost);

            // Đọc nội dung của response
            HttpEntity responseEntity = response.getEntity();
            String responseContent = EntityUtils.toString(responseEntity);

            return responseContent;

        } catch (Exception e) {
            e.printStackTrace();
            return "loi";
        }
    }
}
