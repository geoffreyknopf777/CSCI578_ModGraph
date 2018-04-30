package ModGraph;

public class Name {
	private String first;
	private String last;
	
	public Name(String firstname, String lastname) {
		first = firstname;
		last = lastname;
	}
	
	public String getFirstName() {
		return first;
	}
	
	public String getLastName() {
		return last;
	}
	
	public String getFullName() {
		return first + " " + last;
	}

}
