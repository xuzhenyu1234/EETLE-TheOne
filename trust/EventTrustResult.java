package trust;

/**
 * Event semantic trust outcome for one reporter's event report.
 */
public class EventTrustResult {
	private int evaluatorAddress;
	private int targetAddress;
	private int eventId;
	private int reportedState;
	private int realState;
	private int consensusState;
	private double consensusProbability;
	private boolean agreement;
	private boolean falseReport;
	private boolean uncertain;
	private double reward;
	private double penalty;
	private double reporterGlobalTrust;
	private int eventEvaluatorCount;
	private boolean appliedPenalty;
	private boolean appliedReward;
	private double timestamp;

	public int getEvaluatorAddress() { return this.evaluatorAddress; }
	public void setEvaluatorAddress(int evaluatorAddress) {
		this.evaluatorAddress = evaluatorAddress;
	}
	public int getTargetAddress() { return this.targetAddress; }
	public void setTargetAddress(int targetAddress) {
		this.targetAddress = targetAddress;
	}
	public int getEventId() { return this.eventId; }
	public void setEventId(int eventId) { this.eventId = eventId; }
	public int getReportedState() { return this.reportedState; }
	public void setReportedState(int reportedState) {
		this.reportedState = reportedState;
	}
	public int getRealState() { return this.realState; }
	public void setRealState(int realState) { this.realState = realState; }
	public int getConsensusState() { return this.consensusState; }
	public void setConsensusState(int consensusState) {
		this.consensusState = consensusState;
	}
	public double getConsensusProbability() {
		return this.consensusProbability;
	}
	public void setConsensusProbability(double consensusProbability) {
		this.consensusProbability = consensusProbability;
	}
	public boolean isAgreement() { return this.agreement; }
	public void setAgreement(boolean agreement) { this.agreement = agreement; }
	public boolean isFalseReport() { return this.falseReport; }
	public void setFalseReport(boolean falseReport) {
		this.falseReport = falseReport;
	}
	public boolean isUncertain() { return this.uncertain; }
	public void setUncertain(boolean uncertain) { this.uncertain = uncertain; }
	public double getReward() { return this.reward; }
	public void setReward(double reward) { this.reward = reward; }
	public double getPenalty() { return this.penalty; }
	public void setPenalty(double penalty) { this.penalty = penalty; }
	public double getReporterGlobalTrust() {
		return this.reporterGlobalTrust;
	}
	public void setReporterGlobalTrust(double reporterGlobalTrust) {
		this.reporterGlobalTrust = reporterGlobalTrust;
	}
	public int getEventEvaluatorCount() { return this.eventEvaluatorCount; }
	public void setEventEvaluatorCount(int eventEvaluatorCount) {
		this.eventEvaluatorCount = eventEvaluatorCount;
	}
	public boolean isAppliedPenalty() { return this.appliedPenalty; }
	public void setAppliedPenalty(boolean appliedPenalty) {
		this.appliedPenalty = appliedPenalty;
	}
	public boolean isAppliedReward() { return this.appliedReward; }
	public void setAppliedReward(boolean appliedReward) {
		this.appliedReward = appliedReward;
	}
	public double getTimestamp() { return this.timestamp; }
	public void setTimestamp(double timestamp) { this.timestamp = timestamp; }
}
