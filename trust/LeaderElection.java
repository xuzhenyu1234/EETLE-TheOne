package trust;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Robust Leader election based on global trust, stability, communication
 * quality, region constraints, switching margin, and abnormal reelection.
 */
public class LeaderElection {
	private int currentLeaderAddress = 0;
	private double currentLeaderScore = 0.0;
	private double candidateTrustThreshold = 0.60;
	private double minTrustStability = 0.40;
	private double minCommunicationQuality = 0.30;
	private double switchingMargin = 0.08;
	private double abnormalTrustThreshold = 0.45;
	private double abnormalStabilityThreshold = 0.30;
	private double abnormalCommunicationThreshold = 0.20;
	private double weightTrust = 0.50;
	private double weightStability = 0.30;
	private double weightCommunication = 0.20;
	private int trustHistoryWindowSize = 10;
	private boolean enableRegionConstraint = true;
	private boolean enableTrustStability = true;
	private boolean enableLeaderSwitchMargin = true;

	private Map<Integer, Deque<Double>> globalTrustHistoryMap;
	private List<LeaderCandidate> lastCandidates;
	private int leaderChangeCount;
	private double lastElectionTime;
	private String lastSwitchReason;

	public LeaderElection() {
		this.globalTrustHistoryMap = new HashMap<Integer, Deque<Double>>();
		this.lastCandidates = new ArrayList<LeaderCandidate>();
		this.leaderChangeCount = 0;
		this.lastElectionTime = 0.0;
		this.lastSwitchReason = "INITIAL_SELECTION";
	}

	public void updateElection(GlobalTrustManager globalTrustManager,
			RegionManager regionManager, double now) {
		updateElection(globalTrustManager, regionManager, null, null, now);
	}

	public void updateElection(GlobalTrustManager globalTrustManager,
			RegionManager regionManager,
			MaliciousDetectionPolicy detectionPolicy,
			Collection<TrustEdge> trustEdges, double now) {
		if (globalTrustManager == null || regionManager == null) {
			return;
		}

		Collection<GlobalTrustEntry> entries =
				globalTrustManager.getAllGlobalTrustEntries();
		List<LeaderCandidate> candidates =
				new ArrayList<LeaderCandidate>();
		int leaderRegion = regionManager.getCurrentRegion(
				this.currentLeaderAddress);

		for (GlobalTrustEntry entry : entries) {
			LeaderCandidate candidate = buildCandidate(entry, regionManager,
					leaderRegion, detectionPolicy, trustEdges, now);
			candidates.add(candidate);
		}

		this.lastCandidates = candidates;
		this.lastElectionTime = now;

		LeaderCandidate best = findBestEligible(candidates);
		LeaderCandidate current = findCandidate(candidates,
				this.currentLeaderAddress);
		boolean abnormal = isCurrentLeaderAbnormal(current, regionManager,
				leaderRegion, now);

		if (best == null) {
			this.lastSwitchReason =
					isDetectedMalicious(current) ?
					"LEADER_DETECTED_MALICIOUS_REELECTION" :
					"NO_ELIGIBLE_CANDIDATE";
			if (current != null) {
				this.currentLeaderScore = current.getFinalScore();
			}
			return;
		}

		if (current == null || this.currentLeaderScore <= 0.0) {
			switchLeader(best, "INITIAL_SELECTION");
			return;
		}

		if (abnormal) {
			if (best.getAddress() != this.currentLeaderAddress) {
				String reason = isDetectedMalicious(current) ?
						"LEADER_DETECTED_MALICIOUS_REELECTION" :
						"ABNORMAL_REELECTION";
				switchLeader(best, reason);
			}
			else {
				this.currentLeaderScore = best.getFinalScore();
				this.lastSwitchReason = isDetectedMalicious(current) ?
						"LEADER_DETECTED_MALICIOUS_REELECTION" :
						"ABNORMAL_REELECTION";
			}
			return;
		}

		this.currentLeaderScore = current.getFinalScore();
		double margin = this.enableLeaderSwitchMargin ?
				this.switchingMargin : 0.0;
		if (best.getAddress() != this.currentLeaderAddress &&
				best.getFinalScore() >
				this.currentLeaderScore + margin) {
			switchLeader(best, "BETTER_CANDIDATE");
		}
		else {
			this.lastSwitchReason = "KEEP_CURRENT";
		}
	}

