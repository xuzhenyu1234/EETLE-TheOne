package trust;

/**
 * One candidate snapshot used by robust Leader election.
 */
public class LeaderCandidate {
	private int address;
	private double globalTrust;
	private double trustStability;
	private double communicationQuality;
	private double regionConstraintFactor;
	private double baseScore;
	private double finalScore;
	private boolean eligible;
	private String rejectReason;
	private boolean predictedMalicious;
	private double envCamouflageRisk;
	private double totalFailureCount;
	private double leaderDetectionThreshold;
	private double leaderEnvRiskThreshold;
	private String detectionReason;

	public LeaderCandidate() {
		this.address = 0;
		this.globalTrust = 0.5;
		this.trustStability = 1.0;
		this.communicationQuality = 0.5;
		this.regionConstraintFactor = 1.0;
		this.baseScore = 0.0;
		this.finalScore = 0.0;
		this.eligible = false;
		this.rejectReason = "";
		this.predictedMalicious = false;
		this.envCamouflageRisk = 0.0;
		this.totalFailureCount = 0.0;
		this.leaderDetectionThreshold = 0.0;
		this.leaderEnvRiskThreshold = 0.0;
		this.detectionReason = "NORMAL";
	}

	public int getAddress() { return this.address; }
	public void setAddress(int address) { this.address = address; }
	public double getGlobalTrust() { return this.globalTrust; }
	public void setGlobalTrust(double globalTrust) {
		this.globalTrust = globalTrust;
	}
	public double getTrustStability() { return this.trustStability; }
	public void setTrustStability(double trustStability) {
		this.trustStability = trustStability;
	}
	public double getCommunicationQuality() {
		return this.communicationQuality;
	}
	public void setCommunicationQuality(double communicationQuality) {
		this.communicationQuality = communicationQuality;
	}
	public double getRegionConstraintFactor() {
		return this.regionConstraintFactor;
	}
	public void setRegionConstraintFactor(double regionConstraintFactor) {
		this.regionConstraintFactor = regionConstraintFactor;
	}
	public double getBaseScore() { return this.baseScore; }
	public void setBaseScore(double baseScore) {
		this.baseScore = baseScore;
	}
	public double getFinalScore() { return this.finalScore; }
	public void setFinalScore(double finalScore) {
		this.finalScore = finalScore;
	}
	public boolean isEligible() { return this.eligible; }
	public void setEligible(boolean eligible) { this.eligible = eligible; }
	public String getRejectReason() { return this.rejectReason; }
	public void setRejectReason(String rejectReason) {
		this.rejectReason = rejectReason;
	}
	public boolean isPredictedMalicious() {
		return this.predictedMalicious;
	}
	public void setPredictedMalicious(boolean predictedMalicious) {
		this.predictedMalicious = predictedMalicious;
	}
	public double getEnvCamouflageRisk() {
		return this.envCamouflageRisk;
	}
	public void setEnvCamouflageRisk(double envCamouflageRisk) {
		this.envCamouflageRisk = envCamouflageRisk;
	}
	public double getTotalFailureCount() {
		return this.totalFailureCount;
	}
	public void setTotalFailureCount(double totalFailureCount) {
		this.totalFailureCount = totalFailureCount;
	}
	public double getLeaderDetectionThreshold() {
		return this.leaderDetectionThreshold;
	}
	public void setLeaderDetectionThreshold(
			double leaderDetectionThreshold) {
		this.leaderDetectionThreshold = leaderDetectionThreshold;
	}
	public double getLeaderEnvRiskThreshold() {
		return this.leaderEnvRiskThreshold;
	}
	public void setLeaderEnvRiskThreshold(double leaderEnvRiskThreshold) {
		this.leaderEnvRiskThreshold = leaderEnvRiskThreshold;
	}
	public String getDetectionReason() {
		return this.detectionReason;
	}
	public void setDetectionReason(String detectionReason) {
		this.detectionReason = detectionReason;
	}
}
