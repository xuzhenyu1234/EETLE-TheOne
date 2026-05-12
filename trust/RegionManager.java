package trust;

import java.util.HashMap;
import java.util.Map;

import core.DTNHost;

/**
 * Maintains node region state for cross-region trust constraints.
 *
 * The first version uses a two-region split on the X coordinate. The manager
 * keeps home/current region, residence time, cross-region interaction count,
 * and the region factor later used by robust Leader election.
 */
public class RegionManager {
	private Map<Integer, Integer> homeRegionMap;
	private Map<Integer, Integer> currentRegionMap;
	private Map<Integer, Double> regionEnterTimeMap;
	private Map<Integer, Integer> crossRegionInteractionCountMap;

	private double regionSplitX = 2250.0;
	private double crossRegionWarmup = 900.0;
	private double regionPenaltyFactor = 0.5;
	private int minCrossRegionInteractionsForLeader = 5;

	public RegionManager() {
		this.homeRegionMap = new HashMap<Integer, Integer>();
		this.currentRegionMap = new HashMap<Integer, Integer>();
		this.regionEnterTimeMap = new HashMap<Integer, Double>();
		this.crossRegionInteractionCountMap =
				new HashMap<Integer, Integer>();
	}

	public int estimateRegion(DTNHost host) {
		if (host == null || host.getLocation() == null) {
			return 0;
		}
		if (host.getLocation().getX() < this.regionSplitX) {
			return 0;
		}
		return 1;
	}

	public void updateRegion(DTNHost host, double now) {
		if (host == null) {
			return;
		}

		int address = host.getAddress();
		int newRegion = estimateRegion(host);
		initializeIfMissing(address, newRegion, now);

		int oldRegion = getCurrentRegion(address);
		if (oldRegion != newRegion) {
			this.currentRegionMap.put(new Integer(address),
					new Integer(newRegion));
			this.regionEnterTimeMap.put(new Integer(address),
					new Double(now));
		}
	}

	public void updateForcedCrossRegion(DTNHost host, double now,
			double switchTime) {
		if (host == null) {
			return;
		}

		int address = host.getAddress();
		int homeRegion = estimateRegion(host);
		initializeIfMissing(address, homeRegion, now);

		if (now <= switchTime) {
			forceCurrentRegion(address, getHomeRegion(address), now);
			return;
		}

		int forcedRegion = getHomeRegion(address) == 0 ? 1 : 0;
		forceCurrentRegion(address, forcedRegion, switchTime);
	}

	private void initializeIfMissing(int address, int region, double now) {
		Integer key = new Integer(address);
		if (!this.homeRegionMap.containsKey(key)) {
			this.homeRegionMap.put(key, new Integer(region));
			this.currentRegionMap.put(key, new Integer(region));
			this.regionEnterTimeMap.put(key, new Double(now));
		}
	}

	private void forceCurrentRegion(int address, int region,
			double enterTime) {
		Integer key = new Integer(address);
		int oldRegion = getCurrentRegion(address);
		this.currentRegionMap.put(key, new Integer(region));
		if (!this.regionEnterTimeMap.containsKey(key) ||
				oldRegion != region) {
			this.regionEnterTimeMap.put(key, new Double(enterTime));
		}
	}

	public int getHomeRegion(int address) {
		Integer value = this.homeRegionMap.get(new Integer(address));
		if (value == null) {
			return -1;
		}
		return value.intValue();
	}

	public int getCurrentRegion(int address) {
		Integer value = this.currentRegionMap.get(new Integer(address));
		if (value == null) {
			return -1;
		}
		return value.intValue();
	}

	public boolean isCrossRegion(int address) {
		int home = getHomeRegion(address);
		int current = getCurrentRegion(address);
		return home >= 0 && current >= 0 && home != current;
	}

	public double getRegionResidenceTime(int address, double now) {
		Double enterTime = this.regionEnterTimeMap.get(new Integer(address));
		if (enterTime == null) {
			return 0.0;
		}
		double residence = now - enterTime.doubleValue();
		if (residence < 0.0) {
			return 0.0;
		}
		return residence;
	}

	public boolean isInCrossRegionWarmup(int address, double now) {
		return isCrossRegion(address) &&
				getRegionResidenceTime(address, now) < this.crossRegionWarmup;
	}

	public void recordCrossRegionInteraction(int address) {
		if (!isCrossRegion(address)) {
			return;
		}

		Integer key = new Integer(address);
		Integer count = this.crossRegionInteractionCountMap.get(key);
		if (count == null) {
			count = new Integer(0);
		}
		this.crossRegionInteractionCountMap.put(key,
				new Integer(count.intValue() + 1));
	}

	public int getCrossRegionInteractionCount(int address) {
		Integer count = this.crossRegionInteractionCountMap.get(
				new Integer(address));
		if (count == null) {
			return 0;
		}
		return count.intValue();
	}

	public double getRegionConstraintFactor(int address, int leaderRegion,
			double now) {
		int current = getCurrentRegion(address);
		if (current < 0 || leaderRegion < 0) {
			return 1.0;
		}
		if (isInCrossRegionWarmup(address, now)) {
			return 0.3;
		}
		if (current != leaderRegion) {
			return this.regionPenaltyFactor;
		}
		return 1.0;
	}

	public boolean canBeLeaderCandidate(int address, int leaderRegion,
			double now) {
		if (!isCrossRegion(address)) {
			return true;
		}
		if (isInCrossRegionWarmup(address, now)) {
			return false;
		}
		return getCrossRegionInteractionCount(address) >=
				this.minCrossRegionInteractionsForLeader;
	}

	public double getCrossRegionWarmup() {
		return this.crossRegionWarmup;
	}

	public void setRegionSplitX(double regionSplitX) {
		this.regionSplitX = regionSplitX;
	}

	public void setCrossRegionWarmup(double crossRegionWarmup) {
		if (crossRegionWarmup < 0.0) {
			this.crossRegionWarmup = 0.0;
		}
		else {
			this.crossRegionWarmup = crossRegionWarmup;
		}
	}

	public void setRegionPenaltyFactor(double regionPenaltyFactor) {
		if (regionPenaltyFactor < 0.0) {
			this.regionPenaltyFactor = 0.0;
		}
		else if (regionPenaltyFactor > 1.0) {
			this.regionPenaltyFactor = 1.0;
		}
		else {
			this.regionPenaltyFactor = regionPenaltyFactor;
		}
	}

	public void setMinCrossRegionInteractionsForLeader(double value) {
		if (value < 0.0) {
			this.minCrossRegionInteractionsForLeader = 0;
		}
		else {
			this.minCrossRegionInteractionsForLeader = (int)value;
		}
	}
}
