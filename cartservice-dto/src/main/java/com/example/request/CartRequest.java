package com.example.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartRequest {
    @NotNull(message = "guestId must not be null")
    private String guestId;
    @NotNull(message = "cartProduct is mandatory")
    @Valid
    private ProductRequest cartProduct;
}
