package trust;

/**
 * Local trust record uploaded by evaluator node i to the Leader.
 * Contains i's trust evaluation of target node j.
 */
public class LocalTrustRecord {
	private int evaluatorAddress;
	private int targetAddress;
	private TrustVector trustVector;
	private double scalarTrust;
	private double pout;
	private double timestamp;
	private double recommendationConsistency;
	private double communicationQuality;
	private double spatialCorrelation;
	private double attentionWeight;

	public LocalTrustRecord() {
		this.evaluatorAddress = 0;
		this.targetAddress = 0;
		this.trustVector = new TrustVector();
		this.scalarTrust = 0.5;
		this.pout = 0.0;
		this.timestamp = 0.0;
		this.recommendationConsistency = 1.0;
		this.communicationQuality = 1.0;
		this.spatialCorrelation = 1.0;
		this.attentionWeight = 0.0;
	}

	public LocalTrustRecord(int evaluatorAddress, int targetAddress,
			TrustVector trustVector, double scalarTrust, double pout,
			double timestamp) {
		this();
		this.evaluatorAddress = evaluatorAddress;
		this.targetAddress = targetAddress;
		setTrustVector(trustVector);
		this.scalarTrust = scalarTrust;
		this.pout = pout;
		this.timestamp = timestamp;
	}

	public int getEvaluatorAddress() {
		return this.evaluatorAddress;
	}

	public void setEvaluatorAddress(int evaluatorAddress) {
		this.evaluatorAddress = evaluatorAddress;
	}

	public int getTargetAddress() {
		return this.targetAddress;
	}

	public void setTargetAddress(int targetAddress) {
		this.targetAddress = targetAddress;
	}

	public TrustVector getTrustVector() {
		return this.trustVector;
	}

	public void setTrustVector(TrustVector trustVector) {
		if (trustVector == null) {
			this.trustVector = new TrustVector();
		}
		else {
			this.trustVector = trustVector.copy();
		}
	}

	public double getScalarTrust() {
		return this.scalarTrust;
	}

	public void setScalarTrust(double scalarTrust) {
		this.scalarTrust = scalarTrust;
	}

	public double getPout() {
		return this.pout;
	}

	public void setPout(double pout) {
		this.pout = pout;
	}

	public double getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	public double getRecommendationConsistency() {
		return this.recommendationConsistency;
	}

	public void setRecommendationConsistency(double recommendationConsistency) {
		this.recommendationConsistency = recommendationConsistency;
	}

	public double getCommunicationQuality() {
		return this.communicationQuality;
	}

	public void setCommunicationQuality(double communicationQuality) {
		this.communicationQuality = communicationQuality;
	}

	public double getSpatialCorrelation() {
		return this.spatialCorrelation;
	}

	public void setSpatialCorrelation(double spatialCorrelation) {
		this.spatialCorrelation = spatialCorrelation;
	}

	public double getAttentionWeight() {
		return this.attentionWeight;
	}

	public void setAttentionWeight(double attentionWeight) {
		this.attentionWeight = attentionWeight;
	}
}
