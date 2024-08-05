package com.verve.assessment.service;

import com.verve.assessment.model.Promotion;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface PromotionsService {

  String PROMOTIONS_LOADED_SUCCESSFULLY = "Promotions loaded successfully";

  String processFile(MultipartFile file);

  Optional<Promotion> getPromotion(String id);
}
