package f1nal.essentials.messaging;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagingStateTest {
    @Test
    void conversationTargetsAreBidirectionalAndUuidBased() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        MessagingState state = new MessagingState();
        state.recordConversation(first, second);
        assertEquals(second, state.replyTarget(first).orElseThrow());
        assertEquals(first, state.replyTarget(second).orElseThrow());
    }

    @Test
    void spyToggleIsStable() {
        UUID player = UUID.randomUUID();
        MessagingState state = new MessagingState();
        assertTrue(state.toggleSpy(player));
        assertTrue(state.isSpying(player));
        assertFalse(state.toggleSpy(player));
        assertFalse(state.isSpying(player));
    }
}
