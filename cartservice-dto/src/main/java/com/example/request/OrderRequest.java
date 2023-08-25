package com.example.request;

import com.example.response.ProductResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private CustomerDetailRequest orderedCustomerDetail;
    private String orderId;
    @Email(message = "Invalid email")
    @NotNull(message = "Email cannot be null")
    private String email;
    private List<ProductResponse> availableProduct;
    private OrderStatus orderStatus = OrderStatus.PENDING;
    private double totalOrder;
}
