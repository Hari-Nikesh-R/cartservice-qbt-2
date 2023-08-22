package com.example.request;

import com.example.response.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private CustomerDetailRequest orderedCustomerDetail;
    private List<ProductResponse> availableProduct;
    private OrderStatus orderStatus = OrderStatus.PENDING;
    private double totalOrder;
}
