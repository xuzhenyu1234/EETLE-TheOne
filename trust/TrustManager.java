package trust;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements local trust update formulas for EETLE.
 * TrustManager receives Pout from LinkEnvironmentModel and maps it to
 * environmental uncertainty. It does not calculate SINR directly.
 */
public class TrustManager {
	private static final double DEFAULT_ENV_WEIGHT = 0.7;
	private static final double EVENT_PENALTY_RATIO = 0.5;
	private static final double EVENT_BELIEF_PENALTY_RATIO = 0.8;

	private double betaCog = 2.0;
	private double alphaEnv = 0.7;
	private double etaEnv = 0.6;

	private double envThreshold = 0.6;
	private double hiddenRiskThreshold = 0.5;
	private double rhoHidden = 0.2;
	private int minHiddenSamples = 5;

	private double scalarWeightC = 0.5;
	private double scalarWeightE = 0.3;
	private double scalarWeightD = 1.5;
	private double baseDecayRate = 0.001;
	private double envDecaySensitivity = 0.5;
	private double eventReward = 0.05;
	private double eventPenalty = 0.15;
	private boolean enableEATR = true;

	private Map<Integer, TrustEvidence> evidenceByAddress;
	private Map<Integer, TrustVector> vectorByAddress;
	private Map<Integer, Double> scalarByAddress;
	private double envWeight = DEFAULT_ENV_WEIGHT;

	public TrustManager() {
		this.evidenceByAddress = new HashMap<Integer, TrustEvidence>();
		this.vectorByAddress = new HashMap<Integer, TrustVector>();
		this.scalarByAddress = new HashMap<Integer, Double>();
	}

	public void recordSuccess(int targetAddress, double linkOutageProbability) {
		TrustEvidence evidence = getOrCreateEvidence(targetAddress);
		evidence.success++;
		recomputeAddressTrust(targetAddress, linkOutageProbability);
	}

	public void recordDebugSuccess(int targetAddress,
			double linkOutageProbability) {
		recordSuccess(targetAddress, linkOutageProbability);
	}

	public void recordFailure(int targetAddress, double linkOutageProbability) {
		TrustEvidence evidence = getOrCreateEvidence(targetAddress);
		evidence.failure++;
		recomputeAddressTrust(targetAddress, linkOutageProbability);
	}

	public void recordDebugFailure(int targetAddress,
			double linkOutageProbability) {
		recordFailure(targetAddress, linkOutageProbability);
	}

	public void recordTrueEvent(int targetAddress,
			double linkOutageProbability) {
		TrustEvidence evidence = getOrCreateEvidence(targetAddress);
		evidence.trueEvents++;
		recomputeAddressTrust(targetAddress, linkOutageProbability);
	}

	public void recordFalseEvent(int targetAddress,
			double linkOutageProbability) {
		TrustEvidence evidence = getOrCreateEvidence(targetAddress);
		evidence.falseEvents++;
		recomputeAddressTrust(targetAddress, linkOutageProbability);
	}

	public double getScalarTrust(int targetAddress) {
		Double scalar = this.scalarByAddress.get(new Integer(targetAddress));
		if (scalar == null) {
			return 0.5;
		}
		return scalar.doubleValue();
	}

	public TrustVector getTrustVector(int targetAddress) {
		TrustVector vector = this.vectorByAddress.get(new Integer(targetAddress));
		if (vector == null) {
			return new TrustVector();
		}
		return vector.copy();
	}

	public TrustEdge updateByForwardResult(TrustTable table,
			String evaluatorId, String targetId, boolean success,
			double pout, double currentTime) {
		TrustEdge edge = table.getOrCreateEdge(evaluatorId, targetId);
		updateByForwardResult(edge, success, pout, currentTime);
		return edge;
	}

	public TrustEdge updateByForwardResult(TrustTable table,
			int evaluatorAddress, int targetAddress, boolean success,
			double pout, double currentTime) {
		return updateByForwardResult(table, String.valueOf(evaluatorAddress),
				String.valueOf(targetAddress), success, pout, currentTime);
	}

	public TrustEdge updateByUncertainForwardResult(TrustTable table,
			int evaluatorAddress, int targetAddress, double pout,
			double currentTime) {
		TrustEdge edge = table.getOrCreateEdge(
				String.valueOf(evaluatorAddress), String.valueOf(targetAddress));
		updateByUncertainForwardResult(edge, pout, currentTime);
		return edge;
	}

	private TrustEvidence getOrCreateEvidence(int targetAddress) {
		Integer key = new Integer(targetAddress);
		TrustEvidence evidence = this.evidenceByAddress.get(key);
		if (evidence == null) {
			evidence = new TrustEvidence();
			this.evidenceByAddress.put(key, evidence);
		}
		return evidence;
	}

