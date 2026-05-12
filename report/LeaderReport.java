/*
 * Robust Leader election report for EETLE.
 */
package report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import routing.EETLERouter;
import routing.MessageRouter;
import trust.LeaderCandidate;
import trust.LeaderElection;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

/**
 * Outputs both the current Leader overview and per-candidate election state.
 */
public class LeaderReport extends Report implements UpdateListener {
	private static final String HEADER =
			"time,recordType,currentLeader,node,attackType,globalTrust," +
			"trustStability,communicationQuality,regionConstraintFactor," +
			"baseScore,finalScore,eligible,rejectReason,isCurrentLeader," +
			"leaderChangeCount,lastSwitchReason";
	private static final double DEFAULT_INTERVAL = 100.0;

	private double lastReportTime = -1.0;
	private double interval = DEFAULT_INTERVAL;

	public LeaderReport() {
		Settings settings = getSettings();
		if (settings.contains(INTERVAL_SETTING)) {
			this.interval = settings.getDouble(INTERVAL_SETTING);
		}
		init();
	}

	@Override
	protected void init() {
		super.init();
		write(HEADER);
	}

	public void updated(List<DTNHost> hosts) {
		double now = getSimTime();
		if (this.lastReportTime >= 0 &&
				now - this.lastReportTime < this.interval) {
			return;
		}
		this.lastReportTime = now;
		writeElectionState(hosts);
	}

	private void writeElectionState(List<DTNHost> hosts) {
		LeaderElection election = EETLERouter.getLeaderElection();
		if (election == null) {
			return;
		}

		Map<Integer, String> attackTypes = getAttackTypes(hosts);
		int currentLeader = election.getCurrentLeaderAddress();

		write(format(getSimTime()) + ",LEADER," +
				currentLeader + "," +
				currentLeader + "," +
				getAttackType(attackTypes, currentLeader) + "," +
				",,,,," +
				format(election.getCurrentLeaderScore()) + "," +
				",," +
				"true," +
				election.getLeaderChangeCount() + "," +
				election.getLastSwitchReason());

		List<LeaderCandidate> candidates = election.getLastCandidates();
		for (int i = 0; i < candidates.size(); i++) {
			LeaderCandidate c = candidates.get(i);
			boolean isCurrentLeader = c.getAddress() == currentLeader;
			write(format(getSimTime()) + ",CANDIDATE," +
					currentLeader + "," +
					c.getAddress() + "," +
					getAttackType(attackTypes, c.getAddress()) + "," +
					format(c.getGlobalTrust()) + "," +
					format(c.getTrustStability()) + "," +
					format(c.getCommunicationQuality()) + "," +
					format(c.getRegionConstraintFactor()) + "," +
					format(c.getBaseScore()) + "," +
					format(c.getFinalScore()) + "," +
					c.isEligible() + "," +
					c.getRejectReason() + "," +
					isCurrentLeader + "," +
					election.getLeaderChangeCount() + "," +
					election.getLastSwitchReason());
		}
	}

	private Map<Integer, String> getAttackTypes(List<DTNHost> hosts) {
		Map<Integer, String> attackTypes = new HashMap<Integer, String>();
		for (int i = 0; i < hosts.size(); i++) {
			DTNHost host = hosts.get(i);
			MessageRouter router = host.getRouter();
			if (router instanceof EETLERouter) {
				EETLERouter eetle = (EETLERouter)router;
				attackTypes.put(new Integer(host.getAddress()),
						eetle.getAttackType().toString());
			}
		}
		return attackTypes;
	}

	private String getAttackType(Map<Integer, String> attackTypes,
			int address) {
		String value = attackTypes.get(new Integer(address));
		if (value == null) {
			return "UNKNOWN";
		}
		return value;
	}
}
