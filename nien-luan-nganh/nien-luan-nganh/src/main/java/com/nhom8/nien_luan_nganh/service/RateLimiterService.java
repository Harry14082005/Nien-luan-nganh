package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn tần suất gọi AI (Mặc định 5 lần / phút / user)
 */
@Service
public class RateLimiterService {

    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final long ONE_MINUTE_MS = 60_000L;

    private final Map<UUID, Deque<Long>> userRequestTimestamps = new ConcurrentHashMap<>();

    public void checkRateLimit(UUID userId) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = userRequestTimestamps.computeIfAbsent(userId, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Xóa các mốc thời gian ngoài 1 phút
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > ONE_MINUTE_MS) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
                throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
            }

            timestamps.addLast(now);
        }
    }
}