	private void recomputeAddressTrust(int targetAddress,
			double linkOutageProbability) {
		TrustEvidence evidence = getOrCreateEvidence(targetAddress);
		double pout = clamp(linkOutageProbability);

		/*
		 * Beta behavior trust:
		 * alpha = success + 1, beta = failure + 1.
		 * behaviorBelief = alpha / (alpha + beta),
		 * behaviorDisbelief = beta / (alpha + beta).
		 */
		double alpha = evidence.success + 1.0;
		double beta = evidence.failure + 1.0;
		double behaviorBelief = alpha / (alpha + beta);
		double behaviorDisbelief = beta / (alpha + beta);

		/*
		 * Cognitive uncertainty:
		 * cognitiveUncertainty = 1 / (1 + success + failure).
		 * More forwarding observations reduce uncertainty.
		 */
		double cognitiveUncertainty = 1.0 /
				(1.0 + evidence.success + evidence.failure);

		/*
		 * Environmental uncertainty is derived only from Pout:
		 * envUncertainty = envWeight * linkOutageProbability.
		 */
		double envUncertainty = this.envWeight * pout;

		double disbelief = behaviorDisbelief;
		if (this.enableEATR) {
			/*
			 * EATR: split disbelief into environmental and behavioral parts.
			 * envPart = disbelief * Pout, behaviorPart = disbelief * (1-Pout).
			 * Environmental mass moves from disbelief to envUncertainty.
			 */
			double envPart = behaviorDisbelief * pout;
			double behaviorPart = behaviorDisbelief * (1.0 - pout);
			disbelief = behaviorPart;
			envUncertainty += envPart;
		}

		/*
		 * Simple event punishment: if false event reports dominate, move part
		 * of cognitive uncertainty into disbelief.
		 */
		int totalEvents = evidence.getTotalEvents();
		if (totalEvents > 0 &&
				(1.0 * evidence.falseEvents) / totalEvents > 0.5) {
			double falseRatio = (1.0 * evidence.falseEvents) / totalEvents;
			double eventRisk = falseRatio - 0.5;
			double transfer = EVENT_PENALTY_RATIO * cognitiveUncertainty;
			cognitiveUncertainty -= transfer;
			disbelief += transfer;

			/*
			 * False-event attackers can still forward packets successfully.
			 * When false reports dominate event evidence, move an additional
			 * risk-weighted part of belief to disbelief so semantic attacks
			 * are visible in local trust, not hidden by forwarding success.
			 */
			double beliefTransfer = EVENT_BELIEF_PENALTY_RATIO *
					eventRisk * behaviorBelief;
			behaviorBelief -= beliefTransfer;
			disbelief += beliefTransfer;
		}

		TrustVector vector = new TrustVector(behaviorBelief, envUncertainty,
				cognitiveUncertainty, disbelief);
		double scalar = calculateScalarTrust(vector);

		Integer key = new Integer(targetAddress);
		this.vectorByAddress.put(key, vector);
		this.scalarByAddress.put(key, new Double(scalar));
	}

	public void updateByForwardResult(TrustEdge edge, boolean success,
			double pout, double currentTime) {
		pout = clamp(pout);
		applyEnvironmentalTimeDecay(edge, pout, currentTime);

		if (success) {
			edge.successCount += 1.0;
		}
		else {
			edge.failCount += 1.0;
		}

		double s = edge.successCount;
		double f = edge.failCount;

		/*
		 * Beta behavior evidence:
		 * b0 = (s + 1) / (s + f + 2), d0 = (f + 1) / (s + f + 2).
		 * The +1 smoothing avoids extreme trust values during cold start.
		 */
		double b0 = (s + 1.0) / (s + f + 2.0);
		double d0 = (f + 1.0) / (s + f + 2.0);

		/*
		 * Cognitive uncertainty:
		 * u0 = betaCog / (betaCog + n). More observations reduce u0.
		 */
		double n = s + f;
		double u0 = betaCog / (betaCog + n);

		/*
		 * Environmental uncertainty:
		 * e0 = alphaEnv * Pout. Pout is calculated by LinkEnvironmentModel.
		 */
		double e0 = alphaEnv * pout;

		double z = b0 + d0 + u0 + e0;
		double b;
		double e;
		double u;
		double d;
		if (z <= 0) {
			b = 0.5;
			e = 0.0;
			u = 0.5;
			d = 0.0;
		}
		else {
			b = b0 / z;
			d = d0 / z;
			u = u0 / z;
			e = e0 / z;
		}

		TrustVector vector = new TrustVector(b, e, u, d);
		vector.normalize();

		if (this.enableEATR) {
			/*
			 * EATR: a high outage probability means part of distrust may be
			 * caused by the environment, so move etaEnv * Pout * d from d to e.
			 */
			double dEnv = etaEnv * pout * vector.d;
			vector.d = vector.d - dEnv;
			vector.e = vector.e + dEnv;
			vector.normalize();
		}

		/*
		 * Hidden environmental attack correction: if failures concentrate in
		 * bad environments, move part of e and u back to distrust.
		 */
		if (pout > envThreshold) {
			edge.highEnvTotalCount += 1;
			if (!success) {
				edge.highEnvFailureCount += 1;
			}

			if (edge.highEnvTotalCount >= minHiddenSamples) {
				double h = (1.0 * edge.highEnvFailureCount) /
						edge.highEnvTotalCount;
				if (h > hiddenRiskThreshold) {
					double risk = h - hiddenRiskThreshold;
					double transferEnv = rhoHidden * risk * vector.e;
					double transferCog = rhoHidden * risk * vector.u;

					vector.e = vector.e - transferEnv;
					vector.u = vector.u - transferCog;
					vector.d = vector.d + transferEnv + transferCog;
					vector.normalize();
				}
			}
		}

		applyEventEvidence(edge, vector);
		double lt = calculateScalarTrust(vector);

		edge.vector = vector;
		edge.scalarTrust = lt;
		edge.lastUpdateTime = currentTime;
		edge.lastPout = pout;
		edge.addTrustHistory(lt);
	}

