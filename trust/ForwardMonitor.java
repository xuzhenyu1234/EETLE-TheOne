package trust;

import java.util.ArrayList;
import java.util.List;

/**
 * Static monitor for forwarding cooperation evidence.
 *
 * A handoff from i to j only creates an observation. T_i(j) is updated later
 * when j forwards/delivers the message, drops it, or the observation expires.
 */
public class ForwardMonitor {
	private static List<ForwardObservation> observations =
			new ArrayList<ForwardObservation>();

	public static synchronized void recordDelegation(String messageId,
			int evaluator, int target, double pout, double now,
			double timeout) {
		if (messageId == null || evaluator == target) {
			return;
		}

		ForwardObservation obs = new ForwardObservation();
		obs.setMessageId(messageId);
		obs.setEvaluatorAddress(evaluator);
		obs.setTargetAddress(target);
		obs.setDelegatedTime(now);
		obs.setDeadlineTime(now + timeout);
		obs.setPout(clamp(pout));
		obs.setSettled(false);
		observations.add(obs);
	}

	public static synchronized List<ForwardResult> recordForwarded(
			String messageId, int target, double now) {
		return settleMatching(messageId, target, true);
	}

	public static synchronized List<ForwardResult> recordDropped(
			String messageId, int target, double now) {
		return settleMatching(messageId, target, false);
	}

	public static synchronized void recordForwardingOpportunity(
			String messageId, int target, int nextHop, double now) {
		if (messageId == null || target == nextHop) {
			return;
		}

		for (int i = observations.size() - 1; i >= 0; i--) {
			ForwardObservation obs = observations.get(i);
			if (obs.isSettled()) {
				observations.remove(i);
				continue;
			}
			if (obs.getTargetAddress() == target &&
					obs.getEvaluatorAddress() != nextHop &&
					messageId.equals(obs.getMessageId())) {
				obs.incrementForwardingOpportunityCount();
			}
		}
	}

	public static synchronized List<ForwardResult> expire(double now) {
		List<ForwardResult> results = new ArrayList<ForwardResult>();
		for (int i = observations.size() - 1; i >= 0; i--) {
			ForwardObservation obs = observations.get(i);
			if (obs.isSettled()) {
				observations.remove(i);
				continue;
			}
			if (now >= obs.getDeadlineTime()) {
				obs.setSettled(true);
				if (!obs.hadForwardingOpportunity()) {
					obs.setExpiredAsUncertain(true);
				}
				results.add(new ForwardResult(obs.getEvaluatorAddress(),
						obs.getTargetAddress(),
						obs.hadForwardingOpportunity() ?
						ForwardResult.FAILURE : ForwardResult.UNCERTAIN,
						obs.getPout()));
				observations.remove(i);
			}
		}
		return results;
	}

	public static synchronized void reset() {
		observations.clear();
	}

	private static List<ForwardResult> settleMatching(String messageId,
			int target, boolean success) {
		List<ForwardResult> results = new ArrayList<ForwardResult>();
		if (messageId == null) {
			return results;
		}

		for (int i = observations.size() - 1; i >= 0; i--) {
			ForwardObservation obs = observations.get(i);
			if (obs.isSettled()) {
				observations.remove(i);
				continue;
			}
			if (obs.getTargetAddress() == target &&
					messageId.equals(obs.getMessageId())) {
				obs.setSettled(true);
				results.add(new ForwardResult(obs.getEvaluatorAddress(),
						obs.getTargetAddress(), success, obs.getPout()));
				observations.remove(i);
			}
		}
		return results;
	}

	private static double clamp(double value) {
		if (value < 0.0) {
			return 0.0;
		}
		if (value > 1.0) {
			return 1.0;
		}
		return value;
	}
}
