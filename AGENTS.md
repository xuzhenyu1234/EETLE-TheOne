# EETLE-The ONE Project Instructions

## 1. Project Background

This project modifies The ONE 1.6.0 simulator to implement an online trust management and robust Leader election mechanism for UAV swarm networks in post-disaster scenarios.

The proposed method is called:

EETLE: Environment-Event collaborative Trust management and robust Leader Election.

The target scenario is a post-disaster UAV swarm network. UAVs are used for emergency communication, disaster sensing, event reporting, message forwarding, and cooperative task execution.

The method must support online simulation:

1. The ONE simulates UAV movement, node contact, message forwarding, and network dynamics.
2. During simulation, each UAV maintains local trust edges toward other UAVs.
3. Message forwarding success or failure updates trust edges online.
4. Link outage probability is calculated from the communication environment.
5. Environmental uncertainty is derived from link outage probability.
6. EATR is used to distinguish environmental failure from malicious behavior.
7. Event consistency is used to detect false disaster event reports.
8. Global trust is fused using manually weighted linear attention.
9. Leader election is based on global trust, trust stability, communication quality, region constraint, and switching margin.

Do not implement the whole system at once.

Implementation must be done step by step.

---

## 2. Current Project Structure

This is The ONE 1.6.0 project.

Important existing directories:

- core/
- routing/
- report/
- movement/
- input/
- interfaces/
- applications/
- gui/
- util/
- data/
- example_settings/

Important existing files:

- compile.bat
- one.bat
- one.sh
- default_settings.txt
- README.txt

The ONE 1.6.0 project does not use a `src` directory.

Therefore, new Java packages should be created directly under the project root.

For example:

- trust/
- routing/
- report/

Do not create a new `src` directory.

---

## 3. New Code Organization

Add new code mainly in the following packages:

### trust package

Create a new directory:

trust/

Suggested classes:

- trust.TrustVector
- trust.TrustEdge
- trust.TrustTable
- trust.TrustManager
- trust.LinkEnvironmentModel
- trust.AttackModel
- trust.EventTrustManager
- trust.GlobalTrustManager
- trust.LeaderElection
- trust.NodeTrustState
- trust.RegionManager

### routing package

Add:

- routing.EETLERouter

### report package

Add:

- report.TrustReport
- report.LeaderReport
- report.AttackReport
- report.EventTrustReport

Try not to modify original The ONE core classes unless absolutely necessary.

Prefer adding new classes instead of changing existing framework code.

---

## 4. Coding Rules

Use Java.

Follow the coding style of The ONE.

Avoid advanced Java features that may not be compatible with old projects.

Do not use lambda expressions.

Do not use streams.

Do not use external libraries unless already included in the project.

Keep formulas clearly commented in code.

Every formula-related method must contain comments explaining the paper formula.

After each implementation step, run:

compile.bat

The project should remain runnable using:

one.bat

or batch mode:

one.bat -b 1 default_settings.txt

If compile.bat fails, fix the compile error before continuing.

---

## 5. Implementation Principle

This project should implement online trust update.

The simulation loop should form the following closed loop:

1. UAV nodes move in The ONE.
2. Nodes encounter each other.
3. The router checks trust before forwarding messages.
4. Message forwarding succeeds or fails.
5. The forwarding result updates the local trust edge.
6. Local trust affects future forwarding decisions.
7. Local trust is fused into global trust.
8. Global trust affects Leader election.
9. Leader state affects trust fusion and network coordination.

The first implementation target is only:

- Trust data structures.
- Local trust update.
- Link outage probability model.
- Basic EETLERouter.
- Trust update caused by forwarding success or failure.

Do not implement event consistency, global trust fusion, or Leader election in the first coding step.

---

## 6. Local Trust Edge Definition

For evaluator node i and target node j, maintain a directed trust edge:

T_ij(t) = <b_ij(t), e_ij(t), u_ij(t), d_ij(t)>

Where:

- b_ij(t): trust degree, meaning how trustworthy node j is according to node i.
- e_ij(t): environmental uncertainty, caused by poor link environment.
- u_ij(t): cognitive uncertainty, caused by insufficient observations.
- d_ij(t): distrust degree, caused by suspicious or malicious behavior.

