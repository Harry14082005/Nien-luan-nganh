package com.nhom8.nien_luan_nganh.listener;

import com.nhom8.nien_luan_nganh.dto.event.PollBroadcastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollEventListener {
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPollEvent(PollBroadcastEvent event) {
        log.info("[BROADCAST tới {}]", event.getDestination());
        messagingTemplate.convertAndSend(event.getDestination(), event.getPayload());
        log.info("[BROADCAST xong]");
    }
}
