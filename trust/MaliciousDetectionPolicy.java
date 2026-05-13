package trust;

import java.util.Collection;

import core.Settings;

/**
 * Unified malicious detection policy shared by reports and Leader election.
 *
 * A node is predicted malicious if:
 *   1. globalTrust < maliciousDetectionThreshold; or
 *   2. environmental camouflage risk is high enough and supported by enough
 *      failure evidence.
 *
 * The policy uses only trust/risk evidence, never ground-truth attack labels.
 */
public class MaliciousDetectionPolicy {
	private static final String EETLE_NS = "EETLERouter";
	private static final String THRESHOLD_SETTING =
			"maliciousDetectionThreshold";
	private static final String ENV_RISK_THRESHOLD_SETTING =
			"envRiskDetectionThreshold";
	private static final String MIN_ENV_RISK_EVIDENCE_SETTING =
			"minEnvRiskEvidence";
	private static final String ENABLE_ENV_RISK_DETECTION_SETTING =
			"enableEnvRiskDetection";
	private static final String ENABLE_STRICT_LEADER_DETECTION_SETTING =
			"enableStrictLeaderDetection";
	private static final String LEADER_THRESHOLD_SETTING =
			"leaderMaliciousDetectionThreshold";
	private static final String LEADER_ENV_RISK_THRESHOLD_SETTING =
			"leaderEnvRiskDetectionThreshold";
	private static final String LEADER_MIN_ENV_RISK_EVIDENCE_SETTING =
			"leaderMinEnvRiskEvidence";

	private double maliciousDetectionThreshold = 0.65;
	private double envRiskDetectionThreshold = 0.65;
	private int minEnvRiskEvidence = 3;
	private boolean enableEnvRiskDetection = true;
	private boolean enableStrictLeaderDetection = false;
	private double leaderMaliciousDetectionThreshold = 0.70;
	private double leaderEnvRiskDetectionThreshold = 0.60;
	private int leaderMinEnvRiskEvidence = 2;

	public MaliciousDetectionPolicy() {
		this(new Settings(EETLE_NS));
	}

	public MaliciousDetectionPolicy(Settings settings) {
		configure(settings);
	}

	public void configure(Settings settings) {
		if (settings == null) {
			return;
		}
		if (settings.contains(THRESHOLD_SETTING)) {
			this.maliciousDetectionThreshold =
					clamp(settings.getDouble(THRESHOLD_SETTING));
		}
		if (settings.contains(ENV_RISK_THRESHOLD_SETTING)) {
			this.envRiskDetectionThreshold =
					clamp(settings.getDouble(ENV_RISK_THRESHOLD_SETTING));
		}
		if (settings.contains(MIN_ENV_RISK_EVIDENCE_SETTING)) {
			this.minEnvRiskEvidence =
					settings.getInt(MIN_ENV_RISK_EVIDENCE_SETTING);
			if (this.minEnvRiskEvidence < 0) {
				this.minEnvRiskEvidence = 0;
			}
		}
		if (settings.contains(ENABLE_ENV_RISK_DETECTION_SETTING)) {
			this.enableEnvRiskDetection =
					settings.getBoolean(ENABLE_ENV_RISK_DETECTION_SETTING);
		}
		if (settings.contains(ENABLE_STRICT_LEADER_DETECTION_SETTING)) {
			this.enableStrictLeaderDetection = settings.getBoolean(
					ENABLE_STRICT_LEADER_DETECTION_SETTING);
		}
		if (settings.contains(LEADER_THRESHOLD_SETTING)) {
			this.leaderMaliciousDetectionThreshold =
					clamp(settings.getDouble(LEADER_THRESHOLD_SETTING));
		}
		if (settings.contains(LEADER_ENV_RISK_THRESHOLD_SETTING)) {
			this.leaderEnvRiskDetectionThreshold =
					clamp(settings.getDouble(
							LEADER_ENV_RISK_THRESHOLD_SETTING));
		}
		if (settings.contains(LEADER_MIN_ENV_RISK_EVIDENCE_SETTING)) {
			this.leaderMinEnvRiskEvidence =
					settings.getInt(LEADER_MIN_ENV_RISK_EVIDENCE_SETTING);
			if (this.leaderMinEnvRiskEvidence < 0) {
				this.leaderMinEnvRiskEvidence = 0;
			}
		}
	}