The four components must satisfy:

b_ij(t) + e_ij(t) + u_ij(t) + d_ij(t) = 1

All components must be in [0, 1].

The local scalar trust value is calculated from this four-dimensional trust vector.

---

## 7. TrustVector Class

Create:

trust.TrustVector

Fields:

double b;
double e;
double u;
double d;

Meaning:

- b: trust degree
- e: environmental uncertainty
- u: cognitive uncertainty
- d: distrust degree

Default value:

b = 0.5
e = 0.0
u = 0.5
d = 0.0

Methods:

- normalize()
- copy()
- clamp()
- toString()

Normalization formula:

sum = b + e + u + d

If sum <= 0:

b = 0.5
e = 0.0
u = 0.5
d = 0.0

Otherwise:

b = b / sum
e = e / sum
u = u / sum
d = d / sum

After normalization:

b + e + u + d = 1

All values must be clamped to [0, 1].

---

## 8. TrustEdge Class

Create:

trust.TrustEdge

Each TrustEdge represents node i's trust evaluation toward node j.

Fields:

String evaluatorId;
String targetId;

int successCount;
int failCount;

TrustVector vector;

double scalarTrust;
double lastUpdateTime;

int highEnvTotalCount;
int highEnvFailureCount;

java.util.List<Double> trustHistory;

double lastPout;

Constructor:

TrustEdge(String evaluatorId, String targetId)

Initial values:

successCount = 0
failCount = 0
vector = new TrustVector()
scalarTrust = 0.5
lastUpdateTime = 0.0
highEnvTotalCount = 0
highEnvFailureCount = 0
trustHistory = new ArrayList<Double>()
lastPout = 0.0

Methods:

- getInteractionCount()
- addTrustHistory(double value)
- getLatestTrust()
- getEvaluatorId()
- getTargetId()

---

## 9. TrustTable Class

Create:

trust.TrustTable

Each node should maintain one TrustTable.

TrustTable_i = { targetNodeId -> TrustEdge(i, targetNodeId) }

Internal storage:

Map<String, TrustEdge> edges

Key format:

evaluatorId + "->" + targetId

Methods:

getOrCreateEdge(String evaluatorId, String targetId)

getTrust(String evaluatorId, String targetId)

getAllEdges()

getAllEdgesAsCollection()

Default trust for unknown node:

0.5

Unknown node default TrustVector:

b = 0.5
e = 0.0
u = 0.5
d = 0.0

TrustTable should be a pure Java class and should not depend on The ONE core classes.

---

## 10. Beta Behavior Evidence

When evaluator node i observes target node j, record:

s_ij(t): number of successful interactions
f_ij(t): number of failed interactions

Use Beta distribution smoothing.

alpha_ij = s_ij + 1

beta_ij = f_ij + 1

Initial behavior trust:

b0_ij = (s_ij + 1) / (s_ij + f_ij + 2)

Initial behavior distrust:

d0_ij = (f_ij + 1) / (s_ij + f_ij + 2)

This avoids extreme trust values during cold start.

---

## 11. Cognitive Uncertainty

Let:

n_ij = s_ij + f_ij

Cognitive uncertainty:

u0_ij = betaCog / (betaCog + n_ij)

Suggested default:

betaCog = 2.0

Interpretation:

- If node i has few observations about node j, u0 is high.
- If node i has many observations about node j, u0 decreases.

---

## 12. Link Environment Model

Create:

trust.LinkEnvironmentModel

The environmental uncertainty must not be manually assigned.

The environment should be modeled using link outage probability.

The paper formula is:

SINR_ij(t) = P_r,ij(t) / (N0 + I_ij(t))

Where:

- P_r,ij(t): received power from node j to node i.
- N0: noise power.
- I_ij(t): interference power.

Channel capacity:

C_ij(t) = B * log2(1 + SINR_ij(t))

A link outage occurs when:

C_ij(t) < R_req

Equivalent condition:

