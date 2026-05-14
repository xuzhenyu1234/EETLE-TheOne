package trust;

/**
 * Four-dimensional local trust vector.
 * b: belief/trust, e: environmental uncertainty,
 * u: cognitive uncertainty, d: distrust.
 */
public class TrustVector {
	private static final double DEFAULT_SCALAR_WEIGHT_C = 0.5;
	private static final double DEFAULT_SCALAR_WEIGHT_E = 0.3;
	private static final double DEFAULT_SCALAR_WEIGHT_D = 1.5;

	public double b;
	public double e;
	public double u;
	public double d;

	public TrustVector() {
		this.b = 0.5;
		this.e = 0.0;
		this.u = 0.5;
		this.d = 0.0;
	}

	public TrustVector(double b, double e, double u, double d) {
		this.b = b;
		this.e = e;
		this.u = u;
		this.d = d;
		normalize();
	}

	/**
	 * Normalizes the vector so that b + e + u + d = 1.
	 * Values are clamped first to avoid invalid negative mass.
	 */
	public void normalize() {
		clamp();
		double sum = b + e + u + d;

		if (sum <= 0) {
			this.b = 0.5;
			this.e = 0.0;
			this.u = 0.5;
			this.d = 0.0;
			return;
		}

		this.b = this.b / sum;
		this.e = this.e / sum;
		this.u = this.u / sum;
		this.d = this.d / sum;
		clamp();
	}

	/**
	 * Clamps all trust components to [0, 1].
	 */
	public void clamp() {
		this.b = clampValue(this.b);
		this.e = clampValue(this.e);
		this.u = clampValue(this.u);
		this.d = clampValue(this.d);
	}

	public TrustVector copy() {
		return new TrustVector(this.b, this.e, this.u, this.d);
	}

	public double getBelief() {
		return this.b;
	}

	public double getEnvUncertainty() {
		return this.e;
	}

	public double getCognitiveUncertainty() {
		return this.u;
	}

	public double getDisbelief() {
		return this.d;
	}

	/**
	 * Local scalar trust with default paper weights.
	 */
	public double scalarTrust() {
		return scalarTrust(DEFAULT_SCALAR_WEIGHT_C, DEFAULT_SCALAR_WEIGHT_E,
				DEFAULT_SCALAR_WEIGHT_D);
	}

	/**
	 * Paper Section 4.10 local scalar trust:
	 * LT = (T + aC*C + aE*E) / (T + aC*C + aE*E + aD*D),
	 * where T=belief, C=cognitive uncertainty, E=environmental uncertainty,
	 * and D=disbelief. Weights satisfy 0 <= aE <= aC <= 1 and aD >= 1.
	 */
	public double scalarTrust(double aC, double aE, double aD) {
		aC = clampValue(aC);
		aE = clampValue(aE);
		if (aE > aC) {
			aE = aC;
		}
		if (aD < 1.0) {
			aD = 1.0;
		}

		double numerator = this.b + aC * this.u + aE * this.e;
		double denominator = numerator + aD * this.d;
		if (denominator <= 0) {
			return 0.5;
		}
		return clampValue(numerator / denominator);
	}

	private double clampValue(double value) {
		if (value < 0) {
			return 0.0;
		}
		if (value > 1) {
			return 1.0;
		}
		return value;
	}

	public String toString() {
		return "TrustVector[b=" + b + ", e=" + e + ", u=" + u +
				", d=" + d + "]";
	}
}
