package trust;

import core.SimClock;

/**
 * Attack behavior profile for one node.
 *
 * The profile only decides whether an attack is active. The router decides
 * where the action can safely be inserted into THE ONE message lifecycle.
 */
public class AttackProfile {
	private AttackType type;
	private double dropProbability;
	private double falseEventProbability;
	private double envAttackThreshold;
	private double normalDuration;
	private double attackDuration;
	private double crossRegionWarmup;

	public AttackProfile(AttackType type) {
		this.type = type;
		this.dropProbability = 0.9;
		this.falseEventProbability = 0.8;
		this.envAttackThreshold = 0.6;
		this.normalDuration = 600.0;
		this.attackDuration = 600.0;
		this.crossRegionWarmup = 900.0;
	}

	public AttackProfile(AttackProfile p) {
		this.type = p.type;
		this.dropProbability = p.dropProbability;
		this.falseEventProbability = p.falseEventProbability;
		this.envAttackThreshold = p.envAttackThreshold;
		this.normalDuration = p.normalDuration;
		this.attackDuration = p.attackDuration;
		this.crossRegionWarmup = p.crossRegionWarmup;
	}

	public AttackType getType() {
		return this.type;
	}

	/**
	 * Returns true when the configured attack is active at the current time
	 * and under the current link outage probability.
	 */
	public boolean isMaliciousNow(double linkOutageProbability) {
		if (this.type == AttackType.NORMAL) {
			return false;
		}
		if (this.type == AttackType.ON_OFF) {
			return isOnOffAttackActive();
		}
		if (this.type == AttackType.ENV_CAMOUFLAGE) {
			return isEnvCamouflageActive(linkOutageProbability);
		}
		return true;
	}

	/**
	 * Drop decision for forwarding attacks.
	 * Blackhole and cross-region attacks use dropProbability directly.
	 * On-off drops only during the attack phase. Environmental camouflage
	 * drops only when Pout reaches the configured environmental threshold.
	 */
	public boolean shouldDrop(double linkOutageProbability, double randomValue) {
		if (this.type == AttackType.BLACKHOLE ||
				this.type == AttackType.CROSS_REGION) {
			return randomValue < this.dropProbability;
		}
		if (this.type == AttackType.ON_OFF) {
			return isOnOffAttackActive() &&
					randomValue < this.dropProbability;
		}
		if (this.type == AttackType.ENV_CAMOUFLAGE) {
			return isEnvCamouflageActive(linkOutageProbability) &&
					randomValue < this.dropProbability;
		}
		return false;
	}

	/**
	 * False event injection decision. FALSE_EVENT nodes are always eligible;
	 * ON_OFF nodes inject only during attack phases; ENV_CAMOUFLAGE nodes use
	 * the environmental trigger through isMaliciousNow before this probability
	 * is applied by the caller.
	 */
	public boolean shouldInjectFalseEvent(double randomValue) {
		if (this.type == AttackType.FALSE_EVENT) {
			return randomValue < this.falseEventProbability;
		}
		if (this.type == AttackType.ON_OFF) {
			return isOnOffAttackActive() &&
					randomValue < this.falseEventProbability;
		}
		if (this.type == AttackType.ENV_CAMOUFLAGE) {
			return randomValue < this.falseEventProbability;
		}
		return false;
	}

	/**
	 * On-off attack timing: 0-600s normal, 600-1200s malicious, then repeat.
	 */
	public boolean isOnOffAttackActive() {
		double cycle = this.normalDuration + this.attackDuration;
		if (cycle <= 0) {
			return false;
		}
		double t = SimClock.getTime() % cycle;
		return t >= this.normalDuration;
	}

	/**
	 * Environmental camouflage is active only when Pout is high enough.
	 */
	public boolean isEnvCamouflageActive(double linkOutageProbability) {
		return linkOutageProbability >= this.envAttackThreshold;
	}

	public double getCrossRegionWarmup() {
		return this.crossRegionWarmup;
	}
}