	public void updateByUncertainForwardResult(TrustEdge edge, double pout,
			double currentTime) {
		pout = clamp(pout);
		applyEnvironmentalTimeDecay(edge, pout, currentTime);
		edge.uncertainForwardCount += 1.0;

		TrustVector vector = edge.vector.copy();

		/*
		 * Uncertain forwarding evidence:
		 * If the delegated node had no later forwarding opportunity before the
		 * observation timeout, the timeout is DTN contact scarcity rather than
		 * clear malicious behavior. Do not increase failCount. Instead move a
		 * small committed mass from belief/disbelief to uncertainty; high Pout
		 * sends more of that mass to environmental uncertainty, otherwise to
		 * cognitive uncertainty.
		 */
		double committed = vector.b + vector.d;
		double transfer = Math.min(0.06, committed);
		double envPart = transfer * (0.3 + 0.7 * pout);
		double cogPart = transfer - envPart;

		if (committed > 0.0) {
			double beliefShare = vector.b / committed;
			double distrustShare = vector.d / committed;
			vector.b = vector.b - transfer * beliefShare;
			vector.d = vector.d - transfer * distrustShare;
		}
		vector.e = vector.e + envPart;
		vector.u = vector.u + cogPart;
		vector.normalize();

		applyEventEvidence(edge, vector);
		edge.vector = vector;
		edge.scalarTrust = calculateScalarTrust(vector);
		edge.lastUpdateTime = currentTime;
		edge.lastPout = pout;
		edge.addTrustHistory(edge.scalarTrust);
	}

	public TrustEdge updateByEventResult(TrustTable table,
			int evaluatorAddress, int targetAddress, boolean consistent,
			boolean uncertain, double pout, double currentTime) {
		TrustEdge edge = table.getOrCreateEdge(
				String.valueOf(evaluatorAddress), String.valueOf(targetAddress));
		updateByEventResult(edge, consistent, uncertain, pout, currentTime);
		return edge;
	}

	public void updateByEventResult(TrustEdge edge, boolean consistent,
			boolean uncertain, double pout, double currentTime) {
		pout = clamp(pout);
		applyEnvironmentalTimeDecay(edge, pout, currentTime);

		TrustVector vector = edge.vector.copy();
		if (uncertain) {
			edge.uncertainEventCount += 1.0;
			/*
			 * Uncertain event consensus: do not punish. Move a small part of
			 * committed belief/distrust mass back to cognitive uncertainty.
			 */
			double beliefBack = 0.05 * vector.b;
			double distrustBack = 0.05 * vector.d;
			vector.b -= beliefBack;
			vector.d -= distrustBack;
			vector.u += beliefBack + distrustBack;
		}
		else if (consistent) {
			edge.trueEventCount += 1.0;
			/*
			 * Consistent semantic report: reward by moving part of cognitive
			 * uncertainty into belief. This is event evidence, not forwarding
			 * cooperation evidence.
			 */
			double reward = eventReward * vector.u;
			vector.u -= reward;
			vector.b += reward;
		}
		else {
			edge.falseEventCount += 1.0;
			/*
			 * Inconsistent semantic report: stronger punishment. Move
			 * cognitive uncertainty and a small part of belief into distrust.
			 */
			double transferCog = eventPenalty * vector.u;
			double transferBelief = eventPenalty * vector.b;
			vector.u -= transferCog;
			vector.b -= transferBelief;
			vector.d += transferCog + transferBelief;
		}

		vector.normalize();
		edge.vector = vector;
		edge.scalarTrust = calculateScalarTrust(vector);
		edge.lastUpdateTime = currentTime;
		edge.lastPout = pout;
		edge.addTrustHistory(edge.scalarTrust);
	}

