package ModGraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class ARC_RSF_Formatter {
	
	public static final String DEPS_RSF_FILE_PATH = "hadoop-0.14.2_deps.rsf";

	public static void main(String[] args) throws Exception{
		
		File originalRSF = new File(DEPS_RSF_FILE_PATH); //TODO Consider how we'll know the name of this file
		Map<String,List<String>> map = new HashMap<String,List<String>>();
		try (Scanner scanner = new Scanner(originalRSF)){
			while(scanner.hasNext()) {
				 
				String[] entryParts = scanner.nextLine().split(" ");
				String key = entryParts[1];
				String currentDependency = entryParts[2];
				if (!map.containsKey(key)) {
					List<String> dependencies = new ArrayList<String>();
					dependencies.add(currentDependency);
					map.put(key, dependencies);
				}
				else if (!map.containsKey(currentDependency)) {
					//Bundles code requires that every dependency also be a key in the map
					//TO TRACK - Shouldn't need this, it's done in the D3.js code
					//List<String> placeholder = new ArrayList<String>();
					//	map.put(currentDependency, placeholder);
				}
				else {
					List<String> dependencies = map.get(key);
					dependencies.add(currentDependency);
				}
			}
		}
		System.out.println("Building JSON File...");
		buildJsonFromMap(map);
		System.out.println("ARC JSON data - Complete!");
	}
	private static void buildJsonFromMap(Map map) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter("ARC_Data.json", true));
		//StringBuffer jsonData = new StringBuffer();
		writer.append('[');
		Iterator<String> keySetIter = map.keySet().iterator();
		while (keySetIter.hasNext()) {
			String key = keySetIter.next();
			writer.append('{');
			writer.append("\"name\"");
			writer.append(":");
			writer.append("\"" + key + "\"");
			writer.append(",");
			
			writer.append("\"size\":1,"); //Dummy placeholder
			
			writer.append("\"imports\"");
			writer.append(":");
			writer.append("[");
			Iterator<String> valuesIter = ((List<String>) map.get(key)).iterator();
			while (valuesIter.hasNext()) {
				writer.append("\"" + valuesIter.next() + "\"");
				if (valuesIter.hasNext()) {
					writer.append(",");
				}
			}
			writer.append("]");
			writer.append("}");
			if (keySetIter.hasNext()) {
				writer.append(",");
			}
		}
		writer.append(']');
		writer.close();
		//return jsonData.toString();
	}
}
