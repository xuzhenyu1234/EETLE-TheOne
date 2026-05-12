/*
 * EETLE basic router for online local trust update.
 */
package routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import trust.AttackProfile;
import trust.AttackType;
import trust.LinkEnvironmentModel;
import trust.TrustEdge;
import trust.TrustManager;
import trust.TrustTable;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * Basic EETLE router.
 * Uses Epidemic-style forwarding, but filters ordinary relay candidates by
 * local trust and updates local trust edges after transfer success/failure.
 */
public class EETLERouter extends ActiveRouter {
	public static final String TRUST_THRESHOLD_SETTING = "trustThreshold";
	private static final String EETLE_NS = "EETLERouter";

	private TrustTable trustTable;
	private TrustManager trustManager;
	private LinkEnvironmentModel linkEnvironmentModel;
	private AttackProfile attackProfile;
	private Random attackRng;

	private double trustThreshold = 0.45;
	private String homeRegion;
	private String currentRegion;
	private double regionEnterTime;

	private int droppedByAttack;
	private int falseEventsInjected;
	private int attackAttempts;
	private int blackholeDrops;
	private int onOffDrops;
	private int envCamouflageDrops;
	private int crossRegionDrops;
	private int falseEventCount;

	public EETLERouter(Settings s) {
		super(s);
		this.trustTable = new TrustTable();
		this.trustManager = new TrustManager();
		this.linkEnvironmentModel = new LinkEnvironmentModel();
		this.attackProfile = new AttackProfile(AttackType.NORMAL);
		this.attackRng = new Random(1);
		this.homeRegion = null;
		this.currentRegion = null;
		this.regionEnterTime = 0.0;
		clearAttackCounters();

		Settings eetleSettings = new Settings(EETLE_NS);
		if (eetleSettings.contains(TRUST_THRESHOLD_SETTING)) {
			this.trustThreshold =
					eetleSettings.getDouble(TRUST_THRESHOLD_SETTING);
		}
	}

	protected EETLERouter(EETLERouter r) {
		super(r);
		this.trustTable = new TrustTable();
		this.trustManager = new TrustManager();
		this.linkEnvironmentModel = new LinkEnvironmentModel();
		this.attackProfile = new AttackProfile(r.attackProfile);
		this.attackRng = new Random(1);
		this.trustThreshold = r.trustThreshold;
		this.homeRegion = null;
		this.currentRegion = null;
		this.regionEnterTime = 0.0;
		clearAttackCounters();
	}

	@Override
	public void init(DTNHost host, List<core.MessageListener> mListeners) {
		super.init(host, mListeners);
		this.attackProfile = new AttackProfile(
				getAttackTypeForAddress(host.getAddress()));
		this.attackRng = new Random(host.getAddress() + 1);
		updateRegionState();
	}

	@Override
	public void update() {
		super.update();
		updateRegionState();
		if (isTransferring() || !canStartTransfer()) {
			return;
		}

		if (exchangeDeliverableMessages() != null) {
			return;
		}

		tryTrustedMessagesToConnections();
	}

	private Connection tryTrustedMessagesToConnections() {
		List<Connection> trustedConnections = getTrustedConnections();
		if (trustedConnections.size() == 0 || getNrofMessages() == 0) {
			return null;
		}

		List<Message> messages =
				new ArrayList<Message>(this.getMessageCollection());
		this.sortByQueueMode(messages);

		return tryTrustedMessages(messages, trustedConnections);
	}

	private Connection tryTrustedMessages(List<Message> messages,
			List<Connection> connections) {
		for (int i = 0; i < connections.size(); i++) {
			Connection con = connections.get(i);
			for (int j = 0; j < messages.size(); j++) {
				Message m = messages.get(j);
				double pout = calculatePout(getHost(),
						con.getOtherNode(getHost()));
				if (shouldDropForwarding(pout)) {
					continue;
				}
				if (startTransfer(m, con) == RCV_OK) {
					return con;
				}
			}
		}

		return null;
	}

	private List<Connection> getTrustedConnections() {
		List<Connection> trustedConnections = new ArrayList<Connection>();
		String myId = getHost().toString();

		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			String otherId = other.toString();
			double trust = this.trustTable.getTrust(myId, otherId);

			if (trust >= this.trustThreshold) {
				trustedConnections.add(con);
			}
		}

