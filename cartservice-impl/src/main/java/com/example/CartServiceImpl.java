package com.example;

import com.example.document.Cart;
import com.example.request.CartRequest;
import com.example.request.ProductRequest;
import com.example.response.BaseResponse;
import com.example.response.ProductQuantityCheckResponse;
import com.example.response.ProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public ResponseEntity<?> addToCart(CartRequest cartRequest) {
        try {
            Optional<Cart> optionalCart = cartRepository.findByGuestId(cartRequest.getGuestId());
            return optionalCart.map(cart -> {
                try {
                    return ResponseEntity.ok(new BaseResponse<>(updateCartItem(cart, cartRequest), HttpStatus.OK.value(), null, true));
                } catch (OutOfQuantityException e) {
                    return ResponseEntity.ok(new BaseResponse<>(null, HttpStatus.BAD_REQUEST.value(), e.getMessage(), false));
                }
            }).orElseGet(() -> {
                try {
                    return ResponseEntity.ok(new BaseResponse<>(createCart(cartRequest), HttpStatus.OK.value(), null, true));
                } catch (OutOfQuantityException e) {
                    return ResponseEntity.ok(new BaseResponse<>(null, HttpStatus.BAD_REQUEST.value(), e.getMessage(), false));
                }
            });
        } catch (Exception exception) {
            return ResponseEntity.ok(new BaseResponse<>(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage(), false));
        }
    }

    @Override
    public BaseResponse<String> clearCart() {
        try {
            cartRepository.deleteAll();
            return new BaseResponse<>("Deleted Cart", HttpStatus.OK.value(), null, true);

        } catch (Exception exception) {
            return new BaseResponse<>(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage(), false);
        }
    }

    @Override
    public synchronized BaseResponse<List<ProductResponse>> clearCartItems(String guestId) {
        try {
            Optional<Cart> optionalCart = cartRepository.findByGuestId(guestId);
            return optionalCart.map(cart -> {
                cart.getCartProducts().get(guestId).clear();
                return new BaseResponse<>(migrateCartObjects(cartRepository.save(cart).getGuestId()), HttpStatus.OK.value(), null, true);
            }).orElseGet(() -> new BaseResponse<>(null, HttpStatus.NO_CONTENT.value(), "No Cart Found", false));
        } catch (Exception exception) {
            return new BaseResponse<>(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage(), false);
        }
    }

    @Override
    public BaseResponse<?> deleteItem(String guestId, String productName) {
        try {
            Optional<Cart> optionalCart = cartRepository.findByGuestId(guestId);
            return optionalCart.map(cart -> {
                ProductResponse productResponse = cart.getCartProducts().get(guestId).stream().filter((product) -> product.getProductName().equals(productName)).findFirst().orElse(null);
                if (Objects.nonNull(productResponse)) {
                    cart.getCartProducts().get(guestId).remove(productResponse);
                    cartRepository.save(cart);
                    return new BaseResponse<>(cart.getCartProducts().get(guestId), HttpStatus.OK.value(), null, true);
                } else {
                    return new BaseResponse<>(null, HttpStatus.NO_CONTENT.value(), "No such product found in cart", false);
                }
            }).orElseGet(() -> new BaseResponse<>(null, HttpStatus.NO_CONTENT.value(), "No Cart found", false));
        } catch (Exception exception) {
            return new BaseResponse<>(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage(), false);
        }
    }

    @Override
    public BaseResponse<List<ProductResponse>> viewCartItems(String guestId) {
        try {
            Optional<Cart> optionalCart = cartRepository.findByGuestId(guestId);
            return optionalCart.map(cart -> new BaseResponse<>(cart.getCartProducts().get(guestId), HttpStatus.OK.value(), null, true)).orElseGet(() -> new BaseResponse<>(null, HttpStatus.NO_CONTENT.value(), "No cart is available for the guestID", false));
        } catch (Exception exception) {
            return new BaseResponse<>(null, HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage(), false);
        }
    }

    private List<ProductResponse> createCart(CartRequest cartRequest) throws OutOfQuantityException {
        List<ProductRequest> productRequestList = new ArrayList<>();
        productRequestList.add(cartRequest.getCartProduct());
        return getProductAvailabilityAndConstructCart(productRequestList, cartRequest);
    }

    private List<ProductResponse> getProductAvailabilityAndConstructCart(List<ProductRequest> productRequestList, CartRequest cartRequest) {
        String outOfStockError = "";
        List<ProductResponse> productResponses = new ArrayList<>();
        ProductQuantityCheckResponse[] productQuantityCheckResponse = productQuantityCheck(productRequestList);
        for (ProductQuantityCheckResponse quantityCheckResponse : productQuantityCheckResponse) {
            if (quantityCheckResponse.isAvailable()) {
                productResponses = createCartInDb(cartRequest, quantityCheckResponse);
            } else {
                outOfStockError = quantityCheckResponse.getMessage();
            }
        }
        if (productResponses.size() != 0) {
            return productResponses;
        } else {
            throw new OutOfQuantityException(outOfStockError);
        }
    }

    private List<ProductResponse> getProductAvailabilityAndConstructCart(List<ProductRequest> productRequestList, CartRequest cartRequest, Cart cart) {
        String outOfStockError = "";
        Cart cartUpdate = null;
        List<ProductResponse> productResponses = new ArrayList<>();
        ProductQuantityCheckResponse[] productQuantityCheckResponse = productQuantityCheck(productRequestList);
        for (ProductQuantityCheckResponse quantityCheckResponse : productQuantityCheckResponse) {
            if (quantityCheckResponse.isAvailable()) {
                cartUpdate = serializeCartRequest(cartRequest, cart, quantityCheckResponse.getProductResponse());
                productResponses = migrateCartObjects(cartRepository.save(cartUpdate).getGuestId());
            } else {
                outOfStockError = quantityCheckResponse.getMessage();
            }
        }
        if (productResponses.size() != 0) {
            return productResponses;
        }
        if (outOfStockError.isEmpty()) {
            outOfStockError = "Product not found";
        }
        throw new OutOfQuantityException(outOfStockError);
    }

    private List<ProductResponse> createCartInDb(CartRequest cartRequest, ProductQuantityCheckResponse productQuantityCheckResponse) {
        Cart cart = new Cart();
        Map<String, List<ProductResponse>> cartUpdate = new HashMap<>();
        List<ProductResponse> productRequestList = new ArrayList<>();
        ProductResponse productResponse = productQuantityCheckResponse.getProductResponse();
        double price = productResponse.getProductPrice();
        BeanUtils.copyProperties(cartRequest.getCartProduct(), productResponse);
        productResponse.setProductPrice(price);
        productRequestList.add(productResponse);
        cartUpdate.put(cartRequest.getGuestId(), productRequestList);
        cart.setGuestId(cartRequest.getGuestId());
        cart.setCartProducts(cartUpdate);
        return migrateCartObjects(cartRepository.save(cart).getGuestId());
    }

    private synchronized List<ProductResponse> migrateCartObjects(String guestId) {
        return viewCartItems(guestId).getData();
    }

    private synchronized <T> ProductQuantityCheckResponse[] productQuantityCheck(List<T> productRequest) {
        return restTemplate.postForEntity("http://localhost:8089/product/quantity", productRequest, ProductQuantityCheckResponse[].class).getBody();
    }

    private List<ProductResponse> updateCartItem(Cart cart, CartRequest cartRequest) throws OutOfQuantityException {
        List<ProductRequest> productRequestList = new ArrayList<>();
        productRequestList.add(cartRequest.getCartProduct());
        return getProductAvailabilityAndConstructCart(productRequestList, cartRequest, cart);
    }

    private Cart serializeCartRequest(CartRequest cartRequest, Cart cart, ProductResponse productResponse) {
        AtomicBoolean productExist = new AtomicBoolean(false);
        cart.getCartProducts().get(cartRequest.getGuestId()).forEach((cartItem) -> {
            if (cartItem.getProductName().equals(cartRequest.getCartProduct().getProductName())) {
                productExist.set(true);
                cartItem.setQuantity(cartRequest.getCartProduct().getQuantity());
                cartItem.setProductPrice(productResponse.getProductPrice());
            }
        });
        if (!productExist.get()) {
            double price = productResponse.getProductPrice();
            BeanUtils.copyProperties(cartRequest.getCartProduct(), productResponse);
            productResponse.setProductPrice(price);
            cart.getCartProducts().get(cartRequest.getGuestId()).add(productResponse);
        }
        return cart;
    }
}
