package h2hbankgas.model;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "VNA_USERNAME_API")
public class User {
		  private long id;
		  private String username;
		  private String password;
		   
		  public User() {

		  }

		  public User(String username, String password) {
		    this.username = username;
		    this.password = password; 
		  }

		  @Id
		  @GeneratedValue(strategy = GenerationType.AUTO)
		  public long getId() {
		    return id;
		  }

		  public void setId(long id) {
		    this.id = id;
		  }

		  @Column(name = "username", nullable = false)
		  public String getUsername() {
		    return username;
		  }

		  public void setUsername(String username) {
		    this.username = username;
		  }

		  @Column(name = "password", nullable = false)
		  public String getPassword() {
		    return password;
		  }

		  public void setPassword(String password) {
		    this.password = password;
		  }

}
