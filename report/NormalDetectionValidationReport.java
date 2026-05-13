/*
 * Normal-node false positive validation for high-environment experiments.
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
 * Outputs normal-node false positives and average normal trust/risk.
 */
public class NormalDetectionValidationReport extends Report
		implements UpdateListener {
	private static final String HEADER =
			"time,normalFalsePositiveCount,FPR,avgNormalGlobalTrust," +
			"avgNormalEnvRisk";
	private static final String EETLE_NS = "EETLERouter";
	private static final String THRESHOLD_SETTING =
			"maliciousDetectionThreshold";
	private static final String ENV_RISK_THRESHOLD_SETTING =
			"envRiskDetectionThreshold";
	private static final String MIN_ENV_RISK_EVIDENCE_SETTING =
			"minEnvRiskEvidence";
	private static final String ENABLE_ENV_RISK_DETECTION_SETTING =
			"enableEnvRiskDetection";
	private static final double DEFAULT_INTERVAL = 100.0;
	private static final double DEFAULT_THRESHOLD = 0.65;
	private static final double DEFAULT_ENV_RISK_THRESHOLD = 0.65;
	private static final int DEFAULT_MIN_ENV_RISK_EVIDENCE = 3;

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;
	private double threshold = DEFAULT_THRESHOLD;
	private double envRiskThreshold = DEFAULT_ENV_RISK_THRESHOLD;
	private int minEnvRiskEvidence = DEFAULT_MIN_ENV_RISK_EVIDENCE;
	private boolean enableEnvRiskDetection = true;

	public NormalDetectionValidationReport() {
		Settings settings = getSettings();
		if (settings.contains(INTERVAL_SETTING)) {
			this.interval = settings.getDouble(INTERVAL_SETTING);
		}

		Settings eetleSettings = new Settings(EETLE_NS);
		if (eetleSettings.contains(THRESHOLD_SETTING)) {
			this.threshold = eetleSettings.getDouble(THRESHOLD_SETTING);
		}
		if (eetleSettings.contains(ENV_RISK_THRESHOLD_SETTING)) {
			this.envRiskThreshold = eetleSettings.getDouble(
					ENV_RISK_THRESHOLD_SETTING);
		}
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
		writeValidation(hosts);
	}

	private void writeValidation(List<DTNHost> hosts) {
		GlobalTrustManager manager = EETLERouter.getGlobalTrustManager();
		if (manager == null) {
			return;
		}

		int normalCount = 0;
		int falsePositiveCount = 0;
		double globalTrustSum = 0.0;
		double envRiskSum = 0.0;

		for (int i = 0; i < hosts.size(); i++) {
			DTNHost host = hosts.get(i);
			MessageRouter router = host.getRouter();
			if (!(router instanceof EETLERouter)) {
				continue;
			}

			EETLERouter eetle = (EETLERouter)router;
			if (eetle.getAttackType() != AttackType.NORMAL) {
				continue;
			}

			double globalTrust = manager.getGlobalTrust(host.getAddress());
			EnvRiskEvidence envEvidence = calculateEnvRiskEvidence(hosts,
					host.getAddress());
			boolean predictedMalicious = globalTrust < this.threshold ||
					(this.enableEnvRiskDetection &&
					envEvidence.risk >= this.envRiskThreshold &&
					envEvidence.totalFailureCount >=
							this.minEnvRiskEvidence);

			normalCount++;
			if (predictedMalicious) {
				falsePositiveCount++;
			}
			globalTrustSum += globalTrust;
			envRiskSum += envEvidence.risk;
		}

		double fpr = safeDivide(falsePositiveCount, normalCount);
		double avgNormalGlobalTrust = safeDivide(globalTrustSum, normalCount);
		double avgNormalEnvRisk = safeDivide(envRiskSum, normalCount);

		write(format(getSimTime()) + "," +
				falsePositiveCount + "," +
				format(fpr) + "," +
				format(avgNormalGlobalTrust) + "," +
				format(avgNormalEnvRisk));
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

	private double safeDivide(double numerator, double denominator) {
		if (denominator <= 0.0) {
			return 0.0;
		}
		return numerator / denominator;
	}

	private static class EnvRiskEvidence {
		double highEnvFailureCount;
		double highEnvTotalCount;
		double totalFailureCount;
		double risk;
	}
}
