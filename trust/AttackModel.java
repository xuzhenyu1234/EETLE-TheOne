package trust;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import core.Settings;

/**
 * Configurable attack assignment model.
 *
 * The default addressRange mode preserves the original experiment layout:
 * 40-43 BLACKHOLE, 44-47 ON_OFF, 48-51 FALSE_EVENT,
 * 52-55 ENV_CAMOUFLAGE, and 56-59 CROSS_REGION.
 */
public class AttackModel {
	public static final String MODE_ADDRESS_RANGE = "addressRange";
	public static final String MODE_RANDOM_RATIO = "randomRatio";

	private boolean attackEnabled = true;
	private long attackSeed = 12345L;
	private double attackRatio = 0.30;
	private double blackholeRatio = 0.20;
	private double onOffRatio = 0.20;
	private double falseEventRatio = 0.20;
	private double envCamouflageRatio = 0.20;
	private double crossRegionRatio = 0.20;
	private String attackAssignmentMode = MODE_ADDRESS_RANGE;
	private int hostCount = 60;
	private Map<Integer, AttackProfile> profileByAddress;
	private boolean randomAssignmentBuilt;

	public AttackModel() {
		this.profileByAddress = new HashMap<Integer, AttackProfile>();
		this.randomAssignmentBuilt = false;
	}

	public void configure(Settings settings) {
		if (settings == null) {
			return;
		}
		if (settings.contains("attackEnabled")) {
			this.attackEnabled = settings.getBoolean("attackEnabled");
		}
		if (settings.contains("attackSeed")) {
			this.attackSeed = (long)settings.getInt("attackSeed");
		}
		if (settings.contains("attackRatio")) {
			this.attackRatio = clamp(settings.getDouble("attackRatio"));
		}
		if (settings.contains("blackholeRatio")) {
			this.blackholeRatio = clamp(settings.getDouble("blackholeRatio"));
		}
		if (settings.contains("onOffRatio")) {
			this.onOffRatio = clamp(settings.getDouble("onOffRatio"));
		}
		if (settings.contains("falseEventRatio")) {
			this.falseEventRatio = clamp(settings.getDouble("falseEventRatio"));
		}
		if (settings.contains("envCamouflageRatio")) {
			this.envCamouflageRatio = clamp(
					settings.getDouble("envCamouflageRatio"));
		}
		if (settings.contains("crossRegionRatio")) {
			this.crossRegionRatio = clamp(settings.getDouble("crossRegionRatio"));
		}
		if (settings.contains("attackAssignmentMode")) {
			this.attackAssignmentMode =
					settings.getSetting("attackAssignmentMode");
		}

		Settings groupSettings = new Settings("Group");
		if (groupSettings.contains("nrofHosts")) {
			this.hostCount = groupSettings.getInt("nrofHosts");
		}

		this.profileByAddress.clear();
		this.randomAssignmentBuilt = false;
	}

	public AttackProfile getAttackProfile(int address) {
		return new AttackProfile(getAttackType(address));
	}

	public AttackType getAttackType(int address) {
		if (!this.attackEnabled) {
			return AttackType.NORMAL;
		}
		if (MODE_RANDOM_RATIO.equals(this.attackAssignmentMode)) {
			ensureRandomAssignment();
			AttackProfile profile = this.profileByAddress.get(
					new Integer(address));
			if (profile == null) {
				return AttackType.NORMAL;
			}
			return profile.getType();
		}
		return getAddressRangeAttackType(address);
	}

	private AttackType getAddressRangeAttackType(int address) {
		if (address >= 40 && address <= 43) {
			return AttackType.BLACKHOLE;
		}
		if (address >= 44 && address <= 47) {
			return AttackType.ON_OFF;
		}
		if (address >= 48 && address <= 51) {
			return AttackType.FALSE_EVENT;
		}
		if (address >= 52 && address <= 55) {
			return AttackType.ENV_CAMOUFLAGE;
		}
		if (address >= 56 && address <= 59) {
			return AttackType.CROSS_REGION;
		}
		return AttackType.NORMAL;
	}

	private void ensureRandomAssignment() {
		if (this.randomAssignmentBuilt) {
			return;
		}
		this.randomAssignmentBuilt = true;

		List<Integer> addresses = new ArrayList<Integer>();
		for (int i = 0; i < this.hostCount; i++) {
			addresses.add(new Integer(i));
		}
		Collections.shuffle(addresses, new Random(this.attackSeed));

		int attackCount = (int)Math.round(this.hostCount * this.attackRatio);
		if (attackCount < 0) {
			attackCount = 0;
		}
		if (attackCount > this.hostCount) {
			attackCount = this.hostCount;
		}

		int[] counts = calculateTypeCounts(attackCount);
		int blackholeCount = counts[0];
		int onOffCount = counts[1];
		int falseEventCount = counts[2];
		int envCount = counts[3];
		int crossCount = counts[4];

		int index = 0;
		index = assign(addresses, index, blackholeCount, AttackType.BLACKHOLE);
		index = assign(addresses, index, onOffCount, AttackType.ON_OFF);
		index = assign(addresses, index, falseEventCount,
				AttackType.FALSE_EVENT);
		index = assign(addresses, index, envCount,
				AttackType.ENV_CAMOUFLAGE);
		assign(addresses, index, crossCount, AttackType.CROSS_REGION);
	}

	private int[] calculateTypeCounts(int attackCount) {
		double[] ratios = new double[] {
				this.blackholeRatio,
				this.onOffRatio,
				this.falseEventRatio,
				this.envCamouflageRatio,
				this.crossRegionRatio
		};
		int[] counts = new int[ratios.length];
		double[] fractions = new double[ratios.length];
		double sum = 0.0;
		for (int i = 0; i < ratios.length; i++) {
			sum += ratios[i];
		}
		if (sum <= 0.0) {
			return counts;
		}

		int assigned = 0;
		for (int i = 0; i < ratios.length; i++) {
			double exact = attackCount * ratios[i] / sum;
			counts[i] = (int)Math.floor(exact);
			fractions[i] = exact - counts[i];
			assigned += counts[i];
		}

		while (assigned < attackCount) {
			int best = 0;
			for (int i = 1; i < fractions.length; i++) {
				if (fractions[i] > fractions[best]) {
					best = i;
				}
			}
			counts[best]++;
			fractions[best] = -1.0;
			assigned++;
		}
		return counts;
	}

	private int assign(List<Integer> addresses, int index, int count,
			AttackType type) {
		for (int i = 0; i < count && index < addresses.size(); i++) {
			Integer address = addresses.get(index++);
			this.profileByAddress.put(address, new AttackProfile(type));
		}
		return index;
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
