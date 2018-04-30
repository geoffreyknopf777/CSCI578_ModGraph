package ModGraph;

import java.util.ArrayList;
import java.util.Date;

public class Commit {
	private String sCommitHash;
	private Name uAuthorName;
	private String sEmail;
	private Date uDate;
	private ArrayList <String> aMessage;
	private int nNumFilesChanged;
	private int nNumInsertions;
	private int nNumDeletions;
	
	public Commit(String sCommitHash, 
			  	  String firstname, 
			      String lastname, 
				  String sEmail,
				  Date uDate,
				  ArrayList <String> sMessage, 
				  int nNumFilesChanged, 
				  int nNumInsertions, 
				  int nNumDeletions) {
		this.sCommitHash = sCommitHash;
		this.uAuthorName = new Name(firstname, lastname);
		this.sEmail = sEmail;
		this.uDate = uDate;
		if(aMessage != null) {
			this.aMessage = new ArrayList<String>(aMessage);
		}
		this.nNumFilesChanged = nNumFilesChanged;
		this.nNumInsertions = nNumInsertions;
		this.nNumDeletions = nNumDeletions;
	}
	
	public String getHash() {
		return sCommitHash;
	}
	
	public Name getAuthorName() {
		return uAuthorName;
	}
	
	public String getEmail() {
		return sEmail;
	}
	
	public Date getDate() {
		return uDate;
	}
	
	public long getEpoch() {
		return uDate.getTime();
	}
	
	public String getMessage() {
		return aMessage.toString();
	}
	
	public int getNumFilesChanged() {
		return nNumFilesChanged;
	}
	
	public int getNumInsertions() {
		return nNumInsertions;
	}
	
	public int getNumDeletions() {
		return nNumDeletions;
	}

}
