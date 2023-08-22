package com.example;

import com.example.request.CartOrderRequest;
import org.springframework.http.ResponseEntity;

public interface OrderService {
    ResponseEntity<?> placeOrder(CartOrderRequest cartOrderRequest);
}
