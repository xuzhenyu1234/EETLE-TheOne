package trust;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure Java trust table owned by one node.
 * Stores directed trust edges using evaluatorId->targetId keys.
 */
public class TrustTable {
	private Map<String, TrustEdge> edges;

	public TrustTable() {
		this.edges = new HashMap<String, TrustEdge>();
	}

	private String makeKey(String evaluatorId, String targetId) {
		return evaluatorId + "->" + targetId;
	}

	public TrustEdge getOrCreateEdge(String evaluatorId, String targetId) {
		String key = makeKey(evaluatorId, targetId);
		TrustEdge edge = this.edges.get(key);
		if (edge == null) {
			edge = new TrustEdge(evaluatorId, targetId);
			this.edges.put(key, edge);
		}
		return edge;
	}

	public double getTrust(String evaluatorId, String targetId) {
		TrustEdge edge = this.edges.get(makeKey(evaluatorId, targetId));
		if (edge == null) {
			return 0.5;
		}
		return edge.scalarTrust;
	}

	public Map<String, TrustEdge> getAllEdges() {
		return this.edges;
	}

	public Collection<TrustEdge> getAllEdgesAsCollection() {
		return this.edges.values();
	}
}