SINR_ij(t) < gamma_th

Where:

gamma_th = 2^(R_req / B) - 1

Therefore:

Pout_ij(t) = P(SINR_ij(t) < gamma_th)

Since The ONE does not provide a full physical-layer SINR model by default, implement an engineering approximation based on node distance, path loss, noise, and interference.

Received power:

P_r,ij(t) = P_t * pathGain_ij(t)

Path gain:

pathGain_ij(t) = 1 / (distance_ij(t)^pathLossExponent + epsilon)

SINR:

SINR_ij(t) = P_r,ij(t) / (noisePower + interferencePower)

Capacity:

C_ij(t) = bandwidth * log2(1 + SINR_ij(t))

Threshold:

gamma_th = 2^(requiredRate / bandwidth) - 1

Outage indicator:

if SINR_ij(t) < gamma_th:
    outage = 1
else:
    outage = 0

To obtain outage probability, maintain sliding statistics for each link:

Pout_ij(t) = outageCount_ij / sampleCount_ij

If sampleCount is too small, return a smoothed deterministic result.

Default parameters:

transmitPower = 1.0
noisePower = 1e-9
interferencePower = 1e-10
bandwidth = 1e6
requiredRate = 1e5
pathLossExponent = 2.0
epsilon = 1e-6
minSamples = 5

Important:

- Pout must be clamped to [0, 1].
- Do not directly set environmental uncertainty.
- Do not use fixed values such as 0.1, 0.3, 0.6, 0.9 as the final model.
- Fixed distance-based Pout values are allowed only for temporary debugging, not for final experiments.

Suggested methods:

double calculateReceivedPower(double distance)

double calculateSINR(double distance)

double calculateCapacity(double sinr)

double calculateGammaThreshold()

double calculateInstantOutage(double distance)

double updateAndGetPout(String nodeA, String nodeB, double distance)

double clamp(double value)

The key for link statistics should be direction-insensitive or direction-sensitive depending on implementation.

For local trust edge i -> j, direction-sensitive key is acceptable:

i + "->" + j

---

## 13. Environmental Uncertainty

Environmental uncertainty is derived from link outage probability:

e0_ij(t) = alphaEnv * Pout_ij(t)

Suggested default:

alphaEnv = 0.7

Constraint:

0 <= e0_ij(t) <= 1

Important:

TrustManager should only receive Pout and then compute e0.

TrustManager should not calculate SINR directly.

LinkEnvironmentModel calculates Pout.

TrustManager maps Pout to environmental uncertainty.

---

## 14. Initial Four-Dimensional Trust Construction

Before EATR, construct raw values:

b0 = Beta behavior trust

d0 = Beta behavior distrust

u0 = cognitive uncertainty

e0 = environmental uncertainty

Where:

b0 = (s + 1) / (s + f + 2)

d0 = (f + 1) / (s + f + 2)

u0 = betaCog / (betaCog + s + f)

e0 = alphaEnv * Pout

Normalize:

Z = b0 + d0 + u0 + e0

b = b0 / Z
d = d0 / Z
u = u0 / Z
e = e0 / Z

After normalization:

b + e + u + d = 1

---

## 15. Environment-Aware Trust Recovery: EATR

EATR is used to avoid treating all failed transmissions as malicious behavior.

The idea is:

A part of distrust may be caused by environmental outage.

Environmental part of distrust:

d_env = etaEnv * Pout_ij(t) * d

Behavioral part of distrust:

d_behavior = d - d_env

Update:

d' = d - d_env

e' = e + d_env

b' = b

u' = u

Then normalize.

Suggested default:

etaEnv = 0.6

Interpretation:

- If Pout is high, more failure mass is moved from distrust to environmental uncertainty.
- If Pout is low, failure remains mainly as distrust.

---

## 16. Hidden Environmental Attack Correction

Environmental hidden attack means:

A malicious node attacks mainly when the link environment is bad.

Condition:

Pout_ij(t) > envThreshold

Suggested default:

envThreshold = 0.6

For each trust edge, maintain:

highEnvTotalCount

highEnvFailureCount

When Pout > envThreshold:

highEnvTotalCount += 1

If the interaction failed:

highEnvFailureCount += 1

High-environment failure concentration:

H_ij = highEnvFailureCount / highEnvTotalCount

If:

H_ij > hiddenRiskThreshold

then transfer part of environmental uncertainty and cognitive uncertainty into distrust.

Suggested parameters:

hiddenRiskThreshold = 0.5

rhoHidden = 0.2

minHiddenSamples = 5

risk = H_ij - hiddenRiskThreshold

transferEnv = rhoHidden * risk * e

transferCog = rhoHidden * risk * u

Update:

e' = e - transferEnv

u' = u - transferCog

d' = d + transferEnv + transferCog

b' = b

Then normalize.

This mechanism prevents malicious nodes from always hiding attacks during bad environmental conditions.

---

## 17. Local Scalar Trust

The local scalar trust value is calculated from the four-dimensional trust vector.

Formula:

LT_ij(t) = b / (b + lambdaD * d + lambdaE * e + lambdaU * u)

Suggested default parameters:

lambdaD = 0.7

lambdaE = 0.3

lambdaU = 0.2

Constraint:

lambdaD >= lambdaE >= lambdaU >= 0

Interpretation:

- Distrust should penalize trust most strongly.
- Environmental uncertainty should penalize trust less than distrust.
- Cognitive uncertainty should have the weakest penalty.

If denominator <= 0:

LT = 0.5

The result must be clamped to [0, 1].

---

## 18. TrustManager Class

Create:

trust.TrustManager

Main method:

updateByForwardResult(
    TrustEdge edge,
    boolean success,
    double pout,
    double currentTime
)

Algorithm:

Step 1: Update success or failure count.

If success:

s = s + 1

Else:

f = f + 1

Step 2: Compute Beta behavior evidence.

b0 = (s + 1.0) / (s + f + 2.0)

d0 = (f + 1.0) / (s + f + 2.0)

Step 3: Compute cognitive uncertainty.

n = s + f

u0 = betaCog / (betaCog + n)

Step 4: Compute environmental uncertainty.

pout = clamp(pout, 0, 1)

e0 = alphaEnv * pout

Step 5: Normalize initial four-dimensional trust.

Z = b0 + d0 + u0 + e0

b = b0 / Z

d = d0 / Z

u = u0 / Z

e = e0 / Z

Step 6: Apply EATR.

d_env = etaEnv * pout * d

d = d - d_env

e = e + d_env

normalize()

Step 7: Apply hidden environmental attack correction.

If pout > envThreshold:

highEnvTotalCount += 1

If success == false:

highEnvFailureCount += 1

If highEnvTotalCount >= minHiddenSamples:

H = highEnvFailureCount / highEnvTotalCount

If H > hiddenRiskThreshold:

risk = H - hiddenRiskThreshold

transferEnv = rhoHidden * risk * e

transferCog = rhoHidden * risk * u

e = e - transferEnv

u = u - transferCog

d = d + transferEnv + transferCog

normalize()

Step 8: Compute scalar trust.

LT = b / (b + lambdaD * d + lambdaE * e + lambdaU * u)

If denominator <= 0:

LT = 0.5

Clamp LT to [0, 1].

Step 9: Update edge.

edge.vector = new TrustVector(b, e, u, d)

edge.scalarTrust = LT

edge.lastUpdateTime = currentTime

edge.lastPout = pout

edge.trustHistory.add(LT)

All formulas must be explained in code comments.

---

## 19. Time Decay Mechanism

Time decay may be implemented after the first version.

When trust is not updated for a long time, part of the trust mass should return to cognitive uncertainty.

Let:

deltaT = currentTime - lastUpdateTime

Environmental-aware decay factor:

decay = exp(-lambdaTime * deltaT * (1 + etaTimeEnv * Pout))

Suggested parameters:

lambdaTime = 0.001

etaTimeEnv = 0.5

If trust is dominant:

b' = decay * b

u' = u + (1 - decay) * b

If distrust is dominant:

d' = decay * d

u' = u + (1 - decay) * d

