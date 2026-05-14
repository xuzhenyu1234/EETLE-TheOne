package trust;

/**
 * Observation that evaluator i delegated a message to target j and is waiting
 * to see whether j cooperates by forwarding or delivering it later.
 */
public class ForwardObservation {
	private String messageId;
	private int evaluatorAddress;
	private int targetAddress;
	private double delegatedTime;
	private double deadlineTime;
	private double pout;
	private boolean settled;
	private int forwardingOpportunityCount;
	private boolean hadForwardingOpportunity;
	private boolean expiredAsUncertain;

	public ForwardObservation() {
		this.messageId = null;
		this.evaluatorAddress = -1;
		this.targetAddress = -1;
		this.delegatedTime = 0.0;
		this.deadlineTime = 0.0;
		this.pout = 0.0;
		this.settled = false;
		this.forwardingOpportunityCount = 0;
		this.hadForwardingOpportunity = false;
		this.expiredAsUncertain = false;
	}

	public String getMessageId() {
		return this.messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
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

	public double getDelegatedTime() {
		return this.delegatedTime;
	}

	public void setDelegatedTime(double delegatedTime) {
		this.delegatedTime = delegatedTime;
	}

	public double getDeadlineTime() {
		return this.deadlineTime;
	}

	public void setDeadlineTime(double deadlineTime) {
		this.deadlineTime = deadlineTime;
	}

	public double getPout() {
		return this.pout;
	}

	public void setPout(double pout) {
		this.pout = pout;
	}

	public boolean isSettled() {
		return this.settled;
	}

	public void setSettled(boolean settled) {
		this.settled = settled;
	}

	public int getForwardingOpportunityCount() {
		return this.forwardingOpportunityCount;
	}

	public void incrementForwardingOpportunityCount() {
		this.forwardingOpportunityCount++;
		this.hadForwardingOpportunity = true;
	}

	public boolean hadForwardingOpportunity() {
		return this.hadForwardingOpportunity;
	}

	public void setHadForwardingOpportunity(boolean hadForwardingOpportunity) {
		this.hadForwardingOpportunity = hadForwardingOpportunity;
	}

	public boolean isExpiredAsUncertain() {
		return this.expiredAsUncertain;
	}

	public void setExpiredAsUncertain(boolean expiredAsUncertain) {
		this.expiredAsUncertain = expiredAsUncertain;
	}
}
