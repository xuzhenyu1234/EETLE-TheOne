/*
 * Region state report for EETLE cross-region modeling.
 */
package report;

import java.util.List;

import routing.EETLERouter;
import routing.MessageRouter;
import trust.AttackType;
import trust.RegionManager;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Periodically outputs region state used by cross-region attack modeling and
 * later Leader election constraints.
 */
public class RegionReport extends Report implements UpdateListener {
	private static final String HEADER =
			"time,node,attackType,homeRegion,currentRegion,isCrossRegion," +
			"regionResidenceTime,crossRegionWarmup," +
			"crossRegionInteractionCount,regionConstraintFactor," +
			"canBeLeaderCandidate";
	private static final int DEFAULT_LEADER_ADDRESS = 0;
	private static final double DEFAULT_INTERVAL = 100.0;

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;

	public RegionReport() {
		Settings settings = getSettings();
		if (settings.contains(INTERVAL_SETTING)) {
			this.interval = settings.getDouble(INTERVAL_SETTING);
		}
		init();
	}

	@Override
	protected void init() {
		super.init();
		write(HEADER);
	}

	public void updated(List<DTNHost> hosts) {
		double now = getSimTime();
		if (this.lastReportTime >= 0 &&
				now - this.lastReportTime < this.interval) {
			return;
		}

		this.lastReportTime = now;
		writeRegions(hosts);
	}

	private void writeRegions(List<DTNHost> hosts) {
		RegionManager manager = EETLERouter.getRegionManager();
		int leaderRegion = manager.getCurrentRegion(DEFAULT_LEADER_ADDRESS);

		for (DTNHost host : hosts) {
			MessageRouter router = host.getRouter();
			if (!(router instanceof EETLERouter)) {
				continue;
			}

			EETLERouter eetle = (EETLERouter)router;
			AttackType type = eetle.getAttackType();
			int address = host.getAddress();
			double now = getSimTime();

			String line = format(now) + "," +
					host.toString() + "," +
					type.toString() + "," +
					manager.getHomeRegion(address) + "," +
					manager.getCurrentRegion(address) + "," +
					manager.isCrossRegion(address) + "," +
					format(manager.getRegionResidenceTime(address, now)) + "," +
					format(manager.getCrossRegionWarmup()) + "," +
					manager.getCrossRegionInteractionCount(address) + "," +
					format(manager.getRegionConstraintFactor(address,
							leaderRegion, now)) + "," +
					manager.canBeLeaderCandidate(address, leaderRegion, now);
			write(line);
		}
	}
}