Then normalize.

This can be implemented after the first stable version.

---

## 20. EETLERouter Basic Behavior

Add:

routing.EETLERouter

The router should inherit from the most suitable existing router.

Before implementing, inspect:

- routing.ActiveRouter
- routing.EpidemicRouter
- routing.SprayAndWaitRouter
- routing.DirectDeliveryRouter
- routing.MessageRouter

Most likely EETLERouter should inherit from ActiveRouter, but inspect the project first.

Each EETLERouter instance should contain:

TrustTable trustTable;

TrustManager trustManager;

LinkEnvironmentModel linkEnvironmentModel;

double trustThreshold;

Suggested default:

trustThreshold = 0.45

Basic forwarding rule:

Before forwarding a message to another node, check:

trust = trustTable.getTrust(myNodeId, otherNodeId)

If:

trust >= trustThreshold

then allow forwarding.

Otherwise:

skip this neighbor.

When a transfer succeeds:

update trust edge as success.

When a transfer fails, aborts, or is interrupted:

update trust edge as failure.

Pout should be calculated from LinkEnvironmentModel using node distance.

Distance should be computed from host locations provided by The ONE.

Do not calculate environmental uncertainty directly in EETLERouter.

EETLERouter only calculates or requests Pout and passes it to TrustManager.

---

## 21. Where to Hook Trust Update in The ONE

Before implementing EETLERouter, inspect the project and identify the correct methods.

Possible hook points:

- update()
- changedConnection(Connection con)
- transferDone(Connection con)
- transferAborted(Connection con)
- messageTransferred(...)
- deleteMessage(...)
- startTransfer(...)
- exchangeDeliverableMessages()
- tryAllMessagesToAllConnections()

The final hook points must be selected based on actual The ONE 1.6.0 code.

Trust update policy:

1. Successful transfer:
   updateByForwardResult(edge, true, pout, currentTime)

2. Failed transfer:
   updateByForwardResult(edge, false, pout, currentTime)

3. Aborted transfer:
   updateByForwardResult(edge, false, pout, currentTime)

4. Malicious drop:
   updateByForwardResult(edge, false, pout, currentTime)

5. No contact:
   Do not update trust directly.

Do not punish a node simply because it is not currently connected.

---

## 22. AttackModel for Later Step

Do not implement attack model in the first coding step.

Later create:

trust.AttackModel

Attack types:

NORMAL

BLACKHOLE

ON_OFF

FALSE_MESSAGE

ENV_HIDDEN

CROSS_DOMAIN

Blackhole attack:

A malicious node drops packets with high probability.

pDrop = 0.9

On-off attack:

The malicious node alternates between normal and malicious phases.

Example:

period = 500 seconds

attack phase starts at 300 seconds

If:

currentTime % period >= 300

then attack.

Otherwise behave normally.

Environmental hidden attack:

If:

Pout > envThreshold

then attack with high probability.

Otherwise behave normally.

False message injection attack:

The malicious node reports false disaster event states.

Cross-domain attack:

A malicious node moves from region A to region B and tries to exploit cold start trust.

The attack model should be configurable from settings.

---

## 23. Event Model for Later Step

Disaster events include:

- road blockage
- dangerous area
- victim location
- material demand
- communication relay demand

Let:

E = {event_1, event_2, ..., event_K}

The real state of event k:

x_k(t) in {0, 1}

Node j's report for event k:

r_j,k(t) in {0, 1}

Where:

1 means event happens.

0 means event does not happen.

Normal nodes report the true state with high probability.

Malicious false-message nodes report the opposite state with high probability.

---

## 24. Event Consistency Trust Update for Later Step

Create later:

trust.EventTrustManager

Trust-weighted event occurrence probability:

P_k(t) = sum_{m in N_i} LT_im(t) * r_m,k(t) / sum_{m in N_i} LT_im(t)

Where:

N_i is the neighbor set of evaluator node i.

LT_im(t) is local scalar trust from node i to node m.

Double thresholds:

thetaHigh = 0.7

thetaLow = 0.3

If node j reports 1 and P_k >= thetaHigh:

