package com.example.controller;

import com.example.CartService;

import com.example.helper.Urls;
import com.example.request.CartRequest;
import com.example.response.BaseResponse;
import com.example.response.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = Urls.CART)
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping(value = Urls.ADD_PRODUCTS)
    public ResponseEntity<?> addToCart(@Valid @RequestBody CartRequest cartRequest) {
        return cartService.addToCart(cartRequest);
    }

    @DeleteMapping
    public BaseResponse<String> clearCart() {
        return cartService.clearCart();
    }

    @DeleteMapping(value = Urls.DELETE_ITEM)
    public BaseResponse<List<ProductResponse>> clearCartItem(@RequestParam("guestId") String guestId) {
        return cartService.clearCartItems(guestId);
    }

    @GetMapping(value = Urls.VIEW)
    public BaseResponse<List<ProductResponse>> getCartItems(@RequestParam("guestId") String guestId) {
        return cartService.viewCartItems(guestId);
    }
}
