/*
 * Threshold sensitivity report for EETLE malicious detection.
 */
package report;

import java.util.List;

import routing.EETLERouter;
import routing.MessageRouter;
import trust.AttackType;
import trust.GlobalTrustManager;
import trust.TrustEdge;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Evaluates the same detection rule as DetectionReport for a grid of
 * malicious-trust and environmental-risk thresholds.
 */
public class DetectionThresholdSweepReport extends Report
		implements UpdateListener {
	private static final String HEADER =
			"time,maliciousThreshold,envRiskThreshold,TP,FP,FN,TN," +
			"precision,recall,f1,fpr";
	private static final String EETLE_NS = "EETLERouter";
	private static final String MIN_ENV_RISK_EVIDENCE_SETTING =
			"minEnvRiskEvidence";
	private static final String ENABLE_ENV_RISK_DETECTION_SETTING =
			"enableEnvRiskDetection";
	private static final double DEFAULT_INTERVAL = 100.0;
	private static final int DEFAULT_MIN_ENV_RISK_EVIDENCE = 3;
	private static final double[] THRESHOLDS =
			{0.55, 0.60, 0.65, 0.70, 0.75};

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;
	private int minEnvRiskEvidence = DEFAULT_MIN_ENV_RISK_EVIDENCE;
	private boolean enableEnvRiskDetection = true;

	public DetectionThresholdSweepReport() {
		Settings settings = getSettings();
		if (settings.contains(INTERVAL_SETTING)) {
			this.interval = settings.getDouble(INTERVAL_SETTING);
		}

		Settings eetleSettings = new Settings(EETLE_NS);
		if (eetleSettings.contains(MIN_ENV_RISK_EVIDENCE_SETTING)) {
			this.minEnvRiskEvidence = eetleSettings.getInt(
					MIN_ENV_RISK_EVIDENCE_SETTING);
		}
		if (eetleSettings.contains(ENABLE_ENV_RISK_DETECTION_SETTING)) {
			this.enableEnvRiskDetection = eetleSettings.getBoolean(
					ENABLE_ENV_RISK_DETECTION_SETTING);
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
		writeSweep(hosts);
	}

	private void writeSweep(List<DTNHost> hosts) {
		GlobalTrustManager manager = EETLERouter.getGlobalTrustManager();
		if (manager == null) {
			return;
		}

		for (int i = 0; i < THRESHOLDS.length; i++) {
			for (int j = 0; j < THRESHOLDS.length; j++) {
				double maliciousThreshold = THRESHOLDS[i];
				double envRiskThreshold = THRESHOLDS[j];
				Counts counts = calculateCounts(hosts, manager,
						maliciousThreshold, envRiskThreshold);
				writeCounts(maliciousThreshold, envRiskThreshold, counts);
			}
		}
	}

	private Counts calculateCounts(List<DTNHost> hosts,
			GlobalTrustManager manager, double maliciousThreshold,
			double envRiskThreshold) {
		Counts counts = new Counts();

		for (int i = 0; i < hosts.size(); i++) {
			DTNHost host = hosts.get(i);
			MessageRouter router = host.getRouter();
			if (!(router instanceof EETLERouter)) {
				continue;
			}

			EETLERouter eetle = (EETLERouter)router;
			boolean actualMalicious =
					eetle.getAttackType() != AttackType.NORMAL;
			double globalTrust = manager.getGlobalTrust(host.getAddress());
			EnvRiskEvidence envEvidence = calculateEnvRiskEvidence(hosts,
					host.getAddress());
			boolean predictedMalicious = globalTrust < maliciousThreshold ||
					(this.enableEnvRiskDetection &&
					envEvidence.risk >= envRiskThreshold &&
					envEvidence.totalFailureCount >=
							this.minEnvRiskEvidence);
			counts.add(actualMalicious, predictedMalicious);
		}

		return counts;
	}

	private EnvRiskEvidence calculateEnvRiskEvidence(List<DTNHost> hosts,
			int targetAddress) {
		EnvRiskEvidence evidence = new EnvRiskEvidence();
		String targetId = String.valueOf(targetAddress);

		for (int i = 0; i < hosts.size(); i++) {
			MessageRouter router = hosts.get(i).getRouter();
			if (!(router instanceof EETLERouter)) {
				continue;
			}

			EETLERouter evaluatorRouter = (EETLERouter)router;
			for (TrustEdge edge :
					evaluatorRouter.getTrustTable().getAllEdgesAsCollection()) {
				if (!targetId.equals(edge.getTargetId())) {
					continue;
				}
				if (edge.getEvaluatorId().equals(edge.getTargetId())) {
					continue;
				}
				evidence.highEnvFailureCount += edge.getHighEnvFailureCount();
				evidence.highEnvTotalCount += edge.getHighEnvTotalCount();
				evidence.totalFailureCount += edge.getTotalFailureCount();
			}
		}

		double failureConcentration = evidence.highEnvFailureCount /
				Math.max(1.0, evidence.totalFailureCount);
		double highEnvConcentration = evidence.highEnvFailureCount /
				Math.max(1.0, evidence.highEnvTotalCount);
		evidence.risk = Math.max(failureConcentration,
				highEnvConcentration);
		if (evidence.risk > 1.0) {
			evidence.risk = 1.0;
		}
		return evidence;
	}

	private void writeCounts(double maliciousThreshold,
			double envRiskThreshold, Counts counts) {
		double precision = safeDivide(counts.tp, counts.tp + counts.fp);
		double recall = safeDivide(counts.tp, counts.tp + counts.fn);
		double f1 = precision + recall <= 0.0 ? 0.0 :
				2.0 * precision * recall / (precision + recall);
		double fpr = safeDivide(counts.fp, counts.fp + counts.tn);

		write(format(getSimTime()) + "," +
				format(maliciousThreshold) + "," +
				format(envRiskThreshold) + "," +
				counts.tp + "," +
				counts.fp + "," +
				counts.fn + "," +
				counts.tn + "," +
				format(precision) + "," +
				format(recall) + "," +
				format(f1) + "," +
				format(fpr));
	}

	private double safeDivide(double numerator, double denominator) {
		if (denominator <= 0.0) {
			return 0.0;
		}
		return numerator / denominator;
	}

	private static class Counts {
		int tp;
		int fp;
		int fn;
		int tn;

		void add(boolean actualMalicious, boolean predictedMalicious) {
			if (actualMalicious && predictedMalicious) {
				this.tp++;
			}
			else if (!actualMalicious && predictedMalicious) {
				this.fp++;
			}
			else if (actualMalicious) {
				this.fn++;
			}
			else {
				this.tn++;
			}
		}
	}

	private static class EnvRiskEvidence {
		double highEnvFailureCount;
		double highEnvTotalCount;
		double totalFailureCount;
		double risk;
	}
}