	private void applyEventEvidence(TrustEdge edge, TrustVector vector) {
		double totalEvents = edge.trueEventCount + edge.falseEventCount +
				edge.uncertainEventCount;
		if (totalEvents <= 0.0) {
			return;
		}

		/*
		 * Event semantic evidence is separate from forwarding cooperation.
		 * Consistent reports move cognitive uncertainty into belief; false
		 * reports move cognitive uncertainty and belief into distrust. This
		 * preserves event-consistency effects when forwarding evidence later
		 * recomputes the base four-dimensional trust vector.
		 */
		double trueRatio = edge.trueEventCount / totalEvents;
		double falseRatio = edge.falseEventCount / totalEvents;
		double uncertainRatio = edge.uncertainEventCount / totalEvents;

		if (falseRatio > trueRatio) {
			double transferCog = eventPenalty * falseRatio * vector.u;
			double transferBelief = eventPenalty * falseRatio * vector.b;
			vector.u -= transferCog;
			vector.b -= transferBelief;
			vector.d += transferCog + transferBelief;
		}
		else if (trueRatio > falseRatio) {
			double reward = eventReward * trueRatio * vector.u;
			vector.u -= reward;
			vector.b += reward;
		}

		if (uncertainRatio > 0.0) {
			double beliefBack = 0.05 * uncertainRatio * vector.b;
			double distrustBack = 0.05 * uncertainRatio * vector.d;
			vector.b -= beliefBack;
			vector.d -= distrustBack;
			vector.u += beliefBack + distrustBack;
		}
		vector.normalize();
	}

	private void applyEnvironmentalTimeDecay(TrustEdge edge, double pout,
			double currentTime) {
		double deltaT = currentTime - edge.lastUpdateTime;
		if (deltaT <= 0 || edge.getInteractionCount() <= 0) {
			edge.lastDecayFactor = 1.0;
			return;
		}

		/*
		 * Environment-aware time decay from section 4.6:
		 * decay = exp(-baseDecayRate * deltaT *
		 *        (1 + envDecaySensitivity * Pout)).
		 * Older forwarding evidence fades faster under unreliable links, so
		 * both Beta evidence masses are discounted before adding new evidence.
		 */
		double decay = Math.exp(-baseDecayRate * deltaT *
				(1.0 + envDecaySensitivity * pout));
		decay = clamp(decay);
		edge.successCount = edge.successCount * decay;
		edge.failCount = edge.failCount * decay;
		edge.lastDecayFactor = decay;
	}

	/**
	 * Local scalar trust from paper Section 4.10:
	 * LT = (b + aC*u + aE*e) / (b + aC*u + aE*e + aD*d).
	 * Cognitive and environmental uncertainty contribute cautiously to the
	 * numerator, while disbelief remains an explicit denominator penalty.
	 */
	public double calculateScalarTrust(TrustVector vector) {
		return vector.scalarTrust(this.scalarWeightC, this.scalarWeightE,
				this.scalarWeightD);
	}

	public double clamp(double value) {
		if (value < 0) {
			return 0.0;
		}
		if (value > 1) {
			return 1.0;
		}
		return value;
	}

	public void setBetaCog(double betaCog) {
		this.betaCog = betaCog;
	}

	public void setAlphaEnv(double alphaEnv) {
		this.alphaEnv = alphaEnv;
	}

	public void setEtaEnv(double etaEnv) {
		this.etaEnv = etaEnv;
	}

	public void setBaseDecayRate(double baseDecayRate) {
		this.baseDecayRate = baseDecayRate;
	}

	public void setEnvDecaySensitivity(double envDecaySensitivity) {
		this.envDecaySensitivity = envDecaySensitivity;
	}

	public void setEventReward(double eventReward) {
		this.eventReward = clamp(eventReward);
	}

	public void setEventPenalty(double eventPenalty) {
		this.eventPenalty = clamp(eventPenalty);
	}

	public void setEnableEATR(boolean enableEATR) {
		this.enableEATR = enableEATR;
	}

	public void setScalarTrustWeights(double weightC, double weightE,
			double weightD) {
		this.scalarWeightC = clamp(weightC);
		this.scalarWeightE = clamp(weightE);
		if (this.scalarWeightE > this.scalarWeightC) {
			this.scalarWeightE = this.scalarWeightC;
		}
		if (weightD < 1.0) {
			this.scalarWeightD = 1.0;
		}
		else {
			this.scalarWeightD = weightD;
		}
	}
}
