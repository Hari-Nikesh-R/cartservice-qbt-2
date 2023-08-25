package com.example.response;

import com.example.request.CustomerDetailRequest;
import com.example.request.OrderStatus;
import com.example.request.ProductRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    private double totalOrder;
    private CustomerDetailRequest orderedCustomerDetail;
    private OrderStatus orderStatus;
    private String orderId;
    private List<ProductRequest> availableProduct;
    private String errorDesc;

    public OrderResponse(String errorDesc) {
        this.errorDesc = errorDesc;
    }
}