		return trustedConnections;
	}

	private double calculatePout(DTNHost a, DTNHost b) {
		double distance = a.getLocation().distance(b.getLocation());
		return this.linkEnvironmentModel.updateAndGetPout(
				a.toString(), b.toString(), distance);
	}

	private boolean shouldDropForwarding(double pout) {
		AttackType type = this.attackProfile.getType();
		boolean drop = false;

		if (type == AttackType.CROSS_REGION) {
			drop = isCrossRegionAttackActive() &&
					this.attackProfile.shouldDrop(pout,
							this.attackRng.nextDouble());
		}
		else {
			drop = this.attackProfile.shouldDrop(pout,
					this.attackRng.nextDouble());
		}

		if (drop) {
			this.attackAttempts++;
			this.droppedByAttack++;
			if (type == AttackType.BLACKHOLE) {
				this.blackholeDrops++;
			}
			else if (type == AttackType.ON_OFF) {
				this.onOffDrops++;
			}
			else if (type == AttackType.ENV_CAMOUFLAGE) {
				this.envCamouflageDrops++;
			}
			else if (type == AttackType.CROSS_REGION) {
				this.crossRegionDrops++;
			}
		}

		return drop;
	}

	/**
	 * Event reporting hook for later event modules. A false-event attacker
	 * flips a binary event state: real 1 is reported as 0, and real 0 is
	 * reported as 1. Environmental camouflage can inject false events only
	 * when the current Pout satisfies the environmental trigger.
	 */
	public int reportEventState(int realState, double linkOutageProbability) {
		if (shouldInjectFalseEvent(linkOutageProbability)) {
			this.falseEventsInjected++;
			this.falseEventCount++;
			this.attackAttempts++;
			return realState == 0 ? 1 : 0;
		}
		return realState;
	}

	/**
	 * Event-type reporting hook for multi-type events. If a false event is
	 * injected, the reported type is selected from a type different from the
	 * real one. The selection uses the router's deterministic per-node RNG.
	 */
	public int reportEventType(int realEventType, int nrofEventTypes,
			double linkOutageProbability) {
		if (nrofEventTypes <= 1 ||
				!shouldInjectFalseEvent(linkOutageProbability)) {
			return realEventType;
		}

		this.falseEventsInjected++;
		this.falseEventCount++;
		this.attackAttempts++;

		int falseType = this.attackRng.nextInt(nrofEventTypes - 1);
		if (falseType >= realEventType) {
			falseType++;
		}
		return falseType;
	}

	private boolean shouldInjectFalseEvent(double linkOutageProbability) {
		AttackType type = this.attackProfile.getType();
		if (type == AttackType.ENV_CAMOUFLAGE &&
				!this.attackProfile.isEnvCamouflageActive(
						linkOutageProbability)) {
			return false;
		}
		if (type == AttackType.CROSS_REGION) {
			return false;
		}
		return this.attackProfile.shouldInjectFalseEvent(
				this.attackRng.nextDouble());
	}

	private AttackType getAttackTypeForAddress(int address) {
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

	private void updateRegionState() {
		if (getHost() == null || getHost().getLocation() == null) {
			return;
		}

		String newRegion = getRegionForHost(getHost());
		if (this.homeRegion == null) {
			this.homeRegion = newRegion;
			this.currentRegion = newRegion;
			this.regionEnterTime = SimClock.getTime();
			return;
		}

		if (this.currentRegion == null || !this.currentRegion.equals(newRegion)) {
			this.currentRegion = newRegion;
			this.regionEnterTime = SimClock.getTime();
		}
	}

	private String getRegionForHost(DTNHost host) {
		if (host.getLocation().getX() < 500.0) {
			return "REGION_A";
		}
		return "REGION_B";
	}

	private boolean isCrossRegionAttackActive() {
		if (this.attackProfile.getType() != AttackType.CROSS_REGION) {
			return false;
		}
		if (this.homeRegion == null || this.currentRegion == null ||
				this.homeRegion.equals(this.currentRegion)) {
			return false;
		}
		return SimClock.getTime() - this.regionEnterTime >=
				this.attackProfile.getCrossRegionWarmup();
	}

	private void clearAttackCounters() {
		this.droppedByAttack = 0;
		this.falseEventsInjected = 0;
		this.attackAttempts = 0;
		this.blackholeDrops = 0;
		this.onOffDrops = 0;
		this.envCamouflageDrops = 0;
		this.crossRegionDrops = 0;
		this.falseEventCount = 0;
	}

	private void updateTrust(Connection con, boolean success) {
		DTNHost from = getHost();
		DTNHost to = con.getOtherNode(from);
		double pout = calculatePout(from, to);
		TrustEdge edge = this.trustTable.getOrCreateEdge(
				from.toString(), to.toString());

		this.trustManager.updateByForwardResult(edge, success, pout,
				SimClock.getTime());
	}

	@Override
	protected void transferDone(Connection con) {
		updateTrust(con, true);
		super.transferDone(con);
	}

	@Override
	protected void transferAborted(Connection con) {
		updateTrust(con, false);
		super.transferAborted(con);
	}

	public TrustTable getTrustTable() {
		return this.trustTable;
	}

	public double getLocalTrust(String targetId) {
		return this.trustTable.getTrust(getHost().toString(), targetId);
	}

	public TrustEdge getTrustEdge(String targetId) {
		return this.trustTable.getOrCreateEdge(getHost().toString(), targetId);
	}

	public AttackType getAttackType() {
		return this.attackProfile.getType();
	}

	public boolean isCrossRegionAttackNode() {
		return this.attackProfile.getType() == AttackType.CROSS_REGION;
	}

	public boolean isInForeignRegion() {
		return this.homeRegion != null && this.currentRegion != null &&
				!this.homeRegion.equals(this.currentRegion);
	}

	public String getHomeRegion() {
		return this.homeRegion;
	}

	public String getCurrentRegion() {
		return this.currentRegion;
	}

	public double getRegionEnterTime() {
		return this.regionEnterTime;
	}

	public int getDroppedByAttack() {
		return this.droppedByAttack;
	}

	public int getFalseEventsInjected() {
		return this.falseEventsInjected;
	}

	public int getAttackAttempts() {
		return this.attackAttempts;
	}

	public int getBlackholeDrops() {
		return this.blackholeDrops;
	}

	public int getOnOffDrops() {
		return this.onOffDrops;
	}

	public int getEnvCamouflageDrops() {
		return this.envCamouflageDrops;
	}

	public int getCrossRegionDrops() {
		return this.crossRegionDrops;
	}

	public int getFalseEventCount() {
		return this.falseEventCount;
	}

	@Override
	public MessageRouter replicate() {
		return new EETLERouter(this);
	}
}
