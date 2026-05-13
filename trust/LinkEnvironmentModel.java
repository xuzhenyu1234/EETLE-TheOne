package trust;

import java.util.HashMap;
import java.util.Map;

/**
 * Link environment model for outage probability.
 *
 * This class follows the paper path:
 * distance -> path gain -> received power -> SINR -> channel capacity
 * -> outage indicator -> Pout.
 *
 * THE ONE 1.6.0 does not provide a full physical layer, so this class uses
 * an engineering approximation based on distance, path loss, noise, and
 * interference. Environmental uncertainty is not assigned here; this class
 * only estimates link outage probability Pout.
 */
public class LinkEnvironmentModel {
	private double transmitPower = 1.0;
	private double noisePower = 1e-9;
	private double interferencePower = 1e-10;
	private double bandwidth = 1e6;
	private double requiredRate = 1e5;
	private double pathLossExponent = 2.0;
	private double epsilon = 1e-6;
	private int minSamples = 5;

	private Map<String, Integer> outageCounts;
	private Map<String, Integer> sampleCounts;

	public LinkEnvironmentModel() {
		this.outageCounts = new HashMap<String, Integer>();
		this.sampleCounts = new HashMap<String, Integer>();
	}

	private String makeKey(String nodeA, String nodeB) {
		return nodeA + "->" + nodeB;
	}

	/**
	 * Received power: P_r,ij(t) = P_t * pathGain_ij(t).
	 * pathGain = 1 / ((distance + epsilon)^pathLossExponent + epsilon).
	 */
	public double calculateReceivedPower(double distance) {
		if (distance < 0) {
			distance = 0.0;
		}
		double pathGain = 1.0 /
				(Math.pow(distance + epsilon, pathLossExponent) + epsilon);
		return transmitPower * pathGain;
	}

	/**
	 * SINR_ij(t) = P_r,ij(t) / (N0 + I_ij(t)).
	 */
	public double calculateSINR(double distance) {
		double denominator = noisePower + interferencePower;
		if (denominator <= 0) {
			return Double.MAX_VALUE / 4.0;
		}
		return calculateReceivedPower(distance) / denominator;
	}

	/**
	 * Channel capacity: C_ij(t) = B * log2(1 + SINR_ij(t)).
	 */
	public double calculateCapacity(double sinr) {
		if (sinr < 0) {
			sinr = 0.0;
		}
		return bandwidth * (Math.log(1.0 + sinr) / Math.log(2.0));
	}

	/**
	 * Outage SINR threshold: gamma_th = 2^(R_req / B) - 1.
	 */
	public double calculateGammaThreshold() {
		return Math.pow(2.0, requiredRate / bandwidth) - 1.0;
	}

	/**
	 * Returns 1 if SINR is below the outage threshold, otherwise 0.
	 */
	public int calculateInstantOutage(double distance) {
		double sinr = calculateSINR(distance);
		double gammaThreshold = calculateGammaThreshold();
		return sinr < gammaThreshold ? 1 : 0;
	}

	/**
	 * Updates sliding link statistics and returns Pout.
	 * Pout_ij(t) is estimated as outageCount / sampleCount with smoothing
	 * during the cold-start period.
	 */
	public double updateAndGetPout(String nodeA, String nodeB, double distance) {
		String key = makeKey(nodeA, nodeB);
		int outage = calculateInstantOutage(distance);

		Integer oldSamples = this.sampleCounts.get(key);
		Integer oldOutages = this.outageCounts.get(key);
		int sampleCount = oldSamples == null ? 0 : oldSamples.intValue();
		int outageCount = oldOutages == null ? 0 : oldOutages.intValue();

		sampleCount++;
		outageCount += outage;

		this.sampleCounts.put(key, new Integer(sampleCount));
		this.outageCounts.put(key, new Integer(outageCount));

		double pout;
		if (sampleCount < minSamples) {
			pout = (outageCount + 0.5) / (sampleCount + 1.0);
		}
		else {
			pout = (1.0 * outageCount) / sampleCount;
		}

		return clamp(pout);
	}

	public double getPout(String nodeA, String nodeB) {
		String key = makeKey(nodeA, nodeB);
		Integer samples = this.sampleCounts.get(key);
		Integer outages = this.outageCounts.get(key);
		if (samples == null || samples.intValue() == 0 || outages == null) {
			return 0.5;
		}
		return clamp((1.0 * outages.intValue()) / samples.intValue());
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

	public void setTransmitPower(double transmitPower) {
		if (transmitPower > 0.0) {
			this.transmitPower = transmitPower;
		}
	}

	public void setNoisePower(double noisePower) {
		if (noisePower >= 0.0) {
			this.noisePower = noisePower;
		}
	}

	public void setInterferencePower(double interferencePower) {
		if (interferencePower >= 0.0) {
			this.interferencePower = interferencePower;
		}
	}

	public void setBandwidth(double bandwidth) {
		if (bandwidth > 0.0) {
			this.bandwidth = bandwidth;
		}
	}

	public void setRequiredRate(double requiredRate) {
		if (requiredRate >= 0.0) {
			this.requiredRate = requiredRate;
		}
	}

	public void setPathLossExponent(double pathLossExponent) {
		if (pathLossExponent > 0.0) {
			this.pathLossExponent = pathLossExponent;
		}
	}

	public void setEpsilon(double epsilon) {
		if (epsilon > 0.0) {
			this.epsilon = epsilon;
		}
	}

	public void setMinSamples(int minSamples) {
		if (minSamples > 0) {
			this.minSamples = minSamples;
		}
	}
}
