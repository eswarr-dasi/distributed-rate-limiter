package com.eswarr.ratelimiter;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Sliding window rate limiter using Redis sorted sets.
 * Each request is stored as a score (timestamp) in a sorted set.
 * Window slides to remove expired entries on each check.
 */
@Component
public class SlidingWindowRateLimiter implements RateLimiter {

      private final RedisTemplate<String, String> redisTemplate;

      public SlidingWindowRateLimiter(RedisTemplate<String, String> redisTemplate) {
                this.redisTemplate = redisTemplate;
            }

      @Override
      public boolean tryAcquire(String key, int limit, long windowSeconds) {
                long now = Instant.now().toEpochMilli();
                long windowStart = now - (windowSeconds * 1000);
                String redisKey = "rate:sliding:" + key;

                // Remove expired entries outside the window
                redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

                // Count current requests in window
                Long currentCount = redisTemplate.opsForZSet().size(redisKey);
                if (currentCount != null && currentCount >= limit) {
                              return false;
                          }

                // Add this request
                String member = UUID.randomUUID().toString();
                redisTemplate.opsForZSet().add(redisKey, member, now);
                redisTemplate.expire(redisKey, windowSeconds + 1, TimeUnit.SECONDS);

                return true;
            }

      @Override
      public long getRemaining(String key, int limit, long windowSeconds) {
                long now = Instant.now().toEpochMilli();
                long windowStart = now - (windowSeconds * 1000);
                String redisKey = "rate:sliding:" + key;

                redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
                Long count = redisTemplate.opsForZSet().size(redisKey);
                long used = count == null ? 0 : count;
                return Math.max(0, limit - used);
            }
  }
