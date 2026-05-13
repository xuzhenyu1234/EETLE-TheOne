package trust;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes trust-weighted event consensus and semantic trust results.
 */
public class EventTrustManager {
	private double eventPositiveThreshold = 0.65;
	private double eventNegativeThreshold = 0.35;
	private double eventReward = 0.05;
	private double eventPenalty = 0.15;
	private double eventEvaluatorTrustThreshold = 0.55;
	private double eventConsensusRadius = 300.0;
	private double eventConsensusInterval = 120.0;
	private int minEventConsensusReports = 3;
	private int maxEventEvaluatorsPerReport = 10;

	private Map<Integer, List<EventReport>> reportsByEvent;
	private List<EventTrustResult> resultHistory;

	public EventTrustManager() {
		this.reportsByEvent = new HashMap<Integer, List<EventReport>>();
		this.resultHistory = new ArrayList<EventTrustResult>();
	}

	public void collectEventReport(EventReport report) {
		Integer key = new Integer(report.getEventId());
		List<EventReport> reports = this.reportsByEvent.get(key);
		if (reports == null) {
			reports = new ArrayList<EventReport>();
			this.reportsByEvent.put(key, reports);
		}
		reports.add(report);
	}

	public EventConsensusResult computeConsensus(int eventId,
			Collection<EventReport> reports,
			GlobalTrustManager globalTrustManager) {
		return computeConsensus(eventId, null, reports, 0.0,
				globalTrustManager);
	}

	public EventConsensusResult computeConsensus(int eventId,
			EventReport targetReport, Collection<EventReport> reports,
			double currentTime, GlobalTrustManager globalTrustManager) {
		double weightedState = 0.0;
		double weightSum = 0.0;
		int count = 0;

		for (EventReport report : reports) {
			if (!isConsensusCandidate(targetReport, report, currentTime)) {
				continue;
			}
			double weight = 0.5;
			if (globalTrustManager != null) {
				weight = globalTrustManager.getGlobalTrust(
						report.getReporterAddress());
			}
			weight = clamp(weight);
			weightedState += weight * report.getReportedState();
			weightSum += weight;
			count++;
		}

		double probability = weightSum <= 0 ? 0.5 : weightedState / weightSum;
		EventConsensusResult result = new EventConsensusResult();
		result.setEventId(eventId);
		result.setConsensusProbability(probability);
		result.setReportCount(count);
		result.setAgreementScore(Math.abs(probability - 0.5) * 2.0);

		if (count < this.minEventConsensusReports) {
			result.setConsensusState(-1);
			result.setUncertain(true);
		}
		else if (probability >= this.eventPositiveThreshold) {
			result.setConsensusState(1);
			result.setUncertain(false);
		}
		else if (probability <= this.eventNegativeThreshold) {
			result.setConsensusState(0);
			result.setUncertain(false);
		}
		else {
			result.setConsensusState(-1);
			result.setUncertain(true);
		}
		return result;
	}

	private boolean isConsensusCandidate(EventReport targetReport,
			EventReport candidate, double currentTime) {
		if (candidate == null) {
			return false;
		}
		if (targetReport == null) {
			return true;
		}
		if (candidate.getReporterAddress() ==
				targetReport.getReporterAddress()) {
			return false;
		}
		if (Math.abs(candidate.getTimestamp() -
				targetReport.getTimestamp()) > this.eventConsensusInterval) {
			return false;
		}
		if (currentTime > 0.0 &&
				Math.abs(currentTime - candidate.getTimestamp()) >
				this.eventConsensusInterval) {
			return false;
		}
		if (candidate.getRegion() == targetReport.getRegion()) {
			return true;
		}
		return distance(candidate, targetReport) <= this.eventConsensusRadius;
	}

