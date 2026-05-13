package trust;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Leader-side global trust fusion using manually weighted linear attention.
 *
 * The Leader collects LocalTrustRecord from multiple recommender nodes i
 * about target node j, then fuses them with linear attention weights that
 * consider:
 *   R_i  - recommender's own global trust (self-credibility)
 *   A_i  - recommendation consistency (historical trust deviation)
 *   Q_i  - communication quality between recommender and Leader
 *   S_ij - spatial correlation between recommender and target
 *
 * Reference: EETLE paper Section 5, global trust fusion with linear attention.
 */
public class GlobalTrustManager {

	/* Linear attention weights */
	private double omegaR = 0.35;
	private double omegaA = 0.25;
	private double omegaQ = 0.20;
	private double omegaS = 0.20;
	private double attentionBeta = 2.0;
	private boolean enableLinearAttention = true;

	/* Latest local trust records, keyed by targetAddress then evaluatorAddress */
	private Map<Integer, Map<Integer, LocalTrustRecord>> recordsByTarget;

	/* Previous round global trust per node, for R_i in next round */
	private Map<Integer, Double> previousGlobalTrust;

	/* Current global trust entries after fusion */
	private Map<Integer, GlobalTrustEntry> globalTrustEntries;
	private Map<Integer, Double> recommendationDeviationSum;
	private Map<Integer, Integer> recommendationDeviationCount;

	public GlobalTrustManager() {
		this.recordsByTarget =
				new HashMap<Integer, Map<Integer, LocalTrustRecord>>();
		this.previousGlobalTrust = new HashMap<Integer, Double>();
		this.globalTrustEntries = new HashMap<Integer, GlobalTrustEntry>();
		this.recommendationDeviationSum = new HashMap<Integer, Double>();
		this.recommendationDeviationCount = new HashMap<Integer, Integer>();
	}

	/**
	 * Collects one local trust record from a recommender node.
	 * Called by each EETLERouter periodically.
	 */
	public void collectLocalTrust(LocalTrustRecord record) {
		updateRecommendationConsistency(record);
		Integer key = new Integer(record.getTargetAddress());
		Map<Integer, LocalTrustRecord> records = this.recordsByTarget.get(key);
		if (records == null) {
			records = new HashMap<Integer, LocalTrustRecord>();
			this.recordsByTarget.put(key, records);
		}
		records.put(new Integer(record.getEvaluatorAddress()), record);
	}

	/**
	 * Recommendation consistency:
	 * deviation_i,j = |LT_i,j - GT_j(t-1)|.
	 * A_i = 1 / (1 + avgDeviation_i). If evaluator i has no previous
	 * deviation history, A_i defaults to 1.0 for the first recommendation.
	 */
	private void updateRecommendationConsistency(LocalTrustRecord record) {
		Integer evaluatorKey = new Integer(record.getEvaluatorAddress());
		Double sumObj = this.recommendationDeviationSum.get(evaluatorKey);
		Integer countObj = this.recommendationDeviationCount.get(evaluatorKey);
		double sum = sumObj == null ? 0.0 : sumObj.doubleValue();
		int count = countObj == null ? 0 : countObj.intValue();

		if (count <= 0) {
			record.setRecommendationConsistency(1.0);
		}
		else {
			double avgDeviation = sum / count;
			record.setRecommendationConsistency(clamp(
					1.0 / (1.0 + avgDeviation)));
		}

		double previousTargetTrust =
				getPreviousGlobalTrust(record.getTargetAddress());
		double deviation = Math.abs(record.getScalarTrust() -
				previousTargetTrust);
		this.recommendationDeviationSum.put(evaluatorKey,
				new Double(sum + deviation));
		this.recommendationDeviationCount.put(evaluatorKey,
				new Integer(count + 1));
	}