reward node j.

If node j reports 0 and P_k <= thetaLow:

reward node j.

If node j reports 1 and P_k <= thetaLow:

punish node j.

If node j reports 0 and P_k >= thetaHigh:

punish node j.

Reward:

delta = gammaReward * u

b' = b + delta

u' = u - delta

Punishment:

delta = gammaPunish * u

d' = d + delta

u' = u - delta

Suggested parameters:

gammaReward = 0.1

gammaPunish = 0.2

Usually punishment should be stronger than reward.

If thetaLow < P_k < thetaHigh:

Do not strongly reward or punish.

The event state is uncertain, so cognitive uncertainty may be preserved or slightly increased.

After event update:

normalize()

update scalar trust.

---

## 25. Global Trust Fusion for Later Step

Create later:

trust.GlobalTrustManager

For target node j, the Leader collects local trust values from recommender nodes i:

LT_ij(t)

Global trust:

GT_j(t) = sum_i alpha_i,j(t) * LT_ij(t)

Where:

sum_i alpha_i,j(t) = 1

Do not use simple average in the full method.

Use manually weighted linear attention.

Attention score:

score_i,j(t) =
    omegaR * R_i(t)
  + omegaA * A_i(t)
  + omegaQ * Q_i(t)
  + omegaS * S_i,j(t)

Where:

R_i(t): recommender node's own global trust.

A_i(t): recommendation consistency.

Q_i(t): communication quality between recommender node i and Leader.

S_i,j(t): spatial correlation between recommender node i and target node j.

Suggested weights:

omegaR = 0.35

omegaA = 0.25

omegaQ = 0.20

omegaS = 0.20

Constraint:

omegaR + omegaA + omegaQ + omegaS = 1

Softmax attention:

alpha_i,j(t) = exp(tau * score_i,j(t)) / sum_m exp(tau * score_m,j(t))

Suggested:

tau = 2.0

Simple average should also be implemented as a baseline later:

GT_j(t) = average_i LT_ij(t)

---

## 26. Recommendation Consistency for Later Step

For recommender node i:

A_i(t) = 1 - average historical recommendation deviation

Formula:

A_i(t) = 1 - (1 / |H_i|) * sum_{j in H_i} |LT_ij(t) - GT_j(t-1)|

Clamp A_i(t) to [0, 1].

If no history:

A_i(t) = 0.5

Interpretation:

If recommender i often gives trust values close to the previous global trust, its recommendation consistency is high.

If recommender i often gives extreme or inconsistent trust values, its recommendation consistency is low.

---

## 27. Communication Quality for Global Fusion

Communication quality between recommender node i and Leader L:

Q_i(t) = 1 - Pout_i,L(t)

Clamp to [0, 1].

Pout_i,L(t) should be calculated by LinkEnvironmentModel.

---

## 28. Spatial Correlation for Global Fusion

Distance between recommender i and target j:

dist_i,j(t)

Spatial correlation:

S_i,j(t) = exp(-dist_i,j(t) / sigmaS)

Suggested:

sigmaS = 300.0

If node i and node j are in different regions, apply region discount:

S_i,j(t) = deltaRegion * S_i,j(t)

Suggested:

deltaRegion = 0.5

Interpretation:

A recommender closer to the target node has more reliable observation.

A recommender from a different region should have lower weight.

---

## 29. Cross-Domain Trust for Later Step

The disaster scenario contains two regions:

Region A

Region B

Most UAVs mainly interact within their own region.

Cross-domain attack:

A malicious node moves from Region A to Region B.

Region B lacks historical trust information about this node.

Therefore, the malicious node may exploit cold start trust.

Rules for cross-domain nodes:

1. Initial trust should not be higher than neutral trust.

GT_initial <= 0.5

2. Cross-domain nodes should not become Leader before enough valid interactions.

validInteractions >= minCrossDomainInteractions

Suggested:

minCrossDomainInteractions = 5

3. Recommendation and Leader election should apply region constraint.

RegionFactor = 1.0 for normal in-region nodes.

RegionFactor = 0.5 for cross-domain nodes with insufficient interaction history.

