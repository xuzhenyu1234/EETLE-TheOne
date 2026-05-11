/*
 * EETLE basic router for online local trust update.
 */
package routing;

import java.util.ArrayList;
import java.util.List;

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

	private double trustThreshold = 0.45;

	public EETLERouter(Settings s) {
		super(s);
		this.trustTable = new TrustTable();
		this.trustManager = new TrustManager();
		this.linkEnvironmentModel = new LinkEnvironmentModel();

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
		this.trustThreshold = r.trustThreshold;
	}

	@Override
	public void update() {
		super.update();
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

		return tryMessagesToConnections(messages, trustedConnections);
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

	@Override
	public MessageRouter replicate() {
		return new EETLERouter(this);
	}
}
