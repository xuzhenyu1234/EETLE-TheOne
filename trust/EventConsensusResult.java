package trust;

/**
 * Trust-weighted consensus result for one event window.
 */
public class EventConsensusResult {
	private int eventId;
	private int consensusState;
	private double consensusProbability;
	private double agreementScore;
	private boolean uncertain;
	private int reportCount;
	private double timestamp;

	public int getEventId() {
		return this.eventId;
	}

	public void setEventId(int eventId) {
		this.eventId = eventId;
	}

	public int getConsensusState() {
		return this.consensusState;
	}

	public void setConsensusState(int consensusState) {
		this.consensusState = consensusState;
	}

	public double getConsensusProbability() {
		return this.consensusProbability;
	}

	public void setConsensusProbability(double consensusProbability) {
		this.consensusProbability = consensusProbability;
	}

	public double getAgreementScore() {
		return this.agreementScore;
	}

	public void setAgreementScore(double agreementScore) {
		this.agreementScore = agreementScore;
	}

	public boolean isUncertain() {
		return this.uncertain;
	}

	public void setUncertain(boolean uncertain) {
		this.uncertain = uncertain;
	}

	public int getReportCount() {
		return this.reportCount;
	}

	public void setReportCount(int reportCount) {
		this.reportCount = reportCount;
	}

	public double getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}
}