---

## 30. Leader Election for Later Step

Create later:

trust.LeaderElection

Leader candidate set:

C(t) = { node j | GT_j(t) >= thetaCandidate }

Suggested:

thetaCandidate = 0.5

Trust stability:

Use recent global trust history:

GT_j(t-W+1), ..., GT_j(t)

Stability:

ST_j(t) = 1 / (1 + variance(GT_j history))

Clamp to [0, 1].

Communication quality of candidate j:

CQ_j(t) = average_{m in neighbors(j)} (1 - Pout_j,m(t))

Leader base score:

Score_j(t) =
    phiT * GT_j(t)
  + phiS * ST_j(t)
  + phiQ * CQ_j(t)

Suggested:

phiT = 0.5

phiS = 0.3

phiQ = 0.2

Constraint:

phiT + phiS + phiQ = 1

Region constraint:

ScoreFinal_j(t) = RegionFactor_j(t) * Score_j(t)

Final Leader:

Leader(t) = argmax_j ScoreFinal_j(t)

---

## 31. Leader Switching Margin

Avoid frequent Leader switching.

Let:

currentLeader = L_old

bestCandidate = L_new

Switch only if:

ScoreFinal(L_new) > ScoreFinal(L_old) + switchMargin

Suggested:

switchMargin = 0.05

If the improvement is too small, keep the current Leader.

This prevents short-term trust fluctuation from causing frequent Leader changes.

---

## 32. Abnormal Leader Reelection

Trigger immediate Leader reelection if any condition holds:

GT_currentLeader < thetaLeaderTrust

ST_currentLeader < thetaLeaderStability

CQ_currentLeader < thetaLeaderComm

Suggested:

thetaLeaderTrust = 0.45

thetaLeaderStability = 0.5

thetaLeaderComm = 0.4

When abnormal reelection is triggered, write the reason to LeaderReport.

Possible reasons:

LOW_TRUST

LOW_STABILITY

LOW_COMM_QUALITY

BETTER_CANDIDATE

INITIAL_SELECTION

---

## 33. Reports

Reports are very important for paper figures.

Add later:

report.TrustReport

TrustReport should output CSV-like lines:

time, evaluator, target, b, e, u, d, scalarTrust, successCount, failCount, pout

Example:

100.0, u1, u2, 0.52, 0.10, 0.25, 0.13, 0.68, 5, 2, 0.35

Add later:

report.LeaderReport

LeaderReport should output:

time, leaderId, leaderGlobalTrust, stability, commQuality, score, reason

Example:

500.0, u3, 0.82, 0.91, 0.76, 0.83, INITIAL_SELECTION

Add later:

report.AttackReport

AttackReport should output:

time, nodeId, attackType, action, targetId, messageId, pout

Add later:

report.EventTrustReport

EventTrustReport should output:

time, eventId, nodeId, reportValue, consensusProbability, action, scalarTrust

---

## 34. Experiment Baselines

Implement later.

Baseline 1: BetaTrust

Only uses success and failure counts.

Trust = (s + 1) / (s + f + 2)

No environmental uncertainty.

No event consistency.

No hidden attack correction.

No linear attention.

No robust Leader election.

Baseline 2: EnvTrust

Uses Pout and EATR.

No event consistency.

No hidden environmental attack correction.

No linear attention.

Baseline 3: EventTrust

Uses event consistency.

No EATR.

Baseline 4: MeanFusion

Uses EETLE local trust.

But global trust fusion uses simple average.

Baseline 5: EETLE-Full

Uses full method:

- Four-dimensional trust
- SINR-based Pout
- Environmental uncertainty
- EATR
- Hidden environmental attack correction
- Event consistency
- Linear attention global trust fusion
- Robust Leader election

---

## 35. Expected Experiment Metrics

Later experiments should output:

### Malicious node detection metrics

- Detection rate
- False positive rate
- Accuracy
- F1-score
- Detection delay

### Trust evaluation metrics

- Average trust of normal nodes
- Average trust of malicious nodes
- Trust convergence time
- Trust fluctuation
- Normal node misjudgment rate

