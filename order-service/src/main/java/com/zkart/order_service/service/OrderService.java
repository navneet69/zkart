package com.zkart.order_service.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.zkart.order_service.dto.InventoryResponse;
import com.zkart.order_service.dto.OrderLineItemsDto;
import com.zkart.order_service.dto.OrderRequest;
import com.zkart.order_service.model.Order;
import com.zkart.order_service.model.OrderlineItems;
import com.zkart.order_service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        System.err.println(order.getOrderNumber());
        List<OrderlineItems> orderlineItems =  orderRequest.getOrderLineItemsDtoList()
                        .stream()
                        .map(this::mapToDto)
                        .toList();
        
        order.setOrderLineItemsList(orderlineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                        .map(OrderlineItems::getSkuCode)
                        .toList();

        // Call inventory-servide and place order if product is in stock
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory", 
                        UriBuilder -> UriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();
        Boolean allProductsInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);
        if(allProductsInStock){
            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }
        
    }

    private OrderlineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderlineItems orderlineItems = new OrderlineItems();
        orderlineItems.setPrice(orderLineItemsDto.getPrice());
        orderlineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderlineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderlineItems;
    }
}
