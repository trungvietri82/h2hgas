package h2hbankgas.controller;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import h2hbankgas.model.ReturnMessageh2h;



@Controller
public class ReceiptAPIFromBankSha256 {
	@Autowired
    private DataSource dataSource;
	
	@RequestMapping(value = "/receiptbanksha256", method = RequestMethod.POST)
	public ResponseEntity<ReturnMessageh2h> receiptbank(@RequestBody String request, HttpServletRequest httpRequest ) {	 
		 ReturnMessageh2h returnmesage = new ReturnMessageh2h(); 
		 String vreturn = "";
		 String vHeader = httpRequest.getHeader("idnganhang");
		// String headerValue2 = httpRequest.getHeader("headerName2");
		try  {
			   Connection conn = dataSource.getConnection();
               CallableStatement stmt = conn.prepareCall("{ ? = call VNA_SERVICE_API.ReceiptAPI_nganhang(?, ?)}");
                
                stmt.registerOutParameter(1,Types.CHAR);
                stmt.setString(2, vHeader);
	            stmt.setString(3, request);
	            stmt.execute();
	            
	            vreturn = stmt.getString(1);

	            returnmesage.setstatus("200");
	            returnmesage.setmessage("Hoan Thanh");
	            returnmesage.setid(vreturn);
	            
	            return new ResponseEntity<>(returnmesage, HttpStatus.OK);
	            
	        } catch (SQLException e) {
	            e.printStackTrace();
	            returnmesage.setstatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
	            returnmesage.setmessage(e.getMessage());
	            returnmesage.setid("-1");
	            return new ResponseEntity<>(returnmesage, HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	}

}
