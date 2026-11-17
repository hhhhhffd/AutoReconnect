import com.autoreconnect.RetryPolicy;
import static com.autoreconnect.RetryPolicy.Cause.*;

public class RetryPolicyTest {
    private static int checks;
    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        ++checks;
    }

    public static void main(String[] args) {
        RetryPolicy policy = new RetryPolicy();
        expect(!policy.canSchedule(NETWORK), "no server / singleplayer");
        policy.begin();
        expect(!policy.canSchedule(NETWORK), "initial failed connection is manual only");
        policy.joined(0);
        policy.disconnected(500);
        expect(!policy.canSchedule(MANUAL_ONLY), "policy kicks/auth never scheduled");
        expect(policy.canSchedule(ORDINARY_KICK), "ordinary kick gets a first attempt");
        expect(policy.nextDelaySeconds(5) == 5, "configured first retry");
        expect(policy.startAutomatic(ORDINARY_KICK), "first retry started");
        expect(!policy.startAutomatic(ORDINARY_KICK), "repeated kick stops");
        expect(policy.nextDelaySeconds(5) == 10 && policy.startAutomatic(NETWORK), "second network attempt");
        expect(policy.nextDelaySeconds(5) == 20 && policy.startAutomatic(NETWORK), "third network attempt");
        expect(!policy.startAutomatic(NETWORK), "budget exhausted");

        policy.joined(5_000);
        policy.disconnected(10_000);
        expect(!policy.startAutomatic(NETWORK), "unstable rejoin does not reset retry budget");
        policy.joined(12_000);
        policy.disconnected(42_000);
        expect(policy.canSchedule(NETWORK) && policy.nextDelaySeconds(5) == 5, "stable rejoin gives new retry budget");
        policy.clear();
        expect(!policy.canSchedule(NETWORK) && !policy.canSchedule(ORDINARY_KICK), "manual disconnect/back clears target");

        expect(RetryPolicy.clampDelay(2) == 3 && RetryPolicy.clampDelay(-1) == 3, "minimum enforced");
        expect(RetryPolicy.clampDelay(9_999_999_999L) == Integer.MAX_VALUE, "maximum enforced");
        expect(policy.nextDelaySeconds(Integer.MAX_VALUE) == 2_147_483_647, "max initial value");
        policy.begin();
        policy.joined(0);
        policy.startAutomatic(NETWORK);
        policy.startAutomatic(NETWORK);
        expect(policy.nextDelaySeconds(Integer.MAX_VALUE) == Integer.MAX_VALUE, "max backoff saturates without overflow");
        expect(policy.nextDelaySeconds(3) == 12, "minimum base supports normal backoff");

        expect(RetryPolicy.classify("disconnect.endOfStream", "", "", false) == NETWORK, "localized end of stream");
        expect(RetryPolicy.classify("disconnect.genericReason", "", "Connection refused: getsockopt", false) == NETWORK, "connection refused");
        expect(RetryPolicy.classify("disconnect.timeout", "", "", false) == NETWORK, "timeout");
        expect(RetryPolicy.classify("", "Test kick", "", false) == ORDINARY_KICK, "ordinary kick");
        expect(RetryPolicy.classify("multiplayer.disconnect.not_whitelisted", "", "", false) == MANUAL_ONLY, "whitelist translation");
        expect(RetryPolicy.classify("", "You are banned", "Timeout", false) == MANUAL_ONLY, "ban takes priority");
        expect(RetryPolicy.classify("disconnect.loginFailedInfo.invalidSession", "", "", false) == MANUAL_ONLY, "invalid auth");
        expect(RetryPolicy.classify("disconnect.genericReason", "Packet decoder error", "", true) == MANUAL_ONLY, "no auto on unrelated packet failure");
        System.out.println("Passed " + checks + " reconnect policy checks.");
    }
}
