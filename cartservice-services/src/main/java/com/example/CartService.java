package com.example;

import com.example.request.CartRequest;
import com.example.response.BaseResponse;
import com.example.response.ProductResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface CartService {
    ResponseEntity<?> addToCart(CartRequest cartRequest);

    BaseResponse<String> clearCart();

    BaseResponse<List<ProductResponse>> clearCartItems(String guestId);

    BaseResponse<List<ProductResponse>> viewCartItems(String guestId);


}
