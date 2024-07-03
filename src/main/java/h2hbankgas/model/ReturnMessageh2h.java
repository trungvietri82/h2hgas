package h2hbankgas.model;

public class ReturnMessageh2h {
	private String status;
    private String message;
    private String id;
    
    public ReturnMessageh2h() { 
    	
    }
    
    public String getstatus() {
        return status;
    }

    public void setstatus(String status) {
        this.status = status;
    }

    public String getmessage() {
        return message;
    }

    public void setmessage(String message) {
        this.message = message;
    }

    public String getid() {
        return id;
    }

    public void setid(String id) {
        this.id = id;
    }
}
