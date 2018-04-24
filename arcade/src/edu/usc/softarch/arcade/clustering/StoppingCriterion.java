package edu.usc.softarch.arcade.clustering;

/**
 *
 * @author daniellink
 *
 *         Interface for a stopping criterion, must implement notReadyToStop()
 */
public interface StoppingCriterion {
	public boolean notReadyToStop();
}
