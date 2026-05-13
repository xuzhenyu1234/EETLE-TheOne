/*
 * Average neighbor degree report for density experiments.
 */
package report;

import java.util.List;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Periodically reports the instantaneous connection degree of the network.
 * The degree of a UAV is the number of currently connected neighbors.
 */
public class NeighborDegreeReport extends Report implements UpdateListener {
	private static final String HEADER =
			"time,nodeCount,averageNeighborDegree,minNeighborDegree," +
			"maxNeighborDegree";
	private static final double DEFAULT_INTERVAL = 100.0;

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;

	public NeighborDegreeReport() {
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
		writeNeighborDegree(hosts);
	}

	private void writeNeighborDegree(List<DTNHost> hosts) {
		if (hosts == null || hosts.size() == 0) {
			write(format(getSimTime()) + ",0,0.0000,0,0");
			return;
		}

		int minDegree = Integer.MAX_VALUE;
		int maxDegree = 0;
		int sumDegree = 0;

		for (DTNHost host : hosts) {
			int degree = host.getConnections().size();
			sumDegree += degree;
			if (degree < minDegree) {
				minDegree = degree;
			}
			if (degree > maxDegree) {
				maxDegree = degree;
			}
		}

		double averageDegree = sumDegree / (double)hosts.size();
		write(format(getSimTime()) + "," +
				hosts.size() + "," +
				format(averageDegree) + "," +
				minDegree + "," +
				maxDegree);
	}
}