### Leader election metrics

- Malicious Leader election rate
- Leader average trust
- Leader switching count
- Leader stability duration
- Cross-domain malicious Leader election probability

### Network performance metrics

- Delivery ratio
- Average delay
- Overhead ratio
- Drop ratio
- Event accuracy

---

## 36. Suggested Experiment Settings

Simulation time:

10000 seconds

Node number:

50

Regions:

2

Region A:

road blockage detection and communication relay

Region B:

dangerous area recognition and material demand reporting

Malicious node ratio:

10%, 20%, 30%, 40%

Communication range:

Use The ONE interface settings.

UAV speed:

5 m/s to 20 m/s

Message generation interval:

25 s to 35 s

Event types:

- road blockage
- dangerous area
- material demand

Normal event report correctness:

0.9

False-message attack probability:

0.8

Blackhole drop probability:

0.9

On-off attack:

normal phase 300 s

attack phase 200 s

Environmental hidden attack trigger:

Pout > 0.6

Cross-domain migration time:

t = 5000 s

Leader election period:

100 s or 200 s

---

## 37. Strict Implementation Order

Follow this order strictly.

### Step 1: Project analysis only

Read project structure.

Analyze routing package.

Analyze report package.

Do not modify code.

### Step 2: Trust data structures

Add:

- trust.TrustVector
- trust.TrustEdge
- trust.TrustTable
- trust.TrustManager
- trust.LinkEnvironmentModel

Do not modify routing, report, or core.

Compile.

### Step 3: Basic EETLERouter

Add:

- routing.EETLERouter

Support:

- trust threshold forwarding
- Pout calculation through LinkEnvironmentModel
- local trust update after forwarding success or failure

Compile.

### Step 4: Basic TrustReport

Add:

- report.TrustReport

Output local trust edges.

Compile.

### Step 5: Attack model

Add:

- trust.AttackModel

Support:

- NORMAL
- BLACKHOLE
- ON_OFF
- ENV_HIDDEN

Compile.

### Step 6: Event consistency

Add:

- trust.EventTrustManager

Support false message injection attack detection.

Compile.

### Step 7: Global trust fusion

Add:

- trust.GlobalTrustManager

Support linear attention fusion.

Compile.

### Step 8: Leader election

Add:

- trust.LeaderElection
- trust.NodeTrustState

Support robust Leader election.

Compile.

### Step 9: Cross-domain attack

Add region modeling and cross-domain constraints.

Compile.

### Step 10: Full reports and experiment settings

Add:

- TrustReport
- LeaderReport
- AttackReport
- EventTrustReport
- experiment settings files

Compile and run experiments.

---

## 38. Current Task Rule

When starting a new Codex task, always read this AGENTS.md first.

If the user asks for the first task, only analyze the project.

Do not write code unless the user explicitly says:

- 开始写代码
- 实现 trust 包
- 实现 EETLERouter
- 实现攻击模型
- 实现 Leader 选举

If the user asks to implement something, make the smallest possible change.

Always compile after code changes.

If compilation fails, fix compilation errors before adding new features.

Never implement multiple major modules in one step.

---

## 39. First Codex Task

The first Codex task should be:

Read AGENTS.md and analyze the current The ONE project structure.

Do not modify any code.

Focus on:

1. Which router class EETLERouter should inherit.
2. Which methods can be used for message forwarding success and failure.
3. Which methods can be used for connection up and down.
4. How Report classes work.
5. How to compile and run this project.
6. What is the minimum modification plan.

Output the analysis only.

Do not create files.

Do not edit files.

Do not implement code.

---

## 40. Important Reminder

The environmental uncertainty must follow the paper logic:

SINR -> channel capacity -> outage condition -> Pout -> environmental uncertainty.

Do not directly assign environmental uncertainty manually.

The final model must compute:

e0 = alphaEnv * Pout

Where Pout is calculated from link outage probability.

The simplified distance-based fixed values are not the final paper model.

They are only allowed for temporary debugging.

The final experiment should use LinkEnvironmentModel to compute SINR and Pout.