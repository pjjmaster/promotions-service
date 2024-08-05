package com.verve.assessment.service;

import com.verve.assessment.exceptions.PromotionProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledPromotionsLoader {

  private final PromotionsService promotionService;

  @Scheduled(fixedRate = 1800000)
  public void reloadAllPromotions() {
    try {
      promotionService.processFile(null);
    } catch (PromotionProcessingException e) {
      log.error("Error during scheduled promotions loading: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error during scheduled promotions loading: {}", e.getMessage());
    }
  }
}
