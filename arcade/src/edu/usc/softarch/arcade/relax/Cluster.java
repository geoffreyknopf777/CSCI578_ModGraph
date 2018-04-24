/**
 *
 */
package edu.usc.softarch.arcade.relax;

import java.util.ArrayList;

/**
 * @author daniellink
 *
 */
public class Cluster {
	private ArrayList<CodeEntity> entities;
	private int id;
	private String name;

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(final int id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @return the entities
	 */
	public ArrayList<CodeEntity> getEntities() {
		return entities;
	}

	/**
	 * @param entities
	 *            the entities to set
	 */
	public void setEntities(final ArrayList<CodeEntity> entities) {
		this.entities = entities;
	}

	public Cluster() {
		entities = new ArrayList<>();
	}

	@Override
	public String toString() {
		String outputString = "Cluster id = " + id + " Cluster Name = " + name + " Entities = \n";
		for (final CodeEntity c : entities) {
			outputString += c.toString();
		}
		return outputString;
	}
}
