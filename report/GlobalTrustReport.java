/*
 * Global trust report for EETLE linear attention fusion.
 */
package report;

import java.util.Collection;
import java.util.List;

import routing.EETLERouter;
import trust.GlobalTrustEntry;
import trust.GlobalTrustManager;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Periodically outputs global trust results fused by the Leader.
 * Reads from the static GlobalTrustManager shared across all EETLERouter instances.
 */
public class GlobalTrustReport extends Report implements UpdateListener {
	private static final String HEADER =
			"time,leader,target,globalTrust,b,e,u,d,recommendationCount," +
			"lastUpdateTime";
	private static final int DEFAULT_LEADER_ADDRESS = 0;
	private static final double DEFAULT_INTERVAL = 100.0;

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;

	public GlobalTrustReport() {
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
		writeGlobalTrustEntries();
	}

	private void writeGlobalTrustEntries() {
		GlobalTrustManager gtm = EETLERouter.getGlobalTrustManager();
		if (gtm == null) {
			return;
		}

		Collection<GlobalTrustEntry> entries = gtm.getAllGlobalTrustEntries();
		for (GlobalTrustEntry entry : entries) {
			String line = format(getSimTime()) + "," +
					DEFAULT_LEADER_ADDRESS + "," +
					entry.getTargetAddress() + "," +
					format(entry.getGlobalTrust()) + "," +
					format(entry.getFusedBelief()) + "," +
					format(entry.getFusedEnvUncertainty()) + "," +
					format(entry.getFusedCognitiveUncertainty()) + "," +
					format(entry.getFusedDisbelief()) + "," +
					entry.getRecommendationCount() + "," +
					format(entry.getLastUpdateTime());
			write(line);
		}
	}
}
