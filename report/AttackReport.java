/*
 * Per-action attack report for EETLE experiments.
 */
package report;

import java.util.List;

import routing.EETLERouter;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Outputs one row for each attack action recorded by EETLERouter.
 */
public class AttackReport extends Report implements UpdateListener {
	private static final String HEADER =
			"time,nodeId,attackType,action,targetId,messageId,pout," +
			"homeRegion,currentRegion,isCrossRegion";
	private static final double DEFAULT_INTERVAL = 100.0;

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;

	public AttackReport() {
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
		writeNewRecords();
	}

	private void writeNewRecords() {
		List<String> records = EETLERouter.drainAttackRecords();
		for (int i = 0; i < records.size(); i++) {
			write(records.get(i));
		}
	}
}
