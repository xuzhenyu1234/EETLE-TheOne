/*
 * Malicious node detection metrics for EETLE.
 */
package report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import routing.EETLERouter;
import routing.MessageRouter;
import trust.AttackType;
import trust.GlobalTrustManager;
import trust.MaliciousDetectionPolicy;
import trust.MaliciousDetectionResult;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Periodically evaluates malicious node detection from global trust.
 *
 * Detection rule:
 * predictedMalicious = globalTrust < maliciousDetectionThreshold.
 * Ground truth:
 * actualMalicious = attackType != NORMAL.
 */
public class DetectionReport extends Report implements UpdateListener {
	private static final String HEADER =
			"time,recordType,threshold,envRiskThreshold,attackType,node," +
			"globalTrust,envCamouflageRisk,totalFailureCount," +
			"predictedMalicious,actualMalicious,TP,FP,FN,TN,precision," +
			"recall,f1,fpr";
	private static final String EETLE_NS = "EETLERouter";
	private static final String THRESHOLD_SETTING =
			"maliciousDetectionThreshold";
	private static final String ENV_RISK_THRESHOLD_SETTING =
			"envRiskDetectionThreshold";
	private static final String MIN_ENV_RISK_EVIDENCE_SETTING =
			"minEnvRiskEvidence";
	private static final String ENABLE_ENV_RISK_DETECTION_SETTING =
			"enableEnvRiskDetection";
	private static final double DEFAULT_THRESHOLD = 0.65;
	private static final double DEFAULT_ENV_RISK_THRESHOLD = 0.65;
	private static final int DEFAULT_MIN_ENV_RISK_EVIDENCE = 3;
	private static final double DEFAULT_INTERVAL = 100.0;

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;
	private double threshold = DEFAULT_THRESHOLD;
	private double envRiskThreshold = DEFAULT_ENV_RISK_THRESHOLD;
	private int minEnvRiskEvidence = DEFAULT_MIN_ENV_RISK_EVIDENCE;
	private boolean enableEnvRiskDetection = true;

	public DetectionReport() {
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
		writeDetectionMetrics(hosts);
	}

	private void writeDetectionMetrics(List<DTNHost> hosts) {
		GlobalTrustManager manager = EETLERouter.getGlobalTrustManager();
		if (manager == null) {
			return;
		}
		MaliciousDetectionPolicy policy =
				EETLERouter.getMaliciousDetectionPolicy();
		this.threshold = policy.getMaliciousDetectionThreshold();
		this.envRiskThreshold = policy.getEnvRiskDetectionThreshold();

		Counts all = new Counts();
		Map<String, Counts> byType = new HashMap<String, Counts>();

		for (int i = 0; i < hosts.size(); i++) {
			DTNHost host = hosts.get(i);
			MessageRouter router = host.getRouter();
			if (!(router instanceof EETLERouter)) {
				continue;
			}

			EETLERouter eetle = (EETLERouter)router;
			AttackType attackType = eetle.getAttackType();
			String typeName = attackType.toString();
			boolean actualMalicious = attackType != AttackType.NORMAL;
			double globalTrust = manager.getGlobalTrust(host.getAddress());
			MaliciousDetectionResult detection = policy.evaluate(
					host.getAddress(), globalTrust,
					EETLERouter.getAllTrustEdgesSnapshot());
			boolean predictedMalicious =
					detection.isPredictedMalicious();

			all.add(actualMalicious, predictedMalicious);
			Counts typeCounts = byType.get(typeName);
			if (typeCounts == null) {
				typeCounts = new Counts();
				byType.put(typeName, typeCounts);
			}
			typeCounts.add(actualMalicious, predictedMalicious);
			writeNode(host.getAddress(), typeName, globalTrust, detection,
					predictedMalicious, actualMalicious);
		}

		writeCounts("ALL", all, true);
		writeCounts(AttackType.NORMAL.toString(),
				getCounts(byType, AttackType.NORMAL.toString()), false);
		writeCounts(AttackType.BLACKHOLE.toString(),
				getCounts(byType, AttackType.BLACKHOLE.toString()), false);
		writeCounts(AttackType.ON_OFF.toString(),
				getCounts(byType, AttackType.ON_OFF.toString()), false);
		writeCounts(AttackType.FALSE_EVENT.toString(),
				getCounts(byType, AttackType.FALSE_EVENT.toString()), false);
		writeCounts(AttackType.ENV_CAMOUFLAGE.toString(),
				getCounts(byType, AttackType.ENV_CAMOUFLAGE.toString()), false);
		writeCounts(AttackType.CROSS_REGION.toString(),
				getCounts(byType, AttackType.CROSS_REGION.toString()), false);
	}

	private Counts getCounts(Map<String, Counts> byType, String typeName) {
		Counts counts = byType.get(typeName);
		if (counts == null) {
			return new Counts();
		}
		return counts;
	}

	private void writeCounts(String attackType, Counts counts,
			boolean includeFpr) {
		double precision = safeDivide(counts.tp, counts.tp + counts.fp);
		double recall = safeDivide(counts.tp, counts.tp + counts.fn);
		double f1 = precision + recall <= 0.0 ? 0.0 :
				2.0 * precision * recall / (precision + recall);
		double fpr = includeFpr ?
				safeDivide(counts.fp, counts.fp + counts.tn) : 0.0;

		write(format(getSimTime()) + ",SUMMARY," +
				format(this.threshold) + "," +
				format(this.envRiskThreshold) + "," +
				attackType + "," +
				"," +
				"," +
				"," +
				"," +
				"," +
				"," +
				counts.tp + "," +
				counts.fp + "," +
				counts.fn + "," +
				counts.tn + "," +
				format(precision) + "," +
				format(recall) + "," +
				format(f1) + "," +
				format(fpr));
	}

	private void writeNode(int node, String attackType, double globalTrust,
			MaliciousDetectionResult detection, boolean predictedMalicious,
			boolean actualMalicious) {
		write(format(getSimTime()) + ",NODE," +
				format(this.threshold) + "," +
				format(this.envRiskThreshold) + "," +
				attackType + "," +
				node + "," +
				format(globalTrust) + "," +
				format(detection.getEnvCamouflageRisk()) + "," +
				format(detection.getTotalFailureCount()) + "," +
				predictedMalicious + "," +
				actualMalicious + "," +
				",,,,,,,");
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

}
