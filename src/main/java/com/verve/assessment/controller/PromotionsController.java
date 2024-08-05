package com.verve.assessment.controller;

import com.verve.assessment.model.Promotion;
import com.verve.assessment.model.ResponseBody;
import com.verve.assessment.service.PromotionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PromotionsController {

  public static final String URI = "/promotions";

  private final PromotionsService promotionsService;

  @PostMapping(
      value = URI,
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Operation(summary = "Reload new promotions")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = PromotionsService.PROMOTIONS_LOADED_SUCCESSFULLY,
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ResponseBody.class))
            })
      })
  public ResponseEntity<?> create(
      @Parameter(description = "Promotions csv file") @RequestParam("data")
          final MultipartFile file) {
    String response = promotionsService.processFile(file);
    return new ResponseEntity<>(new ResponseBody<>(response), HttpStatus.OK);
  }

  @GetMapping(URI + "/{id}")
  @Operation(summary = "Get promotion by id")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Promotion with given id found",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ResponseBody.class))
            }),
        @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content)
      })
  public ResponseEntity<?> getPromotionById(@PathVariable String id) {
    Optional<Promotion> promotion = promotionsService.getPromotion(id);
    return promotion
        .map(p -> new ResponseEntity<>(new ResponseBody(p), HttpStatus.OK))
        .orElseGet(
            () ->
                new ResponseEntity<>(
                    new ResponseBody("Promotion not found"), HttpStatus.NOT_FOUND));
  }
}
