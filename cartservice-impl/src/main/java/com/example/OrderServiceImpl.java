package com.example;

import com.example.document.Cart;
import com.example.request.CartOrderRequest;
import com.example.request.OrderRequest;
import com.example.request.OrderStatus;
import com.example.response.BaseResponse;
import com.example.response.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartService cartService;
    @Override
    public ResponseEntity<?> placeOrder(CartOrderRequest cartOrderRequest) {
        try {
            Optional<Cart> optionalCart = cartRepository.findByGuestId(cartOrderRequest.getGuestId());
            return optionalCart.map(cart -> {
                try {
                    OrderRequest orderRequest = new OrderRequest();
                    orderRequest.setOrderStatus(OrderStatus.COMPLETED);
                    orderRequest.setAvailableProduct(cart.getCartProducts().get(cartOrderRequest.getGuestId()));
                    orderRequest.setOrderedCustomerDetail(cartOrderRequest.getOrderedCustomerDetail());
                    AtomicReference<Double> totalOrder = new AtomicReference<>();
                    cart.getCartProducts().get(cartOrderRequest.getGuestId()).forEach((cartItems) -> {
                        totalOrder.set(cartItems.getProductPrice() * cartItems.getQuantity());
                    });
                    orderRequest.setTotalOrder(totalOrder.get());
                    ResponseEntity<OrderResponse> orderResponse = restTemplate.postForEntity("http://localhost:8099/order", orderRequest, OrderResponse.class);
                    clearCartItem(cartOrderRequest.getGuestId());
                    return ResponseEntity.ok(new BaseResponse<>(orderResponse, HttpStatus.OK.value(), null, true));
                }
                catch (Exception exception) {
                    return ResponseEntity.ok(new BaseResponse<>(null, HttpStatus.BAD_REQUEST.value(), "No Cart Found", false));
                }
            }).orElseGet(() -> ResponseEntity.ok(new BaseResponse<>(null, HttpStatus.NO_CONTENT.value(), "No Cart Found", false)));
        }
        catch (Exception exception) {
            return ResponseEntity.ok(new BaseResponse<>(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage(), false));
        }
    }

    private synchronized void clearCartItem(String guestId) {
        cartService.clearCartItems(guestId);
    }

}
