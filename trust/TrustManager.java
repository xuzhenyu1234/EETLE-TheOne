package trust;

/**
 * Implements local trust update formulas for EETLE.
 * TrustManager receives Pout from LinkEnvironmentModel and maps it to
 * environmental uncertainty. It does not calculate SINR directly.
 */
public class TrustManager {
	private double betaCog = 2.0;
	private double alphaEnv = 0.7;
	private double etaEnv = 0.6;

	private double envThreshold = 0.6;
	private double hiddenRiskThreshold = 0.5;
	private double rhoHidden = 0.2;
	private int minHiddenSamples = 5;

	private double lambdaD = 0.7;
	private double lambdaE = 0.3;
	private double lambdaU = 0.2;

	public void updateByForwardResult(TrustEdge edge, boolean success,
			double pout, double currentTime) {
		if (success) {
			edge.successCount += 1;
		}
		else {
			edge.failCount += 1;
		}

		int s = edge.successCount;
		int f = edge.failCount;

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
		int n = s + f;
		double u0 = betaCog / (betaCog + n);

		/*
		 * Environmental uncertainty:
		 * e0 = alphaEnv * Pout. Pout is calculated by LinkEnvironmentModel.
		 */
		pout = clamp(pout);
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

		/*
		 * EATR: a high outage probability means part of distrust may be
		 * caused by the environment, so move etaEnv * Pout * d from d to e.
		 */
		double dEnv = etaEnv * pout * vector.d;
		vector.d = vector.d - dEnv;
		vector.e = vector.e + dEnv;
		vector.normalize();

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

		double lt = calculateScalarTrust(vector);

		edge.vector = vector;
		edge.scalarTrust = lt;
		edge.lastUpdateTime = currentTime;
		edge.lastPout = pout;
		edge.addTrustHistory(lt);
	}

	/**
	 * Local scalar trust:
	 * LT = b / (b + lambdaD*d + lambdaE*e + lambdaU*u).
	 * Distrust has the strongest penalty, environmental uncertainty is weaker,
	 * and cognitive uncertainty has the weakest penalty.
	 */
	public double calculateScalarTrust(TrustVector vector) {
		double denominator = vector.b + lambdaD * vector.d +
				lambdaE * vector.e + lambdaU * vector.u;
		if (denominator <= 0) {
			return 0.5;
		}
		return clamp(vector.b / denominator);
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
}
