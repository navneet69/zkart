package com.zkart.product_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.zkart.product_service.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {

}