	private LeaderCandidate buildCandidate(GlobalTrustEntry entry,
			RegionManager regionManager, int leaderRegion,
			MaliciousDetectionPolicy detectionPolicy,
			Collection<TrustEdge> trustEdges, double now) {
		int address = entry.getTargetAddress();
		double globalTrust = clamp(entry.getGlobalTrust());
		addTrustHistory(address, globalTrust);

		LeaderCandidate candidate = new LeaderCandidate();
		candidate.setAddress(address);
		candidate.setGlobalTrust(globalTrust);
		if (this.enableTrustStability) {
			candidate.setTrustStability(calculateTrustStability(address));
		}
		else {
			candidate.setTrustStability(1.0);
		}
		candidate.setCommunicationQuality(clamp(
				entry.getFusedCommunicationQuality()));
		if (this.enableRegionConstraint) {
			candidate.setRegionConstraintFactor(clamp(
					regionManager.getRegionConstraintFactor(address,
							leaderRegion, now)));
		}
		else {
			candidate.setRegionConstraintFactor(1.0);
		}

		double baseScore = weightTrust * candidate.getGlobalTrust() +
				weightStability * candidate.getTrustStability() +
				weightCommunication * candidate.getCommunicationQuality();
		candidate.setBaseScore(clamp(baseScore));
		candidate.setFinalScore(clamp(candidate.getBaseScore() *
				candidate.getRegionConstraintFactor()));
		if (detectionPolicy != null) {
			MaliciousDetectionResult result =
					detectionPolicy.detectForLeaderCandidate(
							address, globalTrust, trustEdges);
			candidate.setPredictedMalicious(
					result.isPredictedMalicious());
			candidate.setEnvCamouflageRisk(
					result.getEnvCamouflageRisk());
			candidate.setTotalFailureCount(
					result.getTotalFailureCount());
			candidate.setLeaderDetectionThreshold(
					result.getDetectionThreshold());
			candidate.setLeaderEnvRiskThreshold(
					result.getEnvRiskThreshold());
			candidate.setDetectionReason(result.getReason());
		}
		applyEligibility(candidate, regionManager, leaderRegion, now);
		return candidate;
	}

	private void applyEligibility(LeaderCandidate candidate,
			RegionManager regionManager, int leaderRegion, double now) {
		if (candidate.isPredictedMalicious()) {
			candidate.setEligible(false);
			candidate.setRejectReason("LEADER_DETECTED_MALICIOUS");
			return;
		}
		if (candidate.getGlobalTrust() < this.candidateTrustThreshold) {
			candidate.setEligible(false);
			candidate.setRejectReason("LOW_GLOBAL_TRUST");
			return;
		}
		if (candidate.getTrustStability() < this.minTrustStability) {
			candidate.setEligible(false);
			candidate.setRejectReason("LOW_STABILITY");
			return;
		}
		if (candidate.getCommunicationQuality() <
				this.minCommunicationQuality) {
			candidate.setEligible(false);
			candidate.setRejectReason("LOW_COMMUNICATION");
			return;
		}
		if (this.enableRegionConstraint &&
				!regionManager.canBeLeaderCandidate(candidate.getAddress(),
						leaderRegion, now)) {
			candidate.setEligible(false);
			candidate.setRejectReason("REGION_CONSTRAINT");
			return;
		}
		candidate.setEligible(true);
		candidate.setRejectReason("");
	}

	private boolean isCurrentLeaderAbnormal(LeaderCandidate current,
			RegionManager regionManager, int leaderRegion, double now) {
		if (current == null) {
			return true;
		}
		if (current.isPredictedMalicious()) {
			return true;
		}
		if (current.getGlobalTrust() < this.abnormalTrustThreshold) {
			return true;
		}
		if (current.getTrustStability() < this.abnormalStabilityThreshold) {
			return true;
		}
		if (current.getCommunicationQuality() <
				this.abnormalCommunicationThreshold) {
			return true;
		}
		return this.enableRegionConstraint &&
				!regionManager.canBeLeaderCandidate(
						this.currentLeaderAddress, leaderRegion, now);
	}

	private LeaderCandidate findBestEligible(List<LeaderCandidate> candidates) {
		LeaderCandidate best = null;
		for (int i = 0; i < candidates.size(); i++) {
			LeaderCandidate candidate = candidates.get(i);
			if (!candidate.isEligible()) {
				continue;
			}
			if (best == null ||
					candidate.getFinalScore() > best.getFinalScore()) {
				best = candidate;
			}
		}
		return best;
	}

