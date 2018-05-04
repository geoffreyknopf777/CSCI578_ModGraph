package ModGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.io.FileUtils;

public class GitCommitParser {
	private static ArrayList<Commit> aCommits = new ArrayList<Commit>();
	public static int commit_index_100_percent = 0;
	public static int commit_index_75_percent = 0;
	public static int commit_index_50_percent = 0;
	public static int commit_index_25_percent = 0;
	
	public static ArrayList<Commit>getCommits() {
		return aCommits;
	}
	
	public static Commit parseNextCommit(BufferedReader b) {
		String sCommitHash = "";
		String firstname = "";
		String lastname = "";
		String sEmail = "";
		String sDate = "";
		Date uDate = new Date();
		ArrayList<String> aMessage = new ArrayList<String>();
		ArrayList<String> aChanges = new ArrayList<String>();
	    int nNumFilesChanged = 0;
		int nNumInsertions = 0;
		int nNumDeletions = 0;

		try {
			//Get the commit hash
			String sLine = b.readLine();
			if(sLine == null) {
				return null;
			}
			sCommitHash = sLine.substring(7);
			if(sCommitHash == null) { //validation
				return null;
			}
			
			//Get the name and email line
			sLine = b.readLine();
			if(sLine.contains("Merge:")) { //skip over merge line
				sLine = b.readLine();
			}
			String[] aNameEmail = sLine.substring(8).split("<"); //split on "<"
			String[] aName = aNameEmail[0].split(" "); //split on " "
			
			//Get the first name
			if(aName.length >= 1) {
				firstname = aName[0];
			}
			if(firstname == null) { //validation
				return null;
			}
			
			//Get the last name
			if(aName.length >= 2) {
				lastname = aName[1];
			}
			if(lastname == null) { //validation
				return null;
			}
			//System.out.println("Name: " + firstname + " " + lastname);
			
			//Get the email
			int nEmailIndex = aNameEmail.length - 1;
			if(nEmailIndex == 0) {
				sEmail = "";
			}
			else if(aNameEmail[nEmailIndex].length() == 0){
				sEmail = "";
			}
			else { //remove trailing ">"
				sEmail = aNameEmail[nEmailIndex].substring(0, aNameEmail[nEmailIndex].length()-1);
			}
			//Remove any trailing whitespace from the email
			if(sEmail.endsWith(" ")) {
				sEmail = sEmail.substring(0, sEmail.length()-1);
			}
			//System.out.println("Email: " + sEmail);
			if(sEmail == null) { //validation
				return null;
			}
			
			//Get the date line
			sDate = b.readLine().substring(8);
			//System.out.println("Date: " + sDate);
			//sDate = sDate.substring(0, sDate.length()-6);
			if(sDate == null) { //validation
				return null;
			}
			//sDate = sDate.substring(0, sDate.length()-6);
			SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
			uDate = formatter.parse(sDate);
			
			//Get the message
			b.readLine();
			while(true) {
				sLine = b.readLine();
				if(sLine == null) {
					break;
				}
				aMessage.add(sLine);
				if(sLine.isEmpty()) {
					break;
				}
			}
			//System.out.println("Message: " + aMessage.toString());
			
			//Check to see if another commit or changes are coming
			int readAheadLimitChars = 3000;
			b.mark(readAheadLimitChars); //save spot in reader
			if(b.readLine().contains("commit ")) {
				//System.out.println("found next commit early");
				b.reset(); //go back to saved spot
				Commit c = new Commit(sCommitHash, 
		  	  			  firstname, 
		  	  			  lastname, 
		  	  			  sEmail,
		  	  			  uDate,
		  	  			  aMessage, 
		  	  			  nNumFilesChanged, 
		  	  			  nNumInsertions, 
		  	  			  nNumDeletions);
				return c;
			}
			b.reset(); //go back to saved spot
			
			//Get the changes
			while(true) {
				String line = b.readLine();
				aChanges.add(line);
				if(line == null) {
					break;
				}
				if(line.isEmpty()) {
					break;
				}
			}
			
			//Get the last line of the changes
			String sStats = aChanges.get(aChanges.size()-2);
			//System.out.println("sStats: " + sStats);
			
			//Split the last stats line
			String[] aStats = sStats.split(" ");
			
			//Get files changed
			if(aStats.length >= 2) {
				nNumFilesChanged = Integer.parseInt(aStats[1]);
			}
			
			//Get insertions
			if(aStats.length >= 5) {
				nNumInsertions = Integer.parseInt(aStats[4]);
			}
			
			//Get deletions
			if(aStats.length >= 7) {
				nNumDeletions = Integer.parseInt(aStats[6]);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		//Create new commit
		Commit c = new Commit(sCommitHash, 
			  	  			  firstname, 
			  	  			  lastname, 
			  	  			  sEmail,
			  	  			  uDate,
			  	  			  aMessage, 
			  	  			  nNumFilesChanged, 
			  	  			  nNumInsertions, 
			  	  			  nNumDeletions);
		return c;
	}

	private static void delete(File file){ 
		for (File childFile : file.listFiles()) {
			if(childFile.isDirectory()) {
				delete(childFile);
			}
			else {
				if (!childFile.delete()) {
					try {
						throw new IOException();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		file.delete();
	}
	
	//first argument = sURL, second argument = sLang
	public static void main(String [] args) {		
		Runtime r = Runtime.getRuntime();
		String sURL = args[0];
		String sCommand = "";
		String sStats;
		String sGitDir = "repo_clone";
		Process p;
		String line;
		BufferedReader b;
		String sRecProjectsFolder = "/home/cs578user/Desktop/RecProjects";
		String sArchRecOutFolder = "/home/cs578user/Desktop/ArchRecOut";
		
		//Create directory to clone repo to
		File rc = new File(sGitDir);
		delete(rc);
		rc.mkdirs();
		
		try {
			
			//Clone the repo
			System.out.println("cloning repo");
			sCommand = "git clone " + sURL;
			p = r.exec(sCommand, null, rc);
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Get repo directory
			File[] flist = rc.listFiles();
			File rp = new File(flist[0].toString());
			
			//Get the commit stats
			System.out.println("parsing commit stats");
			sCommand = "git log --stat";
			p = r.exec(sCommand, null, rp);
			b = new BufferedReader(new InputStreamReader(p.getInputStream()));
			line = "";
			
			aCommits.clear();
			while(true) {
				Commit c = parseNextCommit(b);
				if(c == null) {
					break;
				}
				aCommits.add(c);
			}
	
			b.close();
			
		//Calculate the four commit epoch times
		System.out.println("calculating epoch times");
		Commit first_commit = aCommits.get(0);
		Commit last_commit = aCommits.get(aCommits.size()-1);
		long epoch_100_percent = first_commit.getEpoch();
		long epoch_0_percent = last_commit.getEpoch();
		long epoch_span = epoch_100_percent - epoch_0_percent;
		long epoch_25_percent = (long)(0.25 * (double)epoch_span) + epoch_0_percent;
		long epoch_50_percent = (long)(0.50 * (double)epoch_span) + epoch_0_percent;
		long epoch_75_percent = (long)(0.75 * (double)epoch_span) + epoch_0_percent;
		
		//Find the four closest actual commits.
		//Algorithm is off by at most one commit
		//which is insignificant in a densely populated commit history.
		commit_index_100_percent = 0;
		commit_index_75_percent = 0;
		commit_index_50_percent = 0;
		commit_index_25_percent = 0;
		for(int i=0; i<aCommits.size(); i++) {
			Commit c = aCommits.get(i);
			long epoch = c.getEpoch();
			
			if(commit_index_75_percent == 0 && epoch_75_percent >= epoch) {
				commit_index_75_percent = i;
			}
			if(commit_index_50_percent == 0 && epoch_50_percent >= epoch) {
				commit_index_50_percent = i;
			}
			if(commit_index_25_percent == 0 && epoch_25_percent >= epoch) {
				commit_index_25_percent = i;
			}
		}
		
		//Get the corresponding four commit hashes
		System.out.println("getting commit hashes");
		String hash_100_percent = aCommits.get(commit_index_100_percent).getHash();
		String hash_75_percent = aCommits.get(commit_index_75_percent).getHash();
		String hash_50_percent = aCommits.get(commit_index_50_percent).getHash();
		String hash_25_percent = aCommits.get(commit_index_25_percent).getHash();
		
		//Make 4 copies of the project to "RecProjects"
		System.out.println("cloning projects at various commit hashes");
			//Clear out whatever is already in "RecProjects"
			File f = new File(sRecProjectsFolder);
			File[] lf = f.listFiles();
			for(int i=0; i<lf.length; i++) {
				delete(lf[i]);
			}

			//100 percent
			f = new File(sRecProjectsFolder+"/REPOd");
			f.mkdir();
			FileUtils.copyDirectory(rp, f);
			
			//75 percent
			f = new File(sRecProjectsFolder+"/REPOc");
			f.mkdir();
			FileUtils.copyDirectory(rp, f);
			sCommand = "git reset --hard " + hash_75_percent;
			p = r.exec(sCommand, null, f);
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//50 percent
			f = new File(sRecProjectsFolder+"/REPOb");
			f.mkdir();
			FileUtils.copyDirectory(rp, f);
			sCommand = "git reset --hard " + hash_50_percent;
			p = r.exec(sCommand, null, f);
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//25 percent
			f = new File(sRecProjectsFolder+"/REPOa");
			f.mkdir();
			FileUtils.copyDirectory(rp, f);
			sCommand = "git reset --hard " + hash_25_percent;
			p = r.exec(sCommand, null, f);
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			
		//Delete any existing recoveries in "ArchRecOut"
		File fa = new File(sArchRecOutFolder);
		File[] lfa = fa.listFiles();
		for(int i=0; i<lfa.length; i++) {
			delete(lfa[i]);
		}
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
