package com.verve.assessment.service;

import com.verve.assessment.model.Promotion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PromotionServiceEfficientTest {

  @Mock
  private RedisTemplate<String, Promotion> redisTemplate;

  @Mock
  private ValueOperations<String, Promotion> valueOperations;

  @Mock
  private MultipartFile file;

  @InjectMocks
  private PromotionServiceEfficient promotionServiceEfficient;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  void testGetPromotionSuccess() {
    Promotion mockPromotion = new Promotion();
    mockPromotion.setId("test-id");
    when(valueOperations.get("test-id")).thenReturn(mockPromotion);

    Optional<Promotion> result = promotionServiceEfficient.getPromotion("test-id");

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo("test-id");
  }

  @Test
  void testGetPromotionNotFound() {
    when(valueOperations.get("test-id")).thenReturn(null);

    Optional<Promotion> result = promotionServiceEfficient.getPromotion("test-id");

    assertThat(result).isEmpty();
  }
}