	private LeaderCandidate findCandidate(List<LeaderCandidate> candidates,
			int address) {
		for (int i = 0; i < candidates.size(); i++) {
			LeaderCandidate candidate = candidates.get(i);
			if (candidate.getAddress() == address) {
				return candidate;
			}
		}
		return null;
	}

	private boolean isDetectedMalicious(LeaderCandidate candidate) {
		return candidate != null && candidate.isPredictedMalicious();
	}

	private void switchLeader(LeaderCandidate candidate, String reason) {
		if (candidate.getAddress() != this.currentLeaderAddress) {
			this.leaderChangeCount++;
		}
		this.currentLeaderAddress = candidate.getAddress();
		this.currentLeaderScore = candidate.getFinalScore();
		this.lastSwitchReason = reason;
	}

	private void addTrustHistory(int address, double globalTrust) {
		Integer key = new Integer(address);
		Deque<Double> history = this.globalTrustHistoryMap.get(key);
		if (history == null) {
			history = new ArrayDeque<Double>();
			this.globalTrustHistoryMap.put(key, history);
		}
		history.addLast(new Double(globalTrust));
		while (history.size() > this.trustHistoryWindowSize) {
			history.removeFirst();
		}
	}

	private double calculateTrustStability(int address) {
		Deque<Double> history = this.globalTrustHistoryMap.get(
				new Integer(address));
		if (history == null || history.size() == 0) {
			return 1.0;
		}

		double sum = 0.0;
		for (Double value : history) {
			sum += value.doubleValue();
		}
		double mean = sum / history.size();
		double variance = 0.0;
		for (Double value : history) {
			double diff = value.doubleValue() - mean;
			variance += diff * diff;
		}
		variance = variance / history.size();
		return clamp(1.0 / (1.0 + variance));
	}

	private double clamp(double value) {
		if (value < 0.0) {
			return 0.0;
		}
		if (value > 1.0) {
			return 1.0;
		}
		return value;
	}

	public int getCurrentLeaderAddress() {
		return this.currentLeaderAddress;
	}
	public double getCurrentLeaderScore() {
		return this.currentLeaderScore;
	}
	public int getLeaderChangeCount() { return this.leaderChangeCount; }
	public double getLastElectionTime() { return this.lastElectionTime; }
	public String getLastSwitchReason() { return this.lastSwitchReason; }
	public List<LeaderCandidate> getLastCandidates() {
		return this.lastCandidates;
	}

	public void setCandidateTrustThreshold(double value) {
		this.candidateTrustThreshold = clamp(value);
	}
	public void setMinTrustStability(double value) {
		this.minTrustStability = clamp(value);
	}
	public void setMinCommunicationQuality(double value) {
		this.minCommunicationQuality = clamp(value);
	}
	public void setSwitchingMargin(double value) {
		this.switchingMargin = clamp(value);
	}
	public void setAbnormalTrustThreshold(double value) {
		this.abnormalTrustThreshold = clamp(value);
	}
	public void setAbnormalStabilityThreshold(double value) {
		this.abnormalStabilityThreshold = clamp(value);
	}
	public void setAbnormalCommunicationThreshold(double value) {
		this.abnormalCommunicationThreshold = clamp(value);
	}
	public void setWeights(double trust, double stability,
			double communication) {
		double sum = trust + stability + communication;
		if (sum <= 0.0) {
			return;
		}
		this.weightTrust = trust / sum;
		this.weightStability = stability / sum;
		this.weightCommunication = communication / sum;
	}
	public void setTrustHistoryWindowSize(int value) {
		if (value < 1) {
			this.trustHistoryWindowSize = 1;
		}
		else {
			this.trustHistoryWindowSize = value;
		}
	}

	public void setEnableRegionConstraint(boolean enableRegionConstraint) {
		this.enableRegionConstraint = enableRegionConstraint;
	}

	public void setEnableTrustStability(boolean enableTrustStability) {
		this.enableTrustStability = enableTrustStability;
	}

	public void setEnableLeaderSwitchMargin(
			boolean enableLeaderSwitchMargin) {
		this.enableLeaderSwitchMargin = enableLeaderSwitchMargin;
	}
}
