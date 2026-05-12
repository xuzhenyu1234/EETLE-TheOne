package trust;

import java.util.ArrayList;
import java.util.List;

/**
 * Directed trust edge T_ij from evaluator node i to target node j.
 */
public class TrustEdge {
	private String evaluatorId;
	private String targetId;

	public double successCount;
	public double failCount;

	public TrustVector vector;

	public double scalarTrust;
	public double lastUpdateTime;
	public double lastDecayFactor;
	public double trueEventCount;
	public double falseEventCount;
	public double uncertainEventCount;
	public double uncertainForwardCount;

	public int highEnvTotalCount;
	public int highEnvFailureCount;

	public double lastPout;

	private List<Double> trustHistory;

	public TrustEdge(String evaluatorId, String targetId) {
		this.evaluatorId = evaluatorId;
		this.targetId = targetId;
		this.successCount = 0;
		this.failCount = 0;
		this.vector = new TrustVector();
		this.scalarTrust = 0.5;
		this.lastUpdateTime = 0.0;
		this.lastDecayFactor = 1.0;
		this.trueEventCount = 0.0;
		this.falseEventCount = 0.0;
		this.uncertainEventCount = 0.0;
		this.uncertainForwardCount = 0.0;
		this.highEnvTotalCount = 0;
		this.highEnvFailureCount = 0;
		this.lastPout = 0.0;
		this.trustHistory = new ArrayList<Double>();
	}

	public String getEvaluatorId() {
		return this.evaluatorId;
	}

	public String getTargetId() {
		return this.targetId;
	}

	public double getInteractionCount() {
		return this.successCount + this.failCount;
	}

	public void addTrustHistory(double value) {
		this.trustHistory.add(clamp(value));
	}

	public List<Double> getTrustHistory() {
		return this.trustHistory;
	}

	public double getLatestTrust() {
		if (this.trustHistory.size() == 0) {
			return this.scalarTrust;
		}
		return this.trustHistory.get(this.trustHistory.size() - 1);
	}

	private double clamp(double value) {
		if (value < 0) {
			return 0.0;
		}
		if (value > 1) {
			return 1.0;
		}
		return value;
	}
}
