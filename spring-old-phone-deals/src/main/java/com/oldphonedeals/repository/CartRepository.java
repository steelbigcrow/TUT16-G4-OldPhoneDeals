package com.oldphonedeals.repository;

import com.oldphonedeals.entity.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {
    
    Optional<Cart> findByUserId(String userId);
    
    void deleteByUserId(String userId);
    
    boolean existsByUserId(String userId);
    
    /**
     * 查找所有包含指定商品ID的购物车
     * 用于删除商品时清理所有用户购物车中的该商品
     */
    @Query("{'items.phoneId': ?0}")
    java.util.List<Cart> findCartsContainingPhone(String phoneId);
}