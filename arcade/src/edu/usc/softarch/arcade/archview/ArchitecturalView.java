/**
 *
 */
package edu.usc.softarch.arcade.archview;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.thoughtworks.xstream.XStream;

import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.util.FileUtil;

/**
 * @author daniellink
 *
 */
public class ArchitecturalView {
	private String name;
	private ArrayList<Integer> smellClustersList;
	private HashMap<String, Set<String>> smellClustersMap;
	private Set<ConcernCluster> clusters;
	private Config config;
	private File configFile;
	private long recoveryStartTime;
	private long recoveryFinishTime;

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(ArchitecturalView.class);

	public void startTimer() {
		recoveryStartTime = System.currentTimeMillis();
	}

	public long getRecoveryTime() {
		recoveryFinishTime = System.currentTimeMillis();
		return recoveryFinishTime - recoveryStartTime;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(final Config config_) {
		config = config_;
	}

	public Set<ConcernCluster> getClusters() {
		return clusters;
	}

	public void setClusters(final Set<ConcernCluster> clusters) {
		this.clusters = clusters;
	}

	public String getEntitiesByNameAsString(final String clusterName) {
		logger.entry(clusterName);
		final Set<String> entities = getEntitiesByName(clusterName);
		if (entities.equals(null)) {
			logger.traceExit();
			return null;
		}
		final String entitiesString = entities.toString();
		logger.traceExit();
		return entitiesString.substring(1, entitiesString.length() - 1);
	}

	public String getEntitiesLongestCommonSubstring(final String clusterName) {
		logger.entry(clusterName);
		final Set<String> entities = getEntitiesByName(clusterName);
		logger.traceExit();
		return edu.usc.softarch.arcade.util.StringUtil
				.longestCommonSubstring(entities.toArray(new String[entities.size()]));
	}

	private Set<String> getEntitiesByName(final String clusterName) {
		logger.entry(clusterName);
		for (final ConcernCluster currentCluster : clusters) {
			if (currentCluster.getName().equals(clusterName)) {
				logger.traceExit();
				return currentCluster.getEntities();
			}
		}
		logger.traceExit();
		return null;
	}

	public HashMap<String, Set<String>> getSmellClusters() {
		return smellClustersMap;
	}

	public void setSmellClusters(final HashMap<String, Set<String>> smellClusters) {
		smellClustersMap = smellClusters;
	}

	public ArrayList<Integer> getSmellyClusters() {
		return smellClustersList;
	}

	public void setSmellyClusters(final ArrayList<Integer> smellyClusters) {
		smellClustersList = smellyClusters;
	}

	public void setSmellyClusters(final Set<String> smellyClustersSet) {
		logger.entry(smellyClustersSet);
		smellClustersList = new ArrayList<Integer>();
		for (final String s : smellyClustersSet) {
			smellClustersList.add(Integer.parseInt(s));
		}
		logger.traceExit();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ArchitecturalView() {
		logger.traceEntry();
		name = "noName";
		config = new Config();
		readConfigFromFile();
		logger.traceExit();
	}

	public ArchitecturalView(final String sysName) {
		logger.entry(sysName);
		name = sysName;
		config = new Config();
		readConfigFromFile();
		logger.traceExit();
	}

	public boolean readConfigFromFile() {
		logger.traceEntry();
		configFile = FileUtil.checkFile(Config.getConfigDataFilename(), false, false);
		if (configFile.exists()) {
			final XStream xstream = new XStream();
			try {
				config = (Config) xstream.fromXML(configFile);
				Config.setStoppingCriterion(config.getInstanceStoppingCriterion());
			} catch (final Exception e) {
				System.out.println("Unable to read config file, please delete file named "
						+ configFile.getAbsolutePath() + " and try again!");
				// e.printStackTrace();
			}
			logger.traceExit();
			return true;
		} else {
			logger.traceExit();
			return false;
		}
	}
}
