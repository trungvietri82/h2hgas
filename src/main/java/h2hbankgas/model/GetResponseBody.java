package h2hbankgas.model;

public class GetResponseBody {
	
 
	private String vFileName;
	private String encryptFilePath;
	private String decryptedFilePath;
	private String folderBackupPath;
	private String vBankcode;
	private String paymentsheetDetailid;   
	public GetResponseBody() {

	}
	
	public GetResponseBody(String vfilename, String encryptfilepath, String decryptedFilePath, 
			               String folderBackupPath,
			               String vBankcode,
			               String paymentsheetDetailid) {
		this.vFileName  = vfilename;
		this.encryptFilePath = encryptfilepath;
		this.decryptedFilePath = decryptedFilePath;
		this.folderBackupPath  = folderBackupPath;
		this.vBankcode   =vBankcode;
		this.paymentsheetDetailid= paymentsheetDetailid;
	}
   
	public String  getpaymentsheetDetailid() {
		return paymentsheetDetailid;
	}

	public void setpaymentsheetDetailid(String paymentsheetDetailid) {
		this.paymentsheetDetailid = paymentsheetDetailid;
	}
	
	public String  getvBankcode() {
		return vBankcode;
	}

	public void setvBankcode(String vBankcode) {
		this.vBankcode = vBankcode;
	}
	
	public String  getvFileName() {
		return vFileName;
	}

	public void setfolderBackupPath(String folderBackupPath) {
		this.folderBackupPath = folderBackupPath;
	}
	
	public String  getfolderBackupPath() {
		return folderBackupPath;
	}

	public void setvFileName(String vfilename) {
		this.vFileName = vfilename;
	}
	
	
	public String  getencryptFilePath() {
		return encryptFilePath;
	}

	public void setencryptFilePath(String encryptFilePath) {
		this.encryptFilePath = encryptFilePath;
	}

   //-----------
   public String  getdecryptedFilePath() {
		return decryptedFilePath;
	}

	public void setdecryptedFilePath(String decryptedFilePath) {
		this.decryptedFilePath = decryptedFilePath;
	}

}
