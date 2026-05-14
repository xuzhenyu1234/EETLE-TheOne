package trust;

/**
 * Snapshot of the malicious detection decision for one node.
 */
public class MaliciousDetectionResult {
	private boolean predictedMalicious;
	private double envCamouflageRisk;
	private double totalFailureCount;
	private double detectionThreshold;
	private double envRiskThreshold;
	private String reason;

	public MaliciousDetectionResult() {
		this.predictedMalicious = false;
		this.envCamouflageRisk = 0.0;
		this.totalFailureCount = 0.0;
		this.detectionThreshold = 0.0;
		this.envRiskThreshold = 0.0;
		this.reason = "NORMAL";
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

	public double getDetectionThreshold() {
		return this.detectionThreshold;
	}

	public void setDetectionThreshold(double detectionThreshold) {
		this.detectionThreshold = detectionThreshold;
	}

	public double getEnvRiskThreshold() {
		return this.envRiskThreshold;
	}

	public void setEnvRiskThreshold(double envRiskThreshold) {
		this.envRiskThreshold = envRiskThreshold;
	}

	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
