package com.verve.assessment.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.verve.assessment.model.Promotion;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Streams the provided large csv file in batches and stores it in redis cache. Each batch is stored
 * in redis cache in parallel using non blocking virtual threads.
 * A batch is a hash map which is filled with promotions key and values. Once the map size reaches the batch
 * size, it flushes the map to redis.
 * Even if it takes on the lock on map, still provides significant improvements in performance over
 * PromotionsServiceSimple implementation.
 */
// @Service
@Slf4j
@RequiredArgsConstructor
public class PromotionServiceMultiThreaded implements PromotionsService {

  @Value("promotions.read.batch.size")
  private static int BATCH_SIZE;

  @Value("${promotions.file.path:promotions.csv}")
  private String filePath;

  private final RedisTemplate<String, Promotion> redisTemplate;

  public String processFile(MultipartFile file) {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    try {
      // Stream the file and process each record
      try (CSVReader reader =
          (file != null)
              ? new CSVReader(new InputStreamReader(file.getInputStream()))
              : new CSVReader(Files.newBufferedReader(Paths.get(filePath)))) {
        processRecordsInBatch(executor, reader);
      }
    } catch (Exception e) {
      log.error("Exception occurred while reading csv files: %s" + e.getStackTrace());
      throw new RuntimeException("Failed to process csv file", e);
    } finally {
      executor.shutdown();
    }
    log.info("All the records saved successfully");
    return PROMOTIONS_LOADED_SUCCESSFULLY;
  }

  private void processRecordsInBatch(ExecutorService executor, CSVReader reader)
      throws IOException, CsvValidationException {
    String[] record;
    Map<String, Promotion> batch = new HashMap<>();
    // Delete old data (optional based on your approach)
    redisTemplate.execute(
        (RedisCallback<Void>)
            connection -> {
              connection.flushDb();
              return null;
            });
    log.info("Reading the records from file with batch size of %d", BATCH_SIZE);
    while ((record = reader.readNext()) != null) {
      final String[] currentRecord = record;
      executor.submit(
          () -> {
            Promotion promotion = new Promotion();
            promotion.setId(currentRecord[0]);
            promotion.setPrice(Double.parseDouble(currentRecord[1]));
            promotion.setExpirationDate(currentRecord[2]);

            synchronized (batch) {
              batch.put(promotion.getId(), promotion);
              if (batch.size() >= BATCH_SIZE) {
                // Batch insert into Redis
                redisTemplate.opsForValue().multiSet(new HashMap<>(batch));
                batch.clear();
              }
            }
          });
    }
    // Insert any remaining records
    synchronized (batch) {
      if (!batch.isEmpty()) {
        redisTemplate.opsForValue().multiSet(batch);
      }
    }
  }

  public Optional<Promotion> getPromotion(String id) {
    return Optional.of(redisTemplate.opsForValue().get(id));
  }
}
