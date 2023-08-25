package com.example;

import com.example.document.Cart;
import com.example.request.CartOrderRequest;
import com.example.request.OrderRequest;
import com.example.request.OrderStatus;
import com.example.request.ProductRequest;
import com.example.response.BaseResponse;
import com.example.response.OrderResponse;
import com.example.response.ProductQuantityCheckResponse;
import com.example.response.ProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.helper.Urls.PRODUCT_ENDPOINT;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

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
                    cart.getCartProducts().get(cartOrderRequest.getGuestId()).forEach((cartItems) -> totalOrder.set(cartItems.getProductPrice() * cartItems.getQuantity()));
                    orderRequest.setTotalOrder(totalOrder.get());
                    orderRequest.setEmail(cartOrderRequest.getEmail());
                    OrderResponse orderResponse = createOrder(orderRequest);
                    //  clearCartItem(cartOrderRequest.getGuestId());
                    return ResponseEntity.ok(new BaseResponse<>(orderResponse, HttpStatus.OK.value(), null, true));
                } catch (Exception exception) {
                    log.error(exception.fillInStackTrace().getLocalizedMessage());
                    return ResponseEntity.ok(new BaseResponse<>(null, HttpStatus.BAD_REQUEST.value(), "No Cart Found", false));
                }
            }).orElseGet(() -> ResponseEntity.ok(new BaseResponse<>(null, HttpStatus.NO_CONTENT.value(), "No Cart Found", false)));
        } catch (Exception exception) {
            return ResponseEntity.ok(new BaseResponse<>(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage(), false));
        }
    }

    private OrderResponse createOrder(OrderRequest orderRequest) {
        try {
            if (checkProductOutOfStock(productQuantityCheck(orderRequest.getAvailableProduct()))) {
                reduceStockFromInventory(orderRequest);
                orderRequest.setOrderId(generateOrderId());
                publishToOrderService(orderRequest);
                OrderResponse orderResponse = new OrderResponse();
                BeanUtils.copyProperties(orderRequest, orderResponse);
                return orderResponse;
            } else {
                //todo: If we need to remove and proceed the product // orderRequest.setAvailableProduct(removeItemRegardsOutOfStock(productQuantityCheck(orderRequest.getAvailableProduct())));
                log.warn("Product was out of stock");
                return new OrderResponse("A product in your cart is out of stock");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            log.error(exception.fillInStackTrace().getLocalizedMessage());
            return null;
        }
    }

    private String generateOrderId() {
        String orderCharacter = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) {
            int index = (int) (rnd.nextFloat() * orderCharacter.length());
            salt.append(orderCharacter.charAt(index));
        }
        return salt.toString();
    }

    private void reduceStockFromInventory(OrderRequest orderRequest) {
        List<ProductRequest> purchaseRequests = new ArrayList<>();
        orderRequest.getAvailableProduct().forEach((product) -> {
            ProductRequest purchaseRequest = new ProductRequest();
            BeanUtils.copyProperties(product, purchaseRequest);
            purchaseRequests.add(purchaseRequest);
        });
        restTemplate.put(PRODUCT_ENDPOINT, purchaseRequests);
    }

    private boolean checkProductOutOfStock(ProductQuantityCheckResponse[] productQuantityCheckResponses) {
        for (ProductQuantityCheckResponse productQuantityCheckResponse : productQuantityCheckResponses) {
            if (!productQuantityCheckResponse.isAvailable()) {
                return false;
            }
        }
        return true;
    }

    private List<ProductResponse> removeItemRegardsOutOfStock(ProductQuantityCheckResponse[] productQuantityCheckResponses) {
        List<ProductResponse> productResponseList = new ArrayList<>();
        for (ProductQuantityCheckResponse productQuantityCheckResponse : productQuantityCheckResponses) {
            if (productQuantityCheckResponse.isAvailable()) {
                productResponseList.add(productQuantityCheckResponse.getProductResponse());
            }
        }
        return productResponseList;
    }

    private synchronized <T> ProductQuantityCheckResponse[] productQuantityCheck(List<T> productRequest) {
        return restTemplate.postForEntity("http://localhost:8089/product/quantity", productRequest, ProductQuantityCheckResponse[].class).getBody();
    }


    private void publishToOrderService(OrderRequest orderRequest) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            kafkaTemplate.send("mytopic", objectMapper.writeValueAsString(orderRequest));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void clearCartItem(String guestId) {
        cartService.clearCartItems(guestId);
    }

}
