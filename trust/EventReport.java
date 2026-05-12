package trust;

/**
 * Disaster event report submitted by one node.
 * realState is only for simulation validation and reporting, not for trust
 * update decisions.
 */
public class EventReport {
	private int reporterAddress;
	private int eventId;
	private int eventType;
	private int reportedState;
	private int realState;
	private double timestamp;
	private double confidence;
	private double x;
	private double y;
	private int region;

	public int getReporterAddress() {
		return this.reporterAddress;
	}

	public void setReporterAddress(int reporterAddress) {
		this.reporterAddress = reporterAddress;
	}

	public int getEventId() {
		return this.eventId;
	}

	public void setEventId(int eventId) {
		this.eventId = eventId;
	}

	public int getEventType() {
		return this.eventType;
	}

	public void setEventType(int eventType) {
		this.eventType = eventType;
	}

	public int getReportedState() {
		return this.reportedState;
	}

	public void setReportedState(int reportedState) {
		this.reportedState = reportedState;
	}

	public int getRealState() {
		return this.realState;
	}

	public void setRealState(int realState) {
		this.realState = realState;
	}

	public double getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	public double getConfidence() {
		return this.confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public double getX() {
		return this.x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return this.y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public int getRegion() {
		return this.region;
	}

	public void setRegion(int region) {
		this.region = region;
	}
}
