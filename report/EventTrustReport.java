package report;

import java.util.Collection;
import java.util.List;

import routing.EETLERouter;
import trust.EventTrustManager;
import trust.EventTrustResult;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Reports event consensus and semantic trust outcomes.
 */
public class EventTrustReport extends Report implements UpdateListener {
	private static final String HEADER =
			"time,eventId,evaluator,target,reporter,reportedState,realState," +
			"consensusState,consensusProbability,consistent,falseReport," +
			"uncertain,reporterGlobalTrust,eventEvaluatorCount," +
			"appliedPenalty,appliedReward,reward,penalty";
	private static final double DEFAULT_INTERVAL = 100.0;

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;
	private int writtenCount = 0;

	public EventTrustReport() {
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
		writeNewResults();
	}

	private void writeNewResults() {
		EventTrustManager manager = EETLERouter.getEventTrustManager();
		Collection<EventTrustResult> results = manager.getResultHistory();
		int index = 0;
		for (EventTrustResult result : results) {
			if (index++ < this.writtenCount) {
				continue;
			}
			write(format(result.getTimestamp()) + "," +
					result.getEventId() + "," +
					result.getEvaluatorAddress() + "," +
					result.getTargetAddress() + "," +
					result.getTargetAddress() + "," +
					result.getReportedState() + "," +
					result.getRealState() + "," +
					result.getConsensusState() + "," +
					format(result.getConsensusProbability()) + "," +
					result.isAgreement() + "," +
					result.isFalseReport() + "," +
					result.isUncertain() + "," +
					format(result.getReporterGlobalTrust()) + "," +
					result.getEventEvaluatorCount() + "," +
					result.isAppliedPenalty() + "," +
					result.isAppliedReward() + "," +
					format(result.getReward()) + "," +
					format(result.getPenalty()));
		}
		this.writtenCount = results.size();
	}
}
