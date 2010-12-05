package org.geopublishing;

public enum Configure {
	/**
	 * Only setup the first feature/coverages type available in the data
	 * store/coveragestore. This is the default value.
	 **/
	first,
	/**
	 * Do not configure any feature types/coverages.
	 */
	none,
	/**
	 * cnfigure all featuretypes/coverages.
	 */
	all,
}
