package com.verve.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("Promotion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
  private String id;
  private double price;
  private String expirationDate;
}
