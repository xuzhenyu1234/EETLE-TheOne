/*
 * Trust report for EETLE local trust edges.
 */
package report;

import java.util.Collection;
import java.util.List;

import routing.EETLERouter;
import routing.MessageRouter;
import trust.TrustEdge;
import trust.TrustTable;
import trust.TrustVector;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Periodically outputs all local trust edges maintained by EETLERouter nodes.
 * This report is read-only: it never updates trust values or link state.
 */
public class TrustReport extends Report implements UpdateListener {
	private static final String HEADER =
			"time,evaluator,target,b,e,u,d,scalarTrust,successCount," +
			"failCount,uncertainForwardCount,pout,lastUpdateTime," +
			"lastDecayFactor";
	private static final double DEFAULT_INTERVAL = 100.0;

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;

	public TrustReport() {
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
		writeAllTrustEdges(hosts);
	}

	private void writeAllTrustEdges(List<DTNHost> hosts) {
		for (DTNHost host : hosts) {
			MessageRouter router = host.getRouter();
			if (!(router instanceof EETLERouter)) {
				continue;
			}

			EETLERouter eetle = (EETLERouter)router;
			TrustTable table = eetle.getTrustTable();
			Collection<TrustEdge> edges = table.getAllEdgesAsCollection();

			for (TrustEdge edge : edges) {
				writeTrustEdge(edge);
			}
		}
	}

	private void writeTrustEdge(TrustEdge edge) {
		TrustVector v = edge.vector;
		String line = format(getSimTime()) + "," +
				edge.getEvaluatorId() + "," +
				edge.getTargetId() + "," +
				format(v.b) + "," +
				format(v.e) + "," +
				format(v.u) + "," +
				format(v.d) + "," +
				format(edge.scalarTrust) + "," +
				format(edge.successCount) + "," +
				format(edge.failCount) + "," +
				format(edge.uncertainForwardCount) + "," +
				format(edge.lastPout) + "," +
				format(edge.lastUpdateTime) + "," +
				format(edge.lastDecayFactor);

		write(line);
	}
}
