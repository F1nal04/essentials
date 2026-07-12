package f1nal.essentials.tpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpaRequestsTest {

    private long now = 1_000_000L;
    private final TpaRequests requests = new TpaRequests(() -> now, () -> 60_000L, () -> 10_000L);

    private final UUID sender = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    @Test
    void createThenSecondBatchIsRejected() {
        assertTrue(requests.createRequest(sender, target, TpaRequests.Type.TPA));
        assertFalse(requests.createRequest(sender, other, TpaRequests.Type.TPA));
        assertEquals(-1, requests.createRequests(sender, List.of(target, other), TpaRequests.Type.TPA_HERE));
    }

    @Test
    void acceptReturnsAndRemovesTheRequest() {
        requests.createRequest(sender, target, TpaRequests.Type.TPA_HERE);
        Optional<TpaRequests.Request> req = requests.accept(target, sender);
        assertTrue(req.isPresent());
        assertEquals(sender, req.get().sender);
        assertEquals(target, req.get().target);
        assertEquals(TpaRequests.Type.TPA_HERE, req.get().type);
        assertTrue(requests.accept(target, sender).isEmpty());
        // sender's batch is gone, so they can send again
        assertTrue(requests.createRequest(sender, target, TpaRequests.Type.TPA));
    }

    @Test
    void acceptWithNullSenderPicksNewestRequest() {
        requests.createRequest(sender, target, TpaRequests.Type.TPA);
        now += 1_000;
        requests.createRequest(other, target, TpaRequests.Type.TPA);
        Optional<TpaRequests.Request> req = requests.accept(target, null);
        assertTrue(req.isPresent());
        assertEquals(other, req.get().sender);
    }

    @Test
    void acceptFromSpecificSenderPicksTheirs() {
        requests.createRequest(sender, target, TpaRequests.Type.TPA);
        now += 1_000;
        requests.createRequest(other, target, TpaRequests.Type.TPA);
        Optional<TpaRequests.Request> req = requests.accept(target, sender);
        assertTrue(req.isPresent());
        assertEquals(sender, req.get().sender);
    }

    @Test
    void denyRemovesTheRequest() {
        requests.createRequest(sender, target, TpaRequests.Type.TPA);
        assertTrue(requests.deny(target, sender).isPresent());
        assertTrue(requests.findIncomingFor(target, sender).isEmpty());
    }

    @Test
    void requestsExpireAfterTtl() {
        requests.createRequest(sender, target, TpaRequests.Type.TPA);
        now += 60_000;
        assertTrue(requests.accept(target, null).isEmpty());
        // expired batch no longer blocks the sender
        assertTrue(requests.createRequest(sender, target, TpaRequests.Type.TPA));
    }

    @Test
    void cancelClearsWholeBatchAndStartsCooldown() {
        assertEquals(2, requests.createRequests(sender, List.of(target, other), TpaRequests.Type.TPA_HERE));
        assertTrue(requests.cancel(sender));
        assertTrue(requests.findIncomingFor(target, null).isEmpty());
        assertTrue(requests.findIncomingFor(other, null).isEmpty());
        assertFalse(requests.createRequest(sender, target, TpaRequests.Type.TPA));
        assertEquals(Optional.of(10L), requests.secondsLeftOnCancelCooldown(sender));
        now += 10_000;
        assertTrue(requests.secondsLeftOnCancelCooldown(sender).isEmpty());
        assertTrue(requests.createRequest(sender, target, TpaRequests.Type.TPA));
    }

    @Test
    void cancelWithNothingPendingReturnsFalse() {
        assertFalse(requests.cancel(sender));
        assertTrue(requests.secondsLeftOnCancelCooldown(sender).isEmpty());
    }

    @Test
    void cooldownSecondsRoundUp() {
        requests.createRequest(sender, target, TpaRequests.Type.TPA);
        requests.cancel(sender);
        now += 9_999;
        assertEquals(Optional.of(1L), requests.secondsLeftOnCancelCooldown(sender));
    }

    @Test
    void createRequestsSkipsTheSenderAndCountsTheRest() {
        assertEquals(2, requests.createRequests(sender, List.of(sender, target, other), TpaRequests.Type.TPA_HERE));
        assertTrue(requests.findIncomingFor(sender, null).isEmpty());
        assertTrue(requests.findIncomingFor(target, null).isPresent());
        assertTrue(requests.findIncomingFor(other, null).isPresent());
    }
}
