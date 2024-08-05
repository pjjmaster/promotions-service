package com.verve.assessment.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.verve.assessment.exceptions.PromotionProcessingException;
import com.verve.assessment.model.Promotion;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Streams the provided large csv file in batches and stores it in redis cache. Each batch is stored
 * in redis cache in parallel using non blocking virtual threads. Here we are using the
 * ConcurrentLinkedQueue which is thread safe. This queue is filled with records and once the size
 * of the queue is equal to batch size, records are flushed to redis. This provides significant
 * performance improvement over PromotionsServiceSimple and it is on par with
 * PromotionServiceMultiThreaded which uses the non-concurrent hash map.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PromotionServiceEfficient implements PromotionsService {

  @Value("${promotions.read.batch.size:50000}")
  private int BATCH_SIZE;

  @Value("${promotions.file.path:promotions.csv}")
  private String filePath;

  private final RedisTemplate<String, Promotion> redisTemplate;

  public String processFile(MultipartFile file) {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    ConcurrentLinkedQueue<Promotion> queue = new ConcurrentLinkedQueue<>();
    AtomicInteger counter = new AtomicInteger(0);

    try {
      try (CSVReader reader =
          (file != null)
              ? new CSVReader(new InputStreamReader(file.getInputStream()))
              : new CSVReader(Files.newBufferedReader(Paths.get(filePath)))) {
        processRecordsInBatch(executor, queue, counter, reader);
      }

      // Insert any remaining records
      if (!queue.isEmpty()) {
        flushBatchToRedis(queue);
      }
    } catch (Exception e) {
      log.error("Exception occurred while reading csv files: %s", e);
      throw new RuntimeException("Failed to process csv file", e);
    } finally {
      executor.shutdown();
    }
    log.info("All the records saved successfully");
    return PROMOTIONS_LOADED_SUCCESSFULLY;
  }

  private void processRecordsInBatch(
      ExecutorService executor,
      ConcurrentLinkedQueue<Promotion> queue,
      AtomicInteger counter,
      CSVReader reader)
      throws IOException, CsvValidationException {
    String[] record;

    cleanupExistingPromotions();

    log.info(String.format("Reading the records from file with batch size of %d", BATCH_SIZE));

    while ((record = reader.readNext()) != null) {
      final String[] currentRecord = record;
      executor.submit(
          () -> {
            Promotion promotion = new Promotion();
            promotion.setId(currentRecord[0]);
            promotion.setPrice(Double.parseDouble(currentRecord[1]));
            promotion.setExpirationDate(currentRecord[2]);

            queue.add(promotion);
            int currentCount = counter.incrementAndGet();
            if (currentCount % BATCH_SIZE == 0) {
              flushBatchToRedis(queue);
            }
          });
    }
  }

  private void cleanupExistingPromotions() {
    redisTemplate.execute(
        (RedisCallback<Void>)
            connection -> {
              try {
                connection.flushDb();
                log.info("Redis database flushed successfully");
              } catch (DataAccessException e) {
                log.error("Failed to flush Redis database: {}", e.getMessage());
                throw new PromotionProcessingException("Failed to flush Redis database", e);
              }
              return null;
            });
  }

  private void flushBatchToRedis(ConcurrentLinkedQueue<Promotion> queue) {
    Map<String, Promotion> batch = new HashMap<>();
    while (!queue.isEmpty()) {
      Promotion promotion = queue.poll();
      if (promotion != null) {
        batch.put(promotion.getId(), promotion);
      }
    }
    if (!batch.isEmpty()) {
      try {
        redisTemplate.opsForValue().multiSet(batch);
        log.info("Batch successfully inserted into Redis");
      } catch (DataAccessException e) {
        log.error("Error saving batch to Redis: {}", e.getMessage());
        throw new PromotionProcessingException("Failed to save batch to Redis", e);
      }
    }
  }

  public Optional<Promotion> getPromotion(String id) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(id));
  }
}
