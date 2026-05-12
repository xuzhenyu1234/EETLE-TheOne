/*
 * Attack-aware EETLE trust report.
 */
package report;

import java.util.List;

import routing.EETLERouter;
import routing.MessageRouter;
import trust.AttackType;
import trust.TrustVector;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Periodically outputs per-node attack counters from EETLERouter.
 */
public class EETLETrustReport extends Report implements UpdateListener {
	private static final String HEADER =
			"time,node,attackType,homeRegion,currentRegion," +
			"totalAttackAttempts,droppedByAttack," +
			"blackholeDrops,onOffDrops,envCamouflageDrops,crossRegionDrops," +
			"falseEventsInjected,falseEventCount,debugSelfScalarTrust," +
			"debugSelfBelief," +
			"envUncertainty,cognitiveUncertainty,disbelief";
	private static final double DEFAULT_INTERVAL = 100.0;

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;

	public EETLETrustReport() {
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
		writeAttackCounters(hosts);
	}

	private void writeAttackCounters(List<DTNHost> hosts) {
		for (DTNHost host : hosts) {
			MessageRouter router = host.getRouter();
			if (!(router instanceof EETLERouter)) {
				continue;
			}

			EETLERouter eetle = (EETLERouter)router;
			AttackType type = eetle.getAttackType();
			TrustVector vector = eetle.getDebugSelfTrustVector();
			String line = format(getSimTime()) + "," +
					host.toString() + "," +
					type.toString() + "," +
					eetle.getHomeRegion() + "," +
					eetle.getCurrentRegion() + "," +
					eetle.getAttackAttempts() + "," +
					eetle.getDroppedByAttack() + "," +
					eetle.getBlackholeDrops() + "," +
					eetle.getOnOffDrops() + "," +
					eetle.getEnvCamouflageDrops() + "," +
					eetle.getCrossRegionDrops() + "," +
					eetle.getFalseEventsInjected() + "," +
					eetle.getFalseEventCount() + "," +
					format(eetle.getDebugSelfScalarTrust()) + "," +
					format(vector.getBelief()) + "," +
					format(vector.getEnvUncertainty()) + "," +
					format(vector.getCognitiveUncertainty()) + "," +
					format(vector.getDisbelief());
			write(line);
		}
	}
}
