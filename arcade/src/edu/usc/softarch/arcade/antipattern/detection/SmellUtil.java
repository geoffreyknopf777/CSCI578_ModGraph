package edu.usc.softarch.arcade.antipattern.detection;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.xstream.XStream;

import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.util.FileUtil;

public class SmellUtil {
	public static String getSmellAbbreviation(final Smell smell) {
		if (smell instanceof BcoSmell) {
			return "bco";
		} else if (smell instanceof SpfSmell) {
			return "spf";
		} else if (smell instanceof BdcSmell) {
			return "bdc";
		} else if (smell instanceof BuoSmell) {
			return "buo";
		} else {
			return "invalid smell type";
		}
	}

	public static Set<ConcernCluster> getSmellClusters(final Smell smell){
		return smell.clusters;
	}
	
	public static Class[] getSmellClasses() {
		final Class[] smellClasses = { BcoSmell.class, BdcSmell.class, BuoSmell.class, SpfSmell.class };
		return smellClasses;
	}

	public static Set<Smell> deserializeDetectedSmells(final String detectedSmellsGtFileName) {
		return deserializeDetectedSmells(FileUtil.checkFile(detectedSmellsGtFileName, false, false));
	}

	public static Set<Smell> deserializeDetectedSmells(final File detectedSmellsGtFile) {
		final XStream xstream = new XStream();
		String xml = null;
		xml = FileUtil.readFile(detectedSmellsGtFile, StandardCharsets.UTF_8);
		final Set<Smell> detectedGtSmells = (Set<Smell>) xstream.fromXML(xml);
		return detectedGtSmells;
	}
	
	public static Set<String> getSmellNames(Set<Smell> smells){
		Set<String> smellNames = new HashSet<String>();
		for(Smell smell: smells){
			smellNames.add(getSmellAbbreviation(smell));
		}
		return smellNames;
	}
}
