package com.verve.assessment.service;

import com.opencsv.CSVReader;
import com.verve.assessment.model.Promotion;
import com.verve.assessment.repository.PromotionRepository;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Class to read the csv file records and store in redis cache with single thread serially. It uses
 * simple implementation of spring data repository.
 */
//@Service
@Slf4j
@RequiredArgsConstructor
public class PromotionsServiceSimple implements PromotionsService{

  @Value("${promotions.file.path:promotions.csv}")
  private String filePath;

  private final PromotionRepository promotionRepository;

  public String processFile(MultipartFile file) {
    try {
      // Clear existing data
      promotionRepository.deleteAll();

      // Read and parse CSV file
      try (CSVReader reader =
          (file != null)
              ? new CSVReader(new InputStreamReader(file.getInputStream()))
              : new CSVReader(Files.newBufferedReader(Paths.get(filePath)))) {
        List<String[]> records = reader.readAll();
        for (String[] record : records) {
          if (record != null && record.length == 3) {
            Promotion promotion = new Promotion();
            promotion.setId(record[0]);
            promotion.setPrice(Double.parseDouble(record[1]));
            promotion.setExpirationDate(record[2]);
            // Save to Redis
            promotionRepository.save(promotion);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.info("All the records saved successfully");
    return PROMOTIONS_LOADED_SUCCESSFULLY;
  }

  public Optional<Promotion> getPromotion(String id) {
    return promotionRepository.findById(id);
  }
}
