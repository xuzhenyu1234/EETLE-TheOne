package trust;

/**
 * Global trust result for a target node j, fused by the Leader from
 * multiple recommender nodes' local trust records.
 */
public class GlobalTrustEntry {
	private int targetAddress;
	private double globalTrust;
	private double fusedBelief;
	private double fusedEnvUncertainty;
	private double fusedCognitiveUncertainty;
	private double fusedDisbelief;
	private double fusedCommunicationQuality;
	private int recommendationCount;
	private double lastUpdateTime;

	public GlobalTrustEntry() {
		this.targetAddress = 0;
		this.globalTrust = 0.5;
		this.fusedBelief = 0.5;
		this.fusedEnvUncertainty = 0.0;
		this.fusedCognitiveUncertainty = 0.5;
		this.fusedDisbelief = 0.0;
		this.fusedCommunicationQuality = 0.5;
		this.recommendationCount = 0;
		this.lastUpdateTime = 0.0;
	}

	public int getTargetAddress() {
		return this.targetAddress;
	}

	public void setTargetAddress(int targetAddress) {
		this.targetAddress = targetAddress;
	}

	public double getGlobalTrust() {
		return this.globalTrust;
	}

	public void setGlobalTrust(double globalTrust) {
		this.globalTrust = globalTrust;
	}

	public double getFusedBelief() {
		return this.fusedBelief;
	}

	public void setFusedBelief(double fusedBelief) {
		this.fusedBelief = fusedBelief;
	}

	public double getFusedEnvUncertainty() {
		return this.fusedEnvUncertainty;
	}

	public void setFusedEnvUncertainty(double fusedEnvUncertainty) {
		this.fusedEnvUncertainty = fusedEnvUncertainty;
	}

	public double getFusedCognitiveUncertainty() {
		return this.fusedCognitiveUncertainty;
	}

	public void setFusedCognitiveUncertainty(double fusedCognitiveUncertainty) {
		this.fusedCognitiveUncertainty = fusedCognitiveUncertainty;
	}

	public double getFusedDisbelief() {
		return this.fusedDisbelief;
	}

	public void setFusedDisbelief(double fusedDisbelief) {
		this.fusedDisbelief = fusedDisbelief;
	}

	public double getFusedCommunicationQuality() {
		return this.fusedCommunicationQuality;
	}

	public void setFusedCommunicationQuality(double fusedCommunicationQuality) {
		this.fusedCommunicationQuality = fusedCommunicationQuality;
	}

	public int getRecommendationCount() {
		return this.recommendationCount;
	}

	public void setRecommendationCount(int recommendationCount) {
		this.recommendationCount = recommendationCount;
	}

	public double getLastUpdateTime() {
		return this.lastUpdateTime;
	}

	public void setLastUpdateTime(double lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public void normalizeVector() {
		TrustVector vector = new TrustVector(this.fusedBelief,
				this.fusedEnvUncertainty, this.fusedCognitiveUncertainty,
				this.fusedDisbelief);
		this.fusedBelief = vector.b;
		this.fusedEnvUncertainty = vector.e;
		this.fusedCognitiveUncertainty = vector.u;
		this.fusedDisbelief = vector.d;
	}
}
