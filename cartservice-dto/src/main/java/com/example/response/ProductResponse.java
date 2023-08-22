package com.example.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProductResponse {
    @NotBlank(message = "ProductName must not be null")
    @Pattern(regexp = "^[a-zA-Z_ ]*$", message = "Invalid ProductName")
    private String productName;
    @NotNull(message = "Price must not be null")
    @Positive(message = "Price cannot be negative")
    private Double productPrice;
    @NotNull(message = "Quantity must not be null")
    @Positive(message = "Quantity must not be negative or zero")
    private Integer quantity;
}