	public MaliciousDetectionResult evaluate(int address, double globalTrust,
			Collection<TrustEdge> edges) {
		MaliciousDetectionResult result = new MaliciousDetectionResult();
		EnvRiskEvidence evidence = calculateEnvRiskEvidence(address, edges);
		result.setEnvCamouflageRisk(evidence.risk);
		result.setTotalFailureCount(evidence.totalFailureCount);
		result.setDetectionThreshold(this.maliciousDetectionThreshold);
		result.setEnvRiskThreshold(this.envRiskDetectionThreshold);

		if (globalTrust < this.maliciousDetectionThreshold) {
			result.setPredictedMalicious(true);
			result.setReason("LOW_GLOBAL_TRUST");
			return result;
		}

		if (this.enableEnvRiskDetection &&
				evidence.risk >= this.envRiskDetectionThreshold &&
				evidence.totalFailureCount >= this.minEnvRiskEvidence) {
			result.setPredictedMalicious(true);
			result.setReason("ENV_CAMOUFLAGE_RISK");
			return result;
		}

		result.setPredictedMalicious(false);
		result.setReason("NORMAL");
		return result;
	}

	public MaliciousDetectionResult detectForLeaderCandidate(int address,
			double globalTrust, Collection<TrustEdge> edges) {
		if (!this.enableStrictLeaderDetection) {
			return evaluate(address, globalTrust, edges);
		}

		MaliciousDetectionResult result = new MaliciousDetectionResult();
		EnvRiskEvidence evidence = calculateEnvRiskEvidence(address, edges);
		result.setEnvCamouflageRisk(evidence.risk);
		result.setTotalFailureCount(evidence.totalFailureCount);
		result.setDetectionThreshold(
				this.leaderMaliciousDetectionThreshold);
		result.setEnvRiskThreshold(this.leaderEnvRiskDetectionThreshold);

		if (globalTrust < this.leaderMaliciousDetectionThreshold) {
			result.setPredictedMalicious(true);
			result.setReason("LEADER_LOW_GLOBAL_TRUST");
			return result;
		}

		if (this.enableEnvRiskDetection &&
				evidence.risk >= this.leaderEnvRiskDetectionThreshold &&
				evidence.totalFailureCount >=
				this.leaderMinEnvRiskEvidence) {
			result.setPredictedMalicious(true);
			result.setReason("LEADER_ENV_RISK");
			return result;
		}

		result.setPredictedMalicious(false);
		result.setReason("NORMAL");
		return result;
	}

	public boolean isPredictedMalicious(int address, double globalTrust,
			Collection<TrustEdge> edges) {
		return evaluate(address, globalTrust, edges).isPredictedMalicious();
	}

	public double getMaliciousDetectionThreshold() {
		return this.maliciousDetectionThreshold;
	}

	public double getEnvRiskDetectionThreshold() {
		return this.envRiskDetectionThreshold;
	}

	public int getMinEnvRiskEvidence() {
		return this.minEnvRiskEvidence;
	}

	public boolean isEnvRiskDetectionEnabled() {
		return this.enableEnvRiskDetection;
	}

	public boolean isStrictLeaderDetectionEnabled() {
		return this.enableStrictLeaderDetection;
	}

	public double getLeaderMaliciousDetectionThreshold() {
		return this.leaderMaliciousDetectionThreshold;
	}

	public double getLeaderEnvRiskDetectionThreshold() {
		return this.leaderEnvRiskDetectionThreshold;
	}

	public int getLeaderMinEnvRiskEvidence() {
		return this.leaderMinEnvRiskEvidence;
	}

	private EnvRiskEvidence calculateEnvRiskEvidence(int targetAddress,
			Collection<TrustEdge> edges) {
		EnvRiskEvidence evidence = new EnvRiskEvidence();
		if (edges == null) {
			return evidence;
		}

		String targetId = String.valueOf(targetAddress);
		for (TrustEdge edge : edges) {
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

		double failureConcentration = evidence.highEnvFailureCount /
				Math.max(1.0, evidence.totalFailureCount);
		double highEnvConcentration = evidence.highEnvFailureCount /
				Math.max(1.0, evidence.highEnvTotalCount);
		evidence.risk = Math.max(failureConcentration,
				highEnvConcentration);
		evidence.risk = clamp(evidence.risk);
		return evidence;
	}

	private double clamp(double value) {
		if (value < 0.0) {
			return 0.0;
		}
		if (value > 1.0) {
			return 1.0;
		}
		return value;
	}

	private static class EnvRiskEvidence {
		double highEnvFailureCount;
		double highEnvTotalCount;
		double totalFailureCount;
		double risk;
	}
}
