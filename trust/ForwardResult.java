package trust;

/**
 * Settled forwarding cooperation evidence for T_i(j).
 */
public class ForwardResult {
	public enum State {
		SUCCESS,
		FAILURE,
		UNCERTAIN
	}

	public static final State SUCCESS = State.SUCCESS;
	public static final State FAILURE = State.FAILURE;
	public static final State UNCERTAIN = State.UNCERTAIN;

	private int evaluatorAddress;
	private int targetAddress;
	private State state;
	private double pout;

	public ForwardResult(int evaluatorAddress, int targetAddress,
			boolean success, double pout) {
		this(evaluatorAddress, targetAddress,
				success ? SUCCESS : FAILURE, pout);
	}

	public ForwardResult(int evaluatorAddress, int targetAddress,
			State state, double pout) {
		this.evaluatorAddress = evaluatorAddress;
		this.targetAddress = targetAddress;
		this.state = state;
		this.pout = pout;
	}

	public int getEvaluatorAddress() {
		return this.evaluatorAddress;
	}

	public void setEvaluatorAddress(int evaluatorAddress) {
		this.evaluatorAddress = evaluatorAddress;
	}

	public int getTargetAddress() {
		return this.targetAddress;
	}

	public void setTargetAddress(int targetAddress) {
		this.targetAddress = targetAddress;
	}

	public boolean isSuccess() {
		return this.state == SUCCESS;
	}

	public void setSuccess(boolean success) {
		this.state = success ? SUCCESS : FAILURE;
	}

	public State getState() {
		return this.state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public boolean isFailure() {
		return this.state == FAILURE;
	}

	public boolean isUncertain() {
		return this.state == UNCERTAIN;
	}

	public double getPout() {
		return this.pout;
	}

	public void setPout(double pout) {
		this.pout = pout;
	}
}
