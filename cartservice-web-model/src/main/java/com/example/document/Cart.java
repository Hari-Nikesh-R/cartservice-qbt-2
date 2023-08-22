package com.example.document;

import com.example.response.ProductResponse;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "cart")
@Data
public class Cart {
    @Id
    private String id;
    private String guestId;
    private Map<String, List<ProductResponse>> cartProducts;
}
