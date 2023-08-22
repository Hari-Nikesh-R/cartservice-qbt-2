package com.example.response;

import com.example.request.ProductRequest;
import lombok.Data;

import java.util.List;

@Data
public class CartResponse {
    private String guestId;
    private List<ProductRequest> cartProducts;
}
