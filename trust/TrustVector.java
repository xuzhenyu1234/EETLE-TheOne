package trust;

/**
 * Four-dimensional local trust vector.
 * b: belief/trust, e: environmental uncertainty,
 * u: cognitive uncertainty, d: distrust.
 */
public class TrustVector {
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
