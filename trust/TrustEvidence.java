package trust;

/**
 * Behavior and event evidence accumulated for one target node.
 */
public class TrustEvidence {
	public int success;
	public int failure;
	public int trueEvents;
	public int falseEvents;

	public TrustEvidence() {
		this.success = 0;
		this.failure = 0;
		this.trueEvents = 0;
		this.falseEvents = 0;
	}

	public int getTotalEvents() {
		return this.trueEvents + this.falseEvents;
	}
}