	private double distance(EventReport a, EventReport b) {
		double dx = a.getX() - b.getX();
		double dy = a.getY() - b.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	public List<EventTrustResult> evaluateReports(int evaluatorAddress,
			double currentTime, GlobalTrustManager globalTrustManager) {
		List<EventTrustResult> results = new ArrayList<EventTrustResult>();

		for (Map.Entry<Integer, List<EventReport>> entry :
				this.reportsByEvent.entrySet()) {
			int eventId = entry.getKey().intValue();
			List<EventReport> reports = entry.getValue();
			if (reports.size() == 0) {
				continue;
			}

			for (int i = 0; i < reports.size(); i++) {
				EventReport report = reports.get(i);
				EventConsensusResult consensus = computeConsensus(eventId,
						report, reports, currentTime, globalTrustManager);
				consensus.setTimestamp(currentTime);
				List<Integer> evaluators = selectEventEvaluators(
						evaluatorAddress, report, reports, consensus,
						globalTrustManager);
				int evaluatorCount = evaluators.size();
				for (int k = 0; k < evaluators.size(); k++) {
					EventTrustResult result = createResult(evaluators.get(k),
							report, consensus, eventId, currentTime,
							globalTrustManager, evaluatorCount);
					results.add(result);
					this.resultHistory.add(result);
				}
			}
		}

		this.reportsByEvent.clear();
		return results;
	}

	public Collection<EventTrustResult> getResultHistory() {
		return this.resultHistory;
	}

	private EventTrustResult createResult(int evaluatorAddress,
			EventReport report, EventConsensusResult consensus, int eventId,
			double currentTime, GlobalTrustManager globalTrustManager,
			int evaluatorCount) {
		EventTrustResult result = new EventTrustResult();
		result.setEvaluatorAddress(evaluatorAddress);
		result.setTargetAddress(report.getReporterAddress());
		result.setEventId(eventId);
		result.setReportedState(report.getReportedState());
		result.setRealState(report.getRealState());
		result.setConsensusState(consensus.getConsensusState());
		result.setConsensusProbability(consensus.getConsensusProbability());
		result.setUncertain(consensus.isUncertain());
		result.setTimestamp(currentTime);
		result.setEventEvaluatorCount(evaluatorCount);
		result.setReporterGlobalTrust(globalTrustManager == null ? 0.5 :
				globalTrustManager.getGlobalTrust(report.getReporterAddress()));

		if (consensus.isUncertain()) {
			result.setAgreement(false);
			result.setFalseReport(false);
			result.setReward(0.0);
			result.setPenalty(0.0);
			result.setAppliedReward(false);
			result.setAppliedPenalty(false);
		}
		else if (report.getReportedState() == consensus.getConsensusState()) {
			result.setAgreement(true);
			result.setFalseReport(false);
			result.setReward(this.eventReward);
			result.setPenalty(0.0);
			result.setAppliedReward(true);
			result.setAppliedPenalty(false);
		}
		else {
			result.setAgreement(false);
			result.setFalseReport(true);
			result.setReward(0.0);
			result.setPenalty(this.eventPenalty);
			result.setAppliedReward(false);
			result.setAppliedPenalty(true);
		}
		return result;
	}

	private List<Integer> selectEventEvaluators(int leaderAddress,
			EventReport targetReport, List<EventReport> reports,
			EventConsensusResult consensus,
			GlobalTrustManager globalTrustManager) {
		List<Integer> evaluators = new ArrayList<Integer>();
		int reporter = targetReport.getReporterAddress();
		boolean falseReport = !consensus.isUncertain() &&
				targetReport.getReportedState() != consensus.getConsensusState();

		addEvaluator(evaluators, leaderAddress, reporter);
		if (!falseReport || consensus.getConsensusState() == -1) {
			return evaluators;
		}

		for (int i = 0; i < reports.size(); i++) {
			EventReport candidate = reports.get(i);
			if (candidate.getReportedState() == consensus.getConsensusState()) {
				addTrustedParticipant(evaluators, candidate, reporter,
						globalTrustManager);
				if (evaluators.size() >= this.maxEventEvaluatorsPerReport) {
					return evaluators;
				}
			}
		}

		for (int i = 0; i < reports.size(); i++) {
			EventReport candidate = reports.get(i);
			if (candidate.getReportedState() != consensus.getConsensusState()) {
				addTrustedParticipant(evaluators, candidate, reporter,
						globalTrustManager);
				if (evaluators.size() >= this.maxEventEvaluatorsPerReport) {
					return evaluators;
				}
			}
		}

		return evaluators;
	}

	private void addTrustedParticipant(List<Integer> evaluators,
			EventReport candidate, int reporter,
			GlobalTrustManager globalTrustManager) {
		int evaluator = candidate.getReporterAddress();
		if (evaluator == reporter) {
			return;
		}
		double trust = globalTrustManager == null ? 0.5 :
				globalTrustManager.getGlobalTrust(evaluator);
		if (trust < this.eventEvaluatorTrustThreshold) {
			return;
		}
		addEvaluator(evaluators, evaluator, reporter);
	}

	private void addEvaluator(List<Integer> evaluators, int evaluator,
			int reporter) {
		if (evaluator == reporter) {
			return;
		}
		for (int i = 0; i < evaluators.size(); i++) {
			if (evaluators.get(i).intValue() == evaluator) {
				return;
			}
		}
		evaluators.add(new Integer(evaluator));
	}

	public void setEventPositiveThreshold(double threshold) {
		this.eventPositiveThreshold = threshold;
	}

	public void setEventNegativeThreshold(double threshold) {
		this.eventNegativeThreshold = threshold;
	}

	public void setEventReward(double reward) {
		this.eventReward = reward;
	}

	public void setEventPenalty(double penalty) {
		this.eventPenalty = penalty;
	}

	public void setEventConsensusRadius(double radius) {
		if (radius < 0.0) {
			this.eventConsensusRadius = 0.0;
		}
		else {
			this.eventConsensusRadius = radius;
		}
	}

	public void setEventConsensusInterval(double interval) {
		if (interval < 0.0) {
			this.eventConsensusInterval = 0.0;
		}
		else {
			this.eventConsensusInterval = interval;
		}
	}

	public void setMinEventConsensusReports(int minReports) {
		if (minReports < 1) {
			this.minEventConsensusReports = 1;
		}
		else {
			this.minEventConsensusReports = minReports;
		}
	}

	public void setEventEvaluatorTrustThreshold(double threshold) {
		this.eventEvaluatorTrustThreshold = clamp(threshold);
	}

	public void setMaxEventEvaluatorsPerReport(int maxEvaluators) {
		if (maxEvaluators < 1) {
			this.maxEventEvaluatorsPerReport = 1;
		}
		else {
			this.maxEventEvaluatorsPerReport = maxEvaluators;
		}
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
}
