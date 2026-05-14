/*
 * EETLE basic router for online local trust update.
 */
package routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import trust.AttackModel;
import trust.AttackProfile;
import trust.AttackType;
import trust.EventReport;
import trust.EventTrustManager;
import trust.EventTrustResult;
import trust.ForwardMonitor;
import trust.ForwardResult;
import trust.GlobalTrustManager;
import trust.LeaderElection;
import trust.LinkEnvironmentModel;
import trust.LocalTrustRecord;
import trust.MaliciousDetectionPolicy;
import trust.RegionManager;
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
	public static final String ENV_CAMOUFLAGE_THRESHOLD_SETTING =
			"envCamouflageThreshold";
	public static final String ENV_CAMOUFLAGE_OUTAGE_BOOST_SETTING =
			"envCamouflageOutageBoost";
	public static final String BASE_DECAY_RATE_SETTING = "baseDecayRate";
	public static final String ENV_DECAY_SENSITIVITY_SETTING =
			"envDecaySensitivity";
	public static final String FORWARD_OBSERVATION_TIMEOUT_SETTING =
			"forwardObservationTimeout";
	public static final String EVENT_CONSENSUS_INTERVAL_SETTING =
			"eventConsensusInterval";
	public static final String EVENT_POSITIVE_THRESHOLD_SETTING =
			"eventPositiveThreshold";
	public static final String EVENT_NEGATIVE_THRESHOLD_SETTING =
			"eventNegativeThreshold";
	public static final String EVENT_REWARD_SETTING = "eventReward";
	public static final String EVENT_PENALTY_SETTING = "eventPenalty";
	public static final String EVENT_EVALUATOR_TRUST_THRESHOLD_SETTING =
			"eventEvaluatorTrustThreshold";
	public static final String EVENT_CONSENSUS_RADIUS_SETTING =
			"eventConsensusRadius";
	public static final String MIN_EVENT_CONSENSUS_REPORTS_SETTING =
			"minEventConsensusReports";
	public static final String MAX_EVENT_EVALUATORS_PER_REPORT_SETTING =
			"maxEventEvaluatorsPerReport";
	public static final String SPATIAL_SCALE_SETTING = "spatialScale";
	public static final String SPATIAL_REGION_DISCOUNT_SETTING =
			"spatialRegionDiscount";
	public static final String REGION_SPLIT_X_SETTING = "regionSplitX";
	public static final String CROSS_REGION_WARMUP_SETTING =
			"crossRegionWarmup";
	public static final String REGION_PENALTY_FACTOR_SETTING =
			"regionPenaltyFactor";
	public static final String MIN_CROSS_REGION_INTERACTIONS_SETTING =
			"minCrossRegionInteractionsForLeader";
	public static final String FORCE_CROSS_REGION_ATTACK_SETTING =
			"forceCrossRegionAttack";
	public static final String CROSS_REGION_SWITCH_TIME_SETTING =
			"crossRegionSwitchTime";
	public static final String LEADER_ELECTION_INTERVAL_SETTING =
			"leaderElectionInterval";
	public static final String CANDIDATE_TRUST_THRESHOLD_SETTING =
			"candidateTrustThreshold";
	public static final String MIN_TRUST_STABILITY_SETTING =
			"minTrustStability";
	public static final String MIN_COMMUNICATION_QUALITY_SETTING =
			"minCommunicationQuality";
	public static final String SWITCHING_MARGIN_SETTING = "switchingMargin";
	public static final String ABNORMAL_TRUST_THRESHOLD_SETTING =
			"abnormalTrustThreshold";
	public static final String ABNORMAL_STABILITY_THRESHOLD_SETTING =
			"abnormalStabilityThreshold";
	public static final String ABNORMAL_COMMUNICATION_THRESHOLD_SETTING =
			"abnormalCommunicationThreshold";
	public static final String LEADER_WEIGHT_TRUST_SETTING =
			"leaderWeightTrust";
	public static final String LEADER_WEIGHT_STABILITY_SETTING =
			"leaderWeightStability";
	public static final String LEADER_WEIGHT_COMMUNICATION_SETTING =
			"leaderWeightCommunication";
	public static final String TRUST_HISTORY_WINDOW_SIZE_SETTING =
			"trustHistoryWindowSize";
	public static final String ATTACK_ENABLED_SETTING = "attackEnabled";
	public static final String ATTACK_SEED_SETTING = "attackSeed";
	public static final String ATTACK_RATIO_SETTING = "attackRatio";
	public static final String BLACKHOLE_RATIO_SETTING = "blackholeRatio";
	public static final String ON_OFF_RATIO_SETTING = "onOffRatio";
	public static final String FALSE_EVENT_RATIO_SETTING = "falseEventRatio";
	public static final String ENV_CAMOUFLAGE_RATIO_SETTING =
			"envCamouflageRatio";
	public static final String CROSS_REGION_RATIO_SETTING = "crossRegionRatio";
	public static final String ATTACK_ASSIGNMENT_MODE_SETTING =
			"attackAssignmentMode";
	public static final String ENABLE_EATR_SETTING = "enableEATR";
	public static final String ENABLE_EVENT_TRUST_SETTING = "enableEventTrust";
	public static final String ENABLE_LINEAR_ATTENTION_SETTING =
			"enableLinearAttention";
	public static final String ENABLE_REGION_CONSTRAINT_SETTING =
			"enableRegionConstraint";
	public static final String ENABLE_TRUST_STABILITY_SETTING =
			"enableTrustStability";
	public static final String ENABLE_LEADER_SWITCH_MARGIN_SETTING =
			"enableLeaderSwitchMargin";
	public static final String SCALAR_WEIGHT_C_SETTING = "scalarWeightC";
	public static final String SCALAR_WEIGHT_E_SETTING = "scalarWeightE";
	public static final String SCALAR_WEIGHT_D_SETTING = "scalarWeightD";
	public static final String LINK_TRANSMIT_POWER_SETTING =
			"linkTransmitPower";
	public static final String LINK_NOISE_POWER_SETTING = "linkNoisePower";
	public static final String LINK_INTERFERENCE_POWER_SETTING =
			"linkInterferencePower";
	public static final String LINK_BANDWIDTH_SETTING = "linkBandwidth";
	public static final String LINK_REQUIRED_RATE_SETTING = "linkRequiredRate";
	public static final String LINK_PATH_LOSS_EXPONENT_SETTING =
			"linkPathLossExponent";
	public static final String LINK_EPSILON_SETTING = "linkEpsilon";
	public static final String LINK_MIN_SAMPLES_SETTING = "linkMinSamples";
	private static final String EETLE_NS = "EETLERouter";
	private static final int DEFAULT_LEADER_ADDRESS = 0;
	private static final double LOCAL_TRUST_UPLOAD_INTERVAL = 120.0;
	private static final double DEFAULT_FORWARD_OBSERVATION_TIMEOUT = 600.0;
	private static final double DEFAULT_LEADER_ELECTION_INTERVAL = 120.0;
	private static GlobalTrustManager globalTrustManager =
			new GlobalTrustManager();
	private static EventTrustManager eventTrustManager =
			new EventTrustManager();
	private static RegionManager regionManager = new RegionManager();
	private static LeaderElection leaderElection = new LeaderElection();
	private static AttackModel attackModel = new AttackModel();
	private static MaliciousDetectionPolicy maliciousDetectionPolicy =
			new MaliciousDetectionPolicy();
	private static Map<Integer, EETLERouter> routerByAddress =
			new HashMap<Integer, EETLERouter>();
	private static List<String> attackRecords = new ArrayList<String>();
	private static Set<String> attackRecordKeys = new HashSet<String>();
	private static double nextLeaderElectionTime = 0.0;

	private TrustTable trustTable;
	private TrustManager trustManager;
	private LinkEnvironmentModel linkEnvironmentModel;
	private AttackProfile attackProfile;
	private Random attackRng;

	private double trustThreshold = 0.45;
	private double envCamouflageThreshold = 0.6;
	private double envCamouflageOutageBoost = 0.0;
	private double baseDecayRate = 0.001;
	private double envDecaySensitivity = 0.5;
	private double forwardObservationTimeout =
			DEFAULT_FORWARD_OBSERVATION_TIMEOUT;
	private String homeRegion;
	private String currentRegion;
	private double regionEnterTime;
	private double nextEventReportTime;
	private double eventReportInterval = 120.0;
	private double eventConsensusInterval = 120.0;
	private double eventReward = 0.05;
	private double eventPenalty = 0.15;
	private double eventEvaluatorTrustThreshold = 0.55;
	private double eventConsensusRadius = 300.0;
	private int minEventConsensusReports = 3;
	private int maxEventEvaluatorsPerReport = 10;
	private double spatialScale = 500.0;
	private double spatialRegionDiscount = 0.5;
	private boolean forceCrossRegionAttack = false;
	private double crossRegionSwitchTime = 900.0;
	private int eventTypeCount = 4;
	private double nextLocalTrustUploadTime;
	private double nextGlobalFusionTime;
	private double nextEventConsensusTime;
	private double leaderElectionInterval = DEFAULT_LEADER_ELECTION_INTERVAL;
	private double leaderWeightTrust = 0.50;
	private double leaderWeightStability = 0.30;
	private double leaderWeightCommunication = 0.20;
	private boolean enableEATR = true;
	private boolean enableEventTrust = true;
	private boolean enableLinearAttention = true;
	private boolean enableRegionConstraint = true;
	private boolean enableTrustStability = true;
	private boolean enableLeaderSwitchMargin = true;
	private double scalarWeightC = 0.5;
	private double scalarWeightE = 0.3;
	private double scalarWeightD = 1.5;

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
		this.nextEventReportTime = 0.0;
		this.nextLocalTrustUploadTime = 0.0;
		this.nextGlobalFusionTime = 0.0;
		this.nextEventConsensusTime = 0.0;
		clearAttackCounters();

		Settings eetleSettings = new Settings(EETLE_NS);
		if (eetleSettings.contains(TRUST_THRESHOLD_SETTING)) {
			this.trustThreshold =
					eetleSettings.getDouble(TRUST_THRESHOLD_SETTING);
		}
		readScalarTrustSettings(eetleSettings);
		readAblationSettings(eetleSettings);
		readLinkEnvironmentSettings(eetleSettings);
		attackModel.configure(eetleSettings);
		maliciousDetectionPolicy.configure(eetleSettings);
		if (eetleSettings.contains(ENV_CAMOUFLAGE_THRESHOLD_SETTING)) {
			this.envCamouflageThreshold = eetleSettings.getDouble(
					ENV_CAMOUFLAGE_THRESHOLD_SETTING);
		}
		if (eetleSettings.contains(ENV_CAMOUFLAGE_OUTAGE_BOOST_SETTING)) {
			this.envCamouflageOutageBoost = eetleSettings.getDouble(
					ENV_CAMOUFLAGE_OUTAGE_BOOST_SETTING);
		}
		if (eetleSettings.contains(BASE_DECAY_RATE_SETTING)) {
			this.baseDecayRate =
					eetleSettings.getDouble(BASE_DECAY_RATE_SETTING);
			this.trustManager.setBaseDecayRate(this.baseDecayRate);
		}
		if (eetleSettings.contains(ENV_DECAY_SENSITIVITY_SETTING)) {
			this.envDecaySensitivity = eetleSettings.getDouble(
					ENV_DECAY_SENSITIVITY_SETTING);
			this.trustManager.setEnvDecaySensitivity(
					this.envDecaySensitivity);
		}
		if (eetleSettings.contains(FORWARD_OBSERVATION_TIMEOUT_SETTING)) {
			this.forwardObservationTimeout = eetleSettings.getDouble(
					FORWARD_OBSERVATION_TIMEOUT_SETTING);
		}
		if (eetleSettings.contains(EVENT_CONSENSUS_INTERVAL_SETTING)) {
			this.eventConsensusInterval = eetleSettings.getDouble(
					EVENT_CONSENSUS_INTERVAL_SETTING);
			eventTrustManager.setEventConsensusInterval(
					this.eventConsensusInterval);
		}
		if (eetleSettings.contains(EVENT_POSITIVE_THRESHOLD_SETTING)) {
			eventTrustManager.setEventPositiveThreshold(
					eetleSettings.getDouble(EVENT_POSITIVE_THRESHOLD_SETTING));
		}
		if (eetleSettings.contains(EVENT_NEGATIVE_THRESHOLD_SETTING)) {
			eventTrustManager.setEventNegativeThreshold(
					eetleSettings.getDouble(EVENT_NEGATIVE_THRESHOLD_SETTING));
		}
		if (eetleSettings.contains(EVENT_REWARD_SETTING)) {
			this.eventReward = eetleSettings.getDouble(EVENT_REWARD_SETTING);
			eventTrustManager.setEventReward(this.eventReward);
			this.trustManager.setEventReward(this.eventReward);
		}
		if (eetleSettings.contains(EVENT_PENALTY_SETTING)) {
			this.eventPenalty = eetleSettings.getDouble(EVENT_PENALTY_SETTING);
			eventTrustManager.setEventPenalty(this.eventPenalty);
			this.trustManager.setEventPenalty(this.eventPenalty);
		}
		if (eetleSettings.contains(
				EVENT_EVALUATOR_TRUST_THRESHOLD_SETTING)) {
			this.eventEvaluatorTrustThreshold = eetleSettings.getDouble(
					EVENT_EVALUATOR_TRUST_THRESHOLD_SETTING);
			eventTrustManager.setEventEvaluatorTrustThreshold(
					this.eventEvaluatorTrustThreshold);
		}
		if (eetleSettings.contains(EVENT_CONSENSUS_RADIUS_SETTING)) {
			this.eventConsensusRadius = eetleSettings.getDouble(
					EVENT_CONSENSUS_RADIUS_SETTING);
		}
		eventTrustManager.setEventConsensusRadius(
				this.eventConsensusRadius);
		if (eetleSettings.contains(MIN_EVENT_CONSENSUS_REPORTS_SETTING)) {
			this.minEventConsensusReports = eetleSettings.getInt(
					MIN_EVENT_CONSENSUS_REPORTS_SETTING);
		}
		eventTrustManager.setMinEventConsensusReports(
				this.minEventConsensusReports);
		if (eetleSettings.contains(
				MAX_EVENT_EVALUATORS_PER_REPORT_SETTING)) {
			this.maxEventEvaluatorsPerReport = eetleSettings.getInt(
					MAX_EVENT_EVALUATORS_PER_REPORT_SETTING);
			eventTrustManager.setMaxEventEvaluatorsPerReport(
					this.maxEventEvaluatorsPerReport);
		}
		if (eetleSettings.contains(SPATIAL_SCALE_SETTING)) {
			this.spatialScale = eetleSettings.getDouble(SPATIAL_SCALE_SETTING);
		}
		if (eetleSettings.contains(SPATIAL_REGION_DISCOUNT_SETTING)) {
			this.spatialRegionDiscount = eetleSettings.getDouble(
					SPATIAL_REGION_DISCOUNT_SETTING);
		}
		if (eetleSettings.contains(REGION_SPLIT_X_SETTING)) {
			regionManager.setRegionSplitX(
					eetleSettings.getDouble(REGION_SPLIT_X_SETTING));
		}
		if (eetleSettings.contains(CROSS_REGION_WARMUP_SETTING)) {
			regionManager.setCrossRegionWarmup(
					eetleSettings.getDouble(CROSS_REGION_WARMUP_SETTING));
		}
		if (eetleSettings.contains(REGION_PENALTY_FACTOR_SETTING)) {
			regionManager.setRegionPenaltyFactor(
					eetleSettings.getDouble(REGION_PENALTY_FACTOR_SETTING));
		}
		if (eetleSettings.contains(
				MIN_CROSS_REGION_INTERACTIONS_SETTING)) {
			regionManager.setMinCrossRegionInteractionsForLeader(
					eetleSettings.getDouble(
							MIN_CROSS_REGION_INTERACTIONS_SETTING));
		}
		if (eetleSettings.contains(FORCE_CROSS_REGION_ATTACK_SETTING)) {
			this.forceCrossRegionAttack = eetleSettings.getBoolean(
					FORCE_CROSS_REGION_ATTACK_SETTING);
		}
		if (eetleSettings.contains(CROSS_REGION_SWITCH_TIME_SETTING)) {
			this.crossRegionSwitchTime = eetleSettings.getDouble(
					CROSS_REGION_SWITCH_TIME_SETTING);
		}
		readLeaderElectionSettings(eetleSettings);
	}

	private void readLinkEnvironmentSettings(Settings settings) {
		if (settings.contains(LINK_TRANSMIT_POWER_SETTING)) {
			this.linkEnvironmentModel.setTransmitPower(
					settings.getDouble(LINK_TRANSMIT_POWER_SETTING));
		}
		if (settings.contains(LINK_NOISE_POWER_SETTING)) {
			this.linkEnvironmentModel.setNoisePower(
					settings.getDouble(LINK_NOISE_POWER_SETTING));
		}
		if (settings.contains(LINK_INTERFERENCE_POWER_SETTING)) {
			this.linkEnvironmentModel.setInterferencePower(
					settings.getDouble(LINK_INTERFERENCE_POWER_SETTING));
		}
		if (settings.contains(LINK_BANDWIDTH_SETTING)) {
			this.linkEnvironmentModel.setBandwidth(
					settings.getDouble(LINK_BANDWIDTH_SETTING));
		}
		if (settings.contains(LINK_REQUIRED_RATE_SETTING)) {
			this.linkEnvironmentModel.setRequiredRate(
					settings.getDouble(LINK_REQUIRED_RATE_SETTING));
		}
		if (settings.contains(LINK_PATH_LOSS_EXPONENT_SETTING)) {
			this.linkEnvironmentModel.setPathLossExponent(
					settings.getDouble(LINK_PATH_LOSS_EXPONENT_SETTING));
		}
		if (settings.contains(LINK_EPSILON_SETTING)) {
			this.linkEnvironmentModel.setEpsilon(
					settings.getDouble(LINK_EPSILON_SETTING));
		}
		if (settings.contains(LINK_MIN_SAMPLES_SETTING)) {
			this.linkEnvironmentModel.setMinSamples(
					settings.getInt(LINK_MIN_SAMPLES_SETTING));
		}
	}

	protected EETLERouter(EETLERouter r) {
		super(r);
		this.trustTable = new TrustTable();
		this.trustManager = new TrustManager();
		this.linkEnvironmentModel = new LinkEnvironmentModel();
		readLinkEnvironmentSettings(new Settings(EETLE_NS));
		this.attackProfile = new AttackProfile(r.attackProfile);
		this.attackRng = new Random(1);
		this.trustThreshold = r.trustThreshold;
		this.envCamouflageThreshold = r.envCamouflageThreshold;
		this.envCamouflageOutageBoost = r.envCamouflageOutageBoost;
		this.baseDecayRate = r.baseDecayRate;
		this.envDecaySensitivity = r.envDecaySensitivity;
		this.forwardObservationTimeout = r.forwardObservationTimeout;
		this.eventConsensusInterval = r.eventConsensusInterval;
		this.eventReward = r.eventReward;
		this.eventPenalty = r.eventPenalty;
		this.eventEvaluatorTrustThreshold = r.eventEvaluatorTrustThreshold;
		this.eventConsensusRadius = r.eventConsensusRadius;
		this.minEventConsensusReports = r.minEventConsensusReports;
		this.maxEventEvaluatorsPerReport = r.maxEventEvaluatorsPerReport;
		this.spatialScale = r.spatialScale;
		this.spatialRegionDiscount = r.spatialRegionDiscount;
		this.forceCrossRegionAttack = r.forceCrossRegionAttack;
		this.crossRegionSwitchTime = r.crossRegionSwitchTime;
		this.trustManager.setBaseDecayRate(this.baseDecayRate);
		this.trustManager.setEnvDecaySensitivity(this.envDecaySensitivity);
		this.trustManager.setEventReward(this.eventReward);
		this.trustManager.setEventPenalty(this.eventPenalty);
		this.enableEATR = r.enableEATR;
		this.enableEventTrust = r.enableEventTrust;
		this.enableLinearAttention = r.enableLinearAttention;
		this.enableRegionConstraint = r.enableRegionConstraint;
		this.enableTrustStability = r.enableTrustStability;
		this.enableLeaderSwitchMargin = r.enableLeaderSwitchMargin;
		this.scalarWeightC = r.scalarWeightC;
		this.scalarWeightE = r.scalarWeightE;
		this.scalarWeightD = r.scalarWeightD;
		this.trustManager.setScalarTrustWeights(this.scalarWeightC,
				this.scalarWeightE, this.scalarWeightD);
		this.trustManager.setEnableEATR(this.enableEATR);
		globalTrustManager.setEnableLinearAttention(
				this.enableLinearAttention);
		leaderElection.setEnableRegionConstraint(
				this.enableRegionConstraint);
		leaderElection.setEnableTrustStability(this.enableTrustStability);
		leaderElection.setEnableLeaderSwitchMargin(
				this.enableLeaderSwitchMargin);
		eventTrustManager.setEventEvaluatorTrustThreshold(
				this.eventEvaluatorTrustThreshold);
		eventTrustManager.setEventConsensusInterval(
				this.eventConsensusInterval);
		eventTrustManager.setEventConsensusRadius(
				this.eventConsensusRadius);
		eventTrustManager.setMinEventConsensusReports(
				this.minEventConsensusReports);
		eventTrustManager.setMaxEventEvaluatorsPerReport(
				this.maxEventEvaluatorsPerReport);
		this.homeRegion = null;
		this.currentRegion = null;
		this.regionEnterTime = 0.0;
		this.nextEventReportTime = 0.0;
		this.nextLocalTrustUploadTime = 0.0;
		this.nextGlobalFusionTime = 0.0;
		this.nextEventConsensusTime = 0.0;
		this.leaderElectionInterval = r.leaderElectionInterval;
		this.leaderWeightTrust = r.leaderWeightTrust;
		this.leaderWeightStability = r.leaderWeightStability;
		this.leaderWeightCommunication = r.leaderWeightCommunication;
		clearAttackCounters();
	}

	@Override
	public void init(DTNHost host, List<core.MessageListener> mListeners) {
		super.init(host, mListeners);
		routerByAddress.put(new Integer(host.getAddress()), this);
		this.attackProfile = new AttackProfile(
				attackModel.getAttackProfile(host.getAddress()));
		if (this.attackProfile.getType() == AttackType.ENV_CAMOUFLAGE) {
			this.attackProfile.setEnvAttackThreshold(
					this.envCamouflageThreshold);
		}
		this.attackRng = new Random(host.getAddress() + 1);
		this.nextEventReportTime = SimClock.getTime() +
				this.attackRng.nextDouble() * this.eventReportInterval;
		this.nextLocalTrustUploadTime = SimClock.getTime() +
				this.attackRng.nextDouble() * LOCAL_TRUST_UPLOAD_INTERVAL;
		this.nextGlobalFusionTime = SimClock.getTime() +
				this.attackRng.nextDouble() * LOCAL_TRUST_UPLOAD_INTERVAL;
		this.nextEventConsensusTime = SimClock.getTime() +
				this.attackRng.nextDouble() * this.eventConsensusInterval;
		updateRegionState();
	}

	@Override
	public void update() {
		super.update();
		applyForwardResults(ForwardMonitor.expire(SimClock.getTime()));
		updateRegionState();
		maybeGenerateEventReport();
		maybeEvaluateEventConsensus();
		maybeUploadLocalTrust();
		maybeFuseGlobalTrust();
		maybeRunLeaderElection();
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
				if (shouldDropForwarding(m, pout)) {
					continue;
				}
				if (startTransfer(m, con) == RCV_OK) {
					DTNHost nextHop = con.getOtherNode(getHost());
					ForwardMonitor.recordForwardingOpportunity(m.getId(),
							getHost().getAddress(), nextHop.getAddress(),
							SimClock.getTime());
					return con;
				}
			}
		}

		return null;
	}

	private List<Connection> getTrustedConnections() {
		List<Connection> trustedConnections = new ArrayList<Connection>();
		String myId = String.valueOf(getHost().getAddress());

		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			String otherId = String.valueOf(other.getAddress());
			double trust = this.trustTable.getTrust(myId, otherId);

			if (trust >= this.trustThreshold) {
				trustedConnections.add(con);
			}
		}

		return trustedConnections;
	}

	private double calculatePout(DTNHost a, DTNHost b) {
		return estimateLinkOutageProbability(a, b);
	}

	private double estimateLinkOutageProbability(DTNHost a, DTNHost b) {
		double distance = a.getLocation().distance(b.getLocation());
		double outage = this.linkEnvironmentModel.updateAndGetPout(
				a.toString(), b.toString(), distance);
		if (isEnvCamouflageEndpoint(a, b)) {
			outage = outage + this.envCamouflageOutageBoost;
		}
		return clamp(outage);
	}

	private boolean isEnvCamouflageEndpoint(DTNHost a, DTNHost b) {
		return getAttackTypeForAddress(a.getAddress()) ==
				AttackType.ENV_CAMOUFLAGE ||
				getAttackTypeForAddress(b.getAddress()) ==
				AttackType.ENV_CAMOUFLAGE;
	}

	private double clamp(double value) {
		if (value < 0) {
			return 0.0;
		}
		if (value > 1) {
			return 1.0;
		}
		return value;
	}

	private boolean shouldDropForwarding(Message message, double pout) {
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
			this.trustManager.recordDebugFailure(getHost().getAddress(), pout);
			applyForwardResults(ForwardMonitor.recordDropped(message.getId(),
					getHost().getAddress(), SimClock.getTime()));
			if (type == AttackType.BLACKHOLE) {
				this.blackholeDrops++;
				logAttackAction("BLACKHOLE_DROP", getHost().getAddress(),
						getAttackTargetAddress(message), message.getId(),
						pout);
			}
			else if (type == AttackType.ON_OFF) {
				this.onOffDrops++;
				logAttackAction("ON_OFF_DROP", getHost().getAddress(),
						getAttackTargetAddress(message), message.getId(),
						pout);
			}
			else if (type == AttackType.ENV_CAMOUFLAGE) {
				this.envCamouflageDrops++;
				logAttackAction("ENV_CAMOUFLAGE_DROP",
						getHost().getAddress(), getAttackTargetAddress(message),
						message.getId(), pout);
			}
			else if (type == AttackType.CROSS_REGION) {
				this.crossRegionDrops++;
				logAttackAction("CROSS_REGION_DROP",
						getHost().getAddress(), getAttackTargetAddress(message),
						message.getId(), pout);
			}
		}

		return drop;
	}

	private void maybeGenerateEventReport() {
		double now = SimClock.getTime();
		if (now < this.nextEventReportTime) {
			return;
		}

		int trueState = ((int)(now / 300.0)) % 2;
		reportEventState(trueState);

		this.nextEventReportTime = now + this.eventReportInterval;
	}

	public int reportEventState(int realState) {
		return reportEventState(realState, getEventReportOutage());
	}

	private double getEventReportOutage() {
		List<Connection> connections = getConnections();
		if (connections.size() == 0) {
			return 0.0;
		}

		double sum = 0.0;
		for (int i = 0; i < connections.size(); i++) {
			DTNHost other = connections.get(i).getOtherNode(getHost());
			sum += calculatePout(getHost(), other);
		}
		return sum / connections.size();
	}

	/**
	 * Event reporting hook for later event modules. A false-event attacker
	 * flips a binary event state: real 1 is reported as 0, and real 0 is
	 * reported as 1. Environmental camouflage can inject false events only
	 * when the current Pout satisfies the environmental trigger.
	 */
	public int reportEventState(int realState, double linkOutageProbability) {
		double now = SimClock.getTime();
		int reportedState = realState;
		if (shouldInjectFalseEvent(linkOutageProbability)) {
			this.falseEventsInjected++;
			this.falseEventCount++;
			this.attackAttempts++;
			reportedState = realState == 0 ? 1 : 0;
		}

		EventReport report = new EventReport();
		report.setReporterAddress(getHost().getAddress());
		report.setEventId((int)(now / this.eventConsensusInterval));
		report.setEventType(((int)(now / 300.0)) % this.eventTypeCount);
		report.setReportedState(reportedState);
		report.setRealState(realState);
		report.setTimestamp(now);
		report.setConfidence(1.0);
		report.setX(getHost().getLocation().getX());
		report.setY(getHost().getLocation().getY());
		report.setRegion(regionManager.getCurrentRegion(getHost().getAddress()));
		eventTrustManager.collectEventReport(report);
		if (reportedState != realState) {
			String action = this.attackProfile.getType() ==
					AttackType.ENV_CAMOUFLAGE ?
					"ENV_CAMOUFLAGE_FALSE_EVENT" : "FALSE_EVENT_INJECT";
			logAttackAction(action, report.getEventId(), report.getEventId(),
					"event-" + report.getEventId(), linkOutageProbability);
		}

		return reportedState;
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
		String action = this.attackProfile.getType() ==
				AttackType.ENV_CAMOUFLAGE ?
				"ENV_CAMOUFLAGE_FALSE_EVENT" : "FALSE_EVENT_INJECT";
		logAttackAction(action, realEventType, realEventType, "eventType-" +
				realEventType, linkOutageProbability);

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
		return attackModel.getAttackType(address);
	}

	private void updateRegionState() {
		if (getHost() == null || getHost().getLocation() == null) {
			return;
		}

		if (this.attackProfile != null &&
				this.attackProfile.getType() == AttackType.CROSS_REGION &&
				this.forceCrossRegionAttack) {
			regionManager.updateForcedCrossRegion(getHost(), SimClock.getTime(),
					this.crossRegionSwitchTime);
			return;
		}

		regionManager.updateRegion(getHost(), SimClock.getTime());
	}

	private boolean isCrossRegionAttackActive() {
		if (this.attackProfile.getType() != AttackType.CROSS_REGION) {
			return false;
		}
		int address = getHost().getAddress();
		if (!regionManager.isCrossRegion(address)) {
			return false;
		}
		return !regionManager.isInCrossRegionWarmup(address,
				SimClock.getTime());
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

	private void readScalarTrustSettings(Settings eetleSettings) {
		if (eetleSettings.contains(SCALAR_WEIGHT_C_SETTING)) {
			this.scalarWeightC = eetleSettings.getDouble(
					SCALAR_WEIGHT_C_SETTING);
		}
		if (eetleSettings.contains(SCALAR_WEIGHT_E_SETTING)) {
			this.scalarWeightE = eetleSettings.getDouble(
					SCALAR_WEIGHT_E_SETTING);
		}
		if (eetleSettings.contains(SCALAR_WEIGHT_D_SETTING)) {
			this.scalarWeightD = eetleSettings.getDouble(
					SCALAR_WEIGHT_D_SETTING);
		}
		this.trustManager.setScalarTrustWeights(this.scalarWeightC,
				this.scalarWeightE, this.scalarWeightD);
	}

	private void readAblationSettings(Settings eetleSettings) {
		if (eetleSettings.contains(ENABLE_EATR_SETTING)) {
			this.enableEATR = eetleSettings.getBoolean(ENABLE_EATR_SETTING);
		}
		if (eetleSettings.contains(ENABLE_EVENT_TRUST_SETTING)) {
			this.enableEventTrust = eetleSettings.getBoolean(
					ENABLE_EVENT_TRUST_SETTING);
		}
		if (eetleSettings.contains(ENABLE_LINEAR_ATTENTION_SETTING)) {
			this.enableLinearAttention = eetleSettings.getBoolean(
					ENABLE_LINEAR_ATTENTION_SETTING);
		}
		if (eetleSettings.contains(ENABLE_REGION_CONSTRAINT_SETTING)) {
			this.enableRegionConstraint = eetleSettings.getBoolean(
					ENABLE_REGION_CONSTRAINT_SETTING);
		}
		if (eetleSettings.contains(ENABLE_TRUST_STABILITY_SETTING)) {
			this.enableTrustStability = eetleSettings.getBoolean(
					ENABLE_TRUST_STABILITY_SETTING);
		}
		if (eetleSettings.contains(ENABLE_LEADER_SWITCH_MARGIN_SETTING)) {
			this.enableLeaderSwitchMargin = eetleSettings.getBoolean(
					ENABLE_LEADER_SWITCH_MARGIN_SETTING);
		}

		this.trustManager.setEnableEATR(this.enableEATR);
		globalTrustManager.setEnableLinearAttention(
				this.enableLinearAttention);
		leaderElection.setEnableRegionConstraint(
				this.enableRegionConstraint);
		leaderElection.setEnableTrustStability(this.enableTrustStability);
		leaderElection.setEnableLeaderSwitchMargin(
				this.enableLeaderSwitchMargin);
	}

	private void readLeaderElectionSettings(Settings eetleSettings) {
		if (eetleSettings.contains(LEADER_ELECTION_INTERVAL_SETTING)) {
			this.leaderElectionInterval = eetleSettings.getDouble(
					LEADER_ELECTION_INTERVAL_SETTING);
		}
		if (eetleSettings.contains(CANDIDATE_TRUST_THRESHOLD_SETTING)) {
			leaderElection.setCandidateTrustThreshold(
					eetleSettings.getDouble(
							CANDIDATE_TRUST_THRESHOLD_SETTING));
		}
		if (eetleSettings.contains(MIN_TRUST_STABILITY_SETTING)) {
			leaderElection.setMinTrustStability(
					eetleSettings.getDouble(MIN_TRUST_STABILITY_SETTING));
		}
		if (eetleSettings.contains(MIN_COMMUNICATION_QUALITY_SETTING)) {
			leaderElection.setMinCommunicationQuality(
					eetleSettings.getDouble(
							MIN_COMMUNICATION_QUALITY_SETTING));
		}
		if (eetleSettings.contains(SWITCHING_MARGIN_SETTING)) {
			leaderElection.setSwitchingMargin(
					eetleSettings.getDouble(SWITCHING_MARGIN_SETTING));
		}
		if (eetleSettings.contains(ABNORMAL_TRUST_THRESHOLD_SETTING)) {
			leaderElection.setAbnormalTrustThreshold(
					eetleSettings.getDouble(
							ABNORMAL_TRUST_THRESHOLD_SETTING));
		}
		if (eetleSettings.contains(ABNORMAL_STABILITY_THRESHOLD_SETTING)) {
			leaderElection.setAbnormalStabilityThreshold(
					eetleSettings.getDouble(
							ABNORMAL_STABILITY_THRESHOLD_SETTING));
		}
		if (eetleSettings.contains(
				ABNORMAL_COMMUNICATION_THRESHOLD_SETTING)) {
			leaderElection.setAbnormalCommunicationThreshold(
					eetleSettings.getDouble(
							ABNORMAL_COMMUNICATION_THRESHOLD_SETTING));
		}
		if (eetleSettings.contains(LEADER_WEIGHT_TRUST_SETTING)) {
			this.leaderWeightTrust = eetleSettings.getDouble(
					LEADER_WEIGHT_TRUST_SETTING);
		}
		if (eetleSettings.contains(LEADER_WEIGHT_STABILITY_SETTING)) {
			this.leaderWeightStability = eetleSettings.getDouble(
					LEADER_WEIGHT_STABILITY_SETTING);
		}
		if (eetleSettings.contains(LEADER_WEIGHT_COMMUNICATION_SETTING)) {
			this.leaderWeightCommunication = eetleSettings.getDouble(
					LEADER_WEIGHT_COMMUNICATION_SETTING);
		}
		leaderElection.setWeights(this.leaderWeightTrust,
				this.leaderWeightStability,
				this.leaderWeightCommunication);
		if (eetleSettings.contains(TRUST_HISTORY_WINDOW_SIZE_SETTING)) {
			leaderElection.setTrustHistoryWindowSize(
					eetleSettings.getInt(TRUST_HISTORY_WINDOW_SIZE_SETTING));
		}
	}

	/**
	 * Periodically uploads local trust edges (evaluator == self address) to the
	 * shared GlobalTrustManager so the Leader can fuse global trust.
	 * Only edges where this node is the evaluator are uploaded.
	 */
	private void maybeUploadLocalTrust() {
		double now = SimClock.getTime();
		if (now < this.nextLocalTrustUploadTime) {
			return;
		}

		this.nextLocalTrustUploadTime = now + LOCAL_TRUST_UPLOAD_INTERVAL;
		uploadLocalTrustToLeader(now);
	}

	private void uploadLocalTrustToLeader(double now) {
		String myId = String.valueOf(getHost().getAddress());
		for (TrustEdge edge : this.trustTable.getAllEdgesAsCollection()) {
			if (!edge.getEvaluatorId().equals(myId)) {
				continue;
			}
			if (edge.getEvaluatorId().equals(edge.getTargetId())) {
				continue;
			}

			int targetAddress;
			try {
				targetAddress = Integer.parseInt(edge.getTargetId());
			}
			catch (NumberFormatException e) {
				continue;
			}

			LocalTrustRecord record = new LocalTrustRecord(
					getHost().getAddress(), targetAddress, edge.vector,
					edge.scalarTrust, edge.lastPout, now);
			record.setCommunicationQuality(
					calculateLeaderCommunicationQuality(edge));
			record.setSpatialCorrelation(
					calculateSpatialCorrelation(targetAddress));
			record.setAttentionWeight(0.0);

			globalTrustManager.collectLocalTrust(record);
		}
		globalTrustManager.updateGlobalTrust(DEFAULT_LEADER_ADDRESS, now);
	}

	private double calculateLeaderCommunicationQuality(TrustEdge edge) {
		int leaderAddress = getCurrentLeaderAddress();
		EETLERouter leaderRouter = routerByAddress.get(
				new Integer(leaderAddress));
		if (leaderRouter == null || leaderRouter.getHost() == null ||
				getHost() == null) {
			return clamp(1.0 - edge.lastPout);
		}

		double leaderPout = leaderRouter.calculatePout(
				leaderRouter.getHost(), getHost());
		return clamp(1.0 - leaderPout);
	}

	private double calculateSpatialCorrelation(int targetAddress) {
		EETLERouter targetRouter = routerByAddress.get(
				new Integer(targetAddress));
		if (targetRouter == null || targetRouter.getHost() == null ||
				getHost() == null) {
			return 1.0;
		}

		double distance = getHost().getLocation().distance(
				targetRouter.getHost().getLocation());
		double scale = this.spatialScale <= 0.0 ? 500.0 : this.spatialScale;
		double spatial = Math.exp(-distance / scale);

		int evaluatorRegion = regionManager.getCurrentRegion(
				getHost().getAddress());
		int targetRegion = regionManager.getCurrentRegion(targetAddress);
		if (evaluatorRegion >= 0 && targetRegion >= 0 &&
				evaluatorRegion != targetRegion) {
			spatial *= this.spatialRegionDiscount;
		}
		return clamp(spatial);
	}

	/**
	 * If this node is the Leader, periodically fuse collected local trust
	 * records into global trust entries.
	 */
	private void maybeFuseGlobalTrust() {
		if (getHost().getAddress() != getCurrentLeaderAddress()) {
			return;
		}

		double now = SimClock.getTime();
		if (now < this.nextGlobalFusionTime) {
			return;
		}

		this.nextGlobalFusionTime = now + LOCAL_TRUST_UPLOAD_INTERVAL;
		globalTrustManager.updateGlobalTrust(getCurrentLeaderAddress(), now);
	}

	private void maybeEvaluateEventConsensus() {
		if (getHost().getAddress() != getCurrentLeaderAddress()) {
			return;
		}

		double now = SimClock.getTime();
		if (now < this.nextEventConsensusTime) {
			return;
		}

		this.nextEventConsensusTime = now + this.eventConsensusInterval;
		List<EventTrustResult> results = eventTrustManager.evaluateReports(
				getCurrentLeaderAddress(), now, globalTrustManager);
		for (int i = 0; i < results.size(); i++) {
			EventTrustResult result = results.get(i);
			if (!this.enableEventTrust) {
				continue;
			}
			EETLERouter evaluatorRouter = routerByAddress.get(
					new Integer(result.getEvaluatorAddress()));
			if (evaluatorRouter == null) {
				continue;
			}
			evaluatorRouter.applyObservedEventResult(result.getTargetAddress(),
					result.isAgreement(), result.isUncertain(), 0.0);
		}
		uploadLocalTrustToLeader(now);
	}

	private void maybeRunLeaderElection() {
		double now = SimClock.getTime();
		if (now < nextLeaderElectionTime) {
			return;
		}
		nextLeaderElectionTime = now + this.leaderElectionInterval;
		leaderElection.updateElection(globalTrustManager, regionManager,
				maliciousDetectionPolicy, getAllTrustEdgesSnapshot(), now);
	}

	/**
	 * Returns the shared static GlobalTrustManager instance.
	 */
	public static GlobalTrustManager getGlobalTrustManager() {
		return globalTrustManager;
	}

	public static EventTrustManager getEventTrustManager() {
		return eventTrustManager;
	}

	public static RegionManager getRegionManager() {
		return regionManager;
	}

	public static LeaderElection getLeaderElection() {
		return leaderElection;
	}

	public static MaliciousDetectionPolicy getMaliciousDetectionPolicy() {
		return maliciousDetectionPolicy;
	}

	public static List<TrustEdge> getAllTrustEdgesSnapshot() {
		List<TrustEdge> edges = new ArrayList<TrustEdge>();
		for (EETLERouter router : routerByAddress.values()) {
			if (router == null || router.trustTable == null) {
				continue;
			}
			edges.addAll(router.trustTable.getAllEdgesAsCollection());
		}
		return edges;
	}

	public static int getCurrentLeaderAddress() {
		return leaderElection.getCurrentLeaderAddress();
	}

	public void applyObservedForwardResult(int targetAddress, boolean success,
			double pout) {
		regionManager.recordCrossRegionInteraction(targetAddress);
		this.trustManager.updateByForwardResult(this.trustTable,
				getHost().getAddress(), targetAddress, success, pout,
				SimClock.getTime());
	}

	public void applyObservedUncertainForwardResult(int targetAddress,
			double pout) {
		regionManager.recordCrossRegionInteraction(targetAddress);
		this.trustManager.updateByUncertainForwardResult(this.trustTable,
				getHost().getAddress(), targetAddress, pout,
				SimClock.getTime());
	}

	public void applyObservedEventResult(int targetAddress, boolean consistent,
			boolean uncertain, double pout) {
		if (targetAddress == getHost().getAddress()) {
			return;
		}
		regionManager.recordCrossRegionInteraction(targetAddress);
		this.trustManager.updateByEventResult(this.trustTable,
				getHost().getAddress(), targetAddress, consistent, uncertain,
				pout, SimClock.getTime());
	}

	private static void applyForwardResults(List<ForwardResult> results) {
		for (int i = 0; i < results.size(); i++) {
			ForwardResult result = results.get(i);
			EETLERouter evaluatorRouter = routerByAddress.get(
					new Integer(result.getEvaluatorAddress()));
			if (evaluatorRouter == null) {
				continue;
			}
			if (result.isUncertain()) {
				evaluatorRouter.applyObservedUncertainForwardResult(
						result.getTargetAddress(), result.getPout());
			}
			else {
				evaluatorRouter.applyObservedForwardResult(
						result.getTargetAddress(), result.isSuccess(),
						result.getPout());
			}
		}
	}

	@Override
	protected void transferDone(Connection con) {
		Message message = con.getMessage();
		DTNHost from = getHost();
		DTNHost to = con.getOtherNode(from);
		double now = SimClock.getTime();

		if (message != null) {
			applyForwardResults(ForwardMonitor.recordForwarded(
					message.getId(), from.getAddress(), now));

			double pout = calculatePout(from, to);
			ForwardMonitor.recordDelegation(message.getId(),
					from.getAddress(), to.getAddress(), pout, now,
					this.forwardObservationTimeout);

			if (message.getTo() == to) {
				applyForwardResults(ForwardMonitor.recordForwarded(
						message.getId(), to.getAddress(), now));
			}
		}
		super.transferDone(con);
	}

	@Override
	protected void transferAborted(Connection con) {
		Message message = con.getMessage();
		DTNHost from = getHost();
		DTNHost to = con.getOtherNode(from);

		if (message != null && from != null && to != null &&
				from.getAddress() != to.getAddress()) {
			double pout = calculatePout(from, to);
			if (pout > this.envCamouflageThreshold) {
				applyObservedUncertainForwardResult(to.getAddress(), pout);
			}
			else {
				applyObservedForwardResult(to.getAddress(), false, pout);
			}
		}
		super.transferAborted(con);
	}

	private int getAttackTargetAddress(Message message) {
		if (message == null || message.getTo() == null) {
			return -1;
		}
		return message.getTo().getAddress();
	}

	private void logAttackAction(String action, int keyAddress,
			int targetAddress,
			String messageId, double pout) {
		String key = getHost().getAddress() + "|" + action + "|" +
				keyAddress + "|" + messageId;
		synchronized (attackRecords) {
			if (attackRecordKeys.contains(key)) {
				return;
			}
			attackRecordKeys.add(key);
		}

		AttackType type = this.attackProfile.getType();
		String line = formatAttackDouble(SimClock.getTime()) + "," +
				getHost().getAddress() + "," +
				type.toString() + "," +
				action + "," +
				targetAddress + "," +
				messageId + "," +
				formatAttackDouble(clamp(pout)) + "," +
				getHomeRegion() + "," +
				getCurrentRegion() + "," +
				isCrossRegion();
		synchronized (attackRecords) {
			attackRecords.add(line);
		}
	}

	private static String formatAttackDouble(double value) {
		return String.format(java.util.Locale.US, "%.4f", value);
	}

	public static List<String> getAttackRecords() {
		synchronized (attackRecords) {
			return new ArrayList<String>(attackRecords);
		}
	}

	public static List<String> drainAttackRecords() {
		synchronized (attackRecords) {
			List<String> drained = attackRecords;
			attackRecords = new ArrayList<String>();
			return drained;
		}
	}

	public TrustTable getTrustTable() {
		return this.trustTable;
	}

	public double getLocalTrust(String targetId) {
		return this.trustTable.getTrust(
				String.valueOf(getHost().getAddress()), targetId);
	}

	public TrustEdge getTrustEdge(String targetId) {
		return this.trustTable.getOrCreateEdge(
				String.valueOf(getHost().getAddress()), targetId);
	}

	public AttackType getAttackType() {
		return this.attackProfile.getType();
	}

	public boolean isCrossRegionAttackNode() {
		return this.attackProfile.getType() == AttackType.CROSS_REGION;
	}

	public boolean isInForeignRegion() {
		return isCrossRegion();
	}

	public String getHomeRegion() {
		return formatRegion(regionManager.getHomeRegion(getHost().getAddress()));
	}

	public String getCurrentRegion() {
		return formatRegion(regionManager.getCurrentRegion(
				getHost().getAddress()));
	}

	public double getRegionEnterTime() {
		return SimClock.getTime() - getRegionResidenceTime();
	}

	public int getHomeRegionId() {
		return regionManager.getHomeRegion(getHost().getAddress());
	}

	public int getCurrentRegionId() {
		return regionManager.getCurrentRegion(getHost().getAddress());
	}

	public boolean isCrossRegion() {
		return regionManager.isCrossRegion(getHost().getAddress());
	}

	public double getRegionResidenceTime() {
		return regionManager.getRegionResidenceTime(getHost().getAddress(),
				SimClock.getTime());
	}

	public double getRegionConstraintFactor(int leaderRegion) {
		return regionManager.getRegionConstraintFactor(getHost().getAddress(),
				leaderRegion, SimClock.getTime());
	}

	public boolean canBeLeaderCandidate(int leaderRegion) {
		return regionManager.canBeLeaderCandidate(getHost().getAddress(),
				leaderRegion, SimClock.getTime());
	}

	private String formatRegion(int region) {
		if (region == 0) {
			return "REGION_A";
		}
		if (region == 1) {
			return "REGION_B";
		}
		return "UNKNOWN";
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

	public double getDebugSelfScalarTrust() {
		return this.trustManager.getScalarTrust(getHost().getAddress());
	}

	public trust.TrustVector getDebugSelfTrustVector() {
		return this.trustManager.getTrustVector(getHost().getAddress());
	}

	@Override
	public MessageRouter replicate() {
		return new EETLERouter(this);
	}
}
