package ModGraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

public class ModifiabilityCalculator {
	private static ArrayList<Commit> aCommits;
	private static ArrayList<Double> aModifiabilities = new ArrayList<Double>();
	private static ArrayList<Long> aEpochs = new ArrayList<Long>();
	private static Long epoch_25;
	private static Long epoch_50;
	private static Long epoch_75;
	private static Long epoch_100;
	
	private static Double min(ArrayList<Double> aModifiabilities) {
		Double d = Double.POSITIVE_INFINITY;
		for(int i=0; i<aModifiabilities.size(); i++) {
			Double mod = aModifiabilities.get(i);
			if(mod < d) {
				d = mod;
			}
		}
		return d;
	}
	
	private static Double max(ArrayList<Double> aModifiabilities) {
		Double d = Double.MIN_NORMAL;
		for(int i=0; i<aModifiabilities.size(); i++) {
			Double mod = aModifiabilities.get(i);
			if(mod > d) {
				d = mod;
			}
		}
		return d;	
	}
	
	private static void normalize(ArrayList<Double> aModifiabilities, Double min, Double max) {
		for(int i=0; i<aModifiabilities.size(); i++) {
			Double mod = aModifiabilities.get(i);
			mod = 100*(mod-min)/(max-min);
			aModifiabilities.set(i, mod);
		}
	}
	
	private static void logscale(ArrayList<Double> aModifiabilities) {
		for(int i=0; i<aModifiabilities.size(); i++) {
			Double mod = aModifiabilities.get(i);
			Double power = 10.0;
			mod = Math.pow(mod, 1/power)*100/Math.pow(100, 1/power);
			aModifiabilities.set(i, mod);
		}
	}
	
	public static void main(String[] args) {
		
		//Calculate modifiability values from the parsed git commits
		calculateModifiability();
		
		//Write modifiability values to a JSON file
		writeJson();
		
	}

	public static void calculateModifiability() {
		//Get reference to list of commits
		//Assumes GitCommitParser.main() has already been called
		aCommits = GitCommitParser.getCommits();
		
		//Get epoch times
		epoch_25 = aCommits.get(GitCommitParser.commit_index_25_percent).getEpoch();
		epoch_50 = aCommits.get(GitCommitParser.commit_index_50_percent).getEpoch();
		epoch_75 = aCommits.get(GitCommitParser.commit_index_75_percent).getEpoch();
		epoch_100 = aCommits.get(GitCommitParser.commit_index_100_percent).getEpoch();
		
		//Iterate over each commit and calculate a modifiability value
		System.out.println("calculating modifiability values");
		aModifiabilities.clear();
		aEpochs.clear();
		for(int i=0; i<aCommits.size()-1; i++) { //skip first commit because last commit time is invalid
			double work;
			double files_changed;
			double insertions;
			double deletions;
			double delta_commit_ms;
			Double modifiability;
			Long epoch_ms;
			
			//Get the current commit
			Commit c = aCommits.get(i);
			
			epoch_ms = c.getEpoch();
			files_changed = c.getNumFilesChanged();
			insertions = c.getNumInsertions();
			deletions = c.getNumDeletions();
			work = files_changed + insertions + deletions;
			Commit last_commit = aCommits.get(i+1);
			Long last_epoch_ms = last_commit.getEpoch();
			delta_commit_ms = Math.abs(epoch_ms.doubleValue() - last_epoch_ms.doubleValue());
			if(delta_commit_ms != 0) { //avoid division by zero
				modifiability = work / delta_commit_ms;
			}
			else {
				modifiability = 0.0;
			}
			if(modifiability < 0) {
				System.out.println("Modifiability values less than 0" + modifiability.toString());
			}
			
			aModifiabilities.add(modifiability);
			aEpochs.add(epoch_ms);
		}
		
		//Normalize modifiabilities to %
		Double max = max(aModifiabilities);
		Double min = min(aModifiabilities);
		normalize(aModifiabilities, min, max);
		logscale(aModifiabilities);
	}
	
	private static void writeJson() {
		//Write modifiability values to .json file
		System.out.println("writing modifiability values to .json file");
		Writer writer = null;

		try {
		    writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("modifiability.json"), "utf-8"));
		    
		    //Write opening bracket
		    writer.write("{\n");
		    
		    //Write modifiability array
		    writer.write("\"modifiability\":[");
		    for(int i=0; i<aModifiabilities.size(); i++) {
		    	Double modifiability = aModifiabilities.get(i).doubleValue();
		    	writer.write("\"" + modifiability.intValue());
		    	if(i < aModifiabilities.size() - 1) {
		    		writer.write("\", ");
		    	}
		    }
		    writer.write("]\n");
		    
		    //Write epochs array
		    writer.write("\"epochs\":[");
		    for(int i=0; i<aEpochs.size(); i++) {
		    	Long epoch = aEpochs.get(i).longValue();
		    	writer.write("\"" + epoch.toString());
		    	if(i < aEpochs.size() - 1) {
		    		writer.write("\", ");
		    	}
		    }
		    writer.write("]\n");
		    
		    //Write epoch_25
		    writer.write("\"epoch_25\":[");
		    writer.write("\"" + epoch_25.toString() + "\"");
		    writer.write("]\n");
		    
		    //Write epoch_50
		    writer.write("\"epoch_50\":[");
		    writer.write("\"" + epoch_50.toString() + "\"");
		    writer.write("]\n");
		    
		    //Write epoch_75
		    writer.write("\"epoch_75\":[");
		    writer.write("\"" + epoch_75.toString() + "\"");
		    writer.write("]\n");
		    
		    //Write epoch_100
		    writer.write("\"epoch_100\":[");
		    writer.write("\"" + epoch_100.toString() + "\"");
		    writer.write("]\n");
		    
		    //Write closing bracket
		    writer.write("}");
		    
		} catch (IOException ex) {
		    // Report
		} finally {
		   try {writer.close();} catch (Exception ex) {}
		}
	}
	
}
