package ModGraph;

public class FormSubmissionHandler {

	//first argument = sURL, second argument = sLang (c or java)
	public static void main(String[] args) {
		
		//Setup default arguments
		if(args.length < 2) {
			args = new String[2];
			args[0] = "https://github.com/apache/hadoop";
			args[1] = "java";
		}
		
		//Display arguments to user
		System.out.println("Repo: " + args[0]);
		System.out.println("Language: " + args[1]);
		
		//Invoke the GitCommitParser
		GitCommitParser.main(args);
		
		//Invoke the ModifiabilityCalculator
		ModifiabilityCalculator.main(args);
		
		//Invoke the ArcRecoveryManager
		//ArcRecoveryManager.main(args);
	}
	
}
