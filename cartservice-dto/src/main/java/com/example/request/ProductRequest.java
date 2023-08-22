package com.example.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductRequest {
    @NotBlank(message = "ProductName must not be null")
    @Pattern(regexp = "^[a-zA-Z_ ]*$", message = "Invalid ProductName")
    private String productName;
    @NotNull(message = "Quantity must not be null")
    @Positive(message = "Quantity must not be negative or zero")
    private Integer quantity;
}