	/**
	 * Fuses all collected local trust records into global trust entries.
	 * Called by the Leader node (default address 0) periodically.
	 *
	 * For each target node j:
	 *   1. Compute linear attention score for each recommender i.
	 *   2. Apply softmax to get attention weights.
	 *   3. Weighted fusion of scalar trust and four-dimensional trust vector.
	 *
	 * @param leaderAddress  address of the Leader node
	 * @param currentTime    current simulation time
	 */
	public void updateGlobalTrust(int leaderAddress, double currentTime) {
		Map<Integer, Double> newGlobalTrustMap = new HashMap<Integer, Double>();

		for (Map.Entry<Integer, Map<Integer, LocalTrustRecord>> entry :
				this.recordsByTarget.entrySet()) {
			int targetAddress = entry.getKey().intValue();
			List<LocalTrustRecord> records =
					new ArrayList<LocalTrustRecord>(entry.getValue().values());

			if (records.size() == 0) {
				continue;
			}

			/*
			 * Step 1: Compute linear attention score for each recommender.
			 * score_i = omegaR * R_i + omegaA * A_i
			 *         + omegaQ * Q_i + omegaS * S_ij
			 *
			 * R_i = previous global trust of recommender, default 0.5.
			 * A_i = recommendation consistency, default 1.0.
			 * Q_i = 1 - pout(Leader->evaluator), clamped to [0,1].
			 * S_ij = spatial correlation, default 1.0.
			 */
			double[] scores = new double[records.size()];
			double maxScore = Double.NEGATIVE_INFINITY;

			for (int i = 0; i < records.size(); i++) {
				LocalTrustRecord rec = records.get(i);

				double r = getPreviousGlobalTrust(rec.getEvaluatorAddress());
				double a = clamp(rec.getRecommendationConsistency());
				double q = rec.getCommunicationQuality();
				if (q < 0.0) {
					q = clamp(1.0 - rec.getPout());
					rec.setCommunicationQuality(q);
				}
				else {
					q = clamp(q);
				}
				double s = rec.getSpatialCorrelation();
				if (s < 0.0) {
					s = 1.0;
				}
				else {
					s = clamp(s);
				}

				double score = omegaR * r + omegaA * a + omegaQ * q + omegaS * s;
				scores[i] = score;
				if (score > maxScore) {
					maxScore = score;
				}
			}

			/*
			 * Step 2: Softmax attention weights.
			 * weight_i = exp(beta * score_i) / sum(exp(beta * score_j)).
			 * Subtract maxScore for numerical stability.
			 */
			double[] weights = new double[records.size()];
			double weightSum = 0.0;

			for (int i = 0; i < records.size(); i++) {
				double expVal = Math.exp(attentionBeta * (scores[i] - maxScore));
				weights[i] = expVal;
				weightSum += expVal;
			}

			for (int i = 0; i < records.size(); i++) {
				double w;
				if (this.enableLinearAttention) {
					w = weights[i] / weightSum;
				}
				else {
					w = 1.0 / records.size();
				}
				weights[i] = w;
				records.get(i).setAttentionWeight(w);
			}

			/*
			 * Step 3: Weighted fusion.
			 * globalTrust(j) = sum_i weight_i * scalarTrust_i(j).
			 * Fused four-dimensional trust vector is also weighted.
			 */
			double fusedB = 0.0;
			double fusedE = 0.0;
			double fusedU = 0.0;
			double fusedD = 0.0;
			double fusedScalar = 0.0;
			double fusedCommunicationQuality = 0.0;
			double averageAttentionWeight = 0.0;
			double averageRecommendationConsistency = 0.0;
			double averageSpatialCorrelation = 0.0;

			for (int i = 0; i < records.size(); i++) {
				LocalTrustRecord rec = records.get(i);
				double w = weights[i];
				double q = rec.getCommunicationQuality();
				if (q < 0.0) {
					q = clamp(1.0 - rec.getPout());
				}
				else {
					q = clamp(q);
				}

				fusedB += w * rec.getTrustVector().b;
				fusedE += w * rec.getTrustVector().e;
				fusedU += w * rec.getTrustVector().u;
				fusedD += w * rec.getTrustVector().d;
				fusedScalar += w * rec.getScalarTrust();
				fusedCommunicationQuality += w * q;
				averageAttentionWeight += w;
				averageRecommendationConsistency +=
						rec.getRecommendationConsistency();
				averageSpatialCorrelation += rec.getSpatialCorrelation();
			}

			/* Normalize the four-dimensional vector */
			TrustVector fusedVector = new TrustVector(fusedB, fusedE, fusedU,
					fusedD);
			fusedVector.normalize();

			/* Clamp scalar trust */
			fusedScalar = clamp(fusedScalar);

			/*
			 * Step 4: Store global trust entry.
			 */
			GlobalTrustEntry gtEntry = new GlobalTrustEntry();
			gtEntry.setTargetAddress(targetAddress);
			gtEntry.setGlobalTrust(fusedScalar);
			gtEntry.setFusedBelief(fusedVector.b);
			gtEntry.setFusedEnvUncertainty(fusedVector.e);
			gtEntry.setFusedCognitiveUncertainty(fusedVector.u);
			gtEntry.setFusedDisbelief(fusedVector.d);
			gtEntry.setFusedCommunicationQuality(
					clamp(fusedCommunicationQuality));
			gtEntry.setAverageAttentionWeight(
					clamp(averageAttentionWeight / records.size()));
			gtEntry.setAverageRecommendationConsistency(clamp(
					averageRecommendationConsistency / records.size()));
			gtEntry.setAverageSpatialCorrelation(clamp(
					averageSpatialCorrelation / records.size()));
			gtEntry.normalizeVector();
			gtEntry.setRecommendationCount(records.size());
			gtEntry.setLastUpdateTime(currentTime);

			this.globalTrustEntries.put(new Integer(targetAddress), gtEntry);
			newGlobalTrustMap.put(new Integer(targetAddress),
					new Double(fusedScalar));
		}

		/* Update previous global trust for next fusion round */
		for (Map.Entry<Integer, Double> e : newGlobalTrustMap.entrySet()) {
			this.previousGlobalTrust.put(e.getKey(), e.getValue());
		}

	}

	/**
	 * Returns the global trust entry for a target node.
	 * Null if no fusion has been performed for this target.
	 */
	public GlobalTrustEntry getGlobalTrustEntry(int targetAddress) {
		return this.globalTrustEntries.get(new Integer(targetAddress));
	}

	/**
	 * Returns the global scalar trust for a target node.
	 * Default 0.5 if no entry exists.
	 */
	public double getGlobalTrust(int targetAddress) {
		GlobalTrustEntry entry = getGlobalTrustEntry(targetAddress);
		if (entry == null) {
			return 0.5;
		}
		return entry.getGlobalTrust();
	}

	/**
	 * Returns all current global trust entries.
	 */
	public Collection<GlobalTrustEntry> getAllGlobalTrustEntries() {
		return this.globalTrustEntries.values();
	}

	/**
	 * Returns the previous round global trust for a node.
	 * Default 0.5 if no history exists.
	 */
	private double getPreviousGlobalTrust(int address) {
		Double val = this.previousGlobalTrust.get(new Integer(address));
		if (val == null) {
			return 0.5;
		}
		return val.doubleValue();
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

	public void setEnableLinearAttention(boolean enableLinearAttention) {
		this.enableLinearAttention = enableLinearAttention;
	}
}
