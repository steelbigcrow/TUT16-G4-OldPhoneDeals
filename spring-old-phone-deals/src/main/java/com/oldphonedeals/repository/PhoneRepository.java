package com.oldphonedeals.repository;

import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.enums.PhoneBrand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhoneRepository extends MongoRepository<Phone, String> {
    
    // 根据品牌查询
    Page<Phone> findByBrand(PhoneBrand brand, Pageable pageable);
    
    // 根据卖家查询
    Page<Phone> findBySellerId(String sellerId, Pageable pageable);
    
    List<Phone> findBySellerId(String sellerId);
    
    // 查询未禁用的商品
    Page<Phone> findByIsDisabledFalse(Pageable pageable);
    
    // 根据品牌和状态查询
    Page<Phone> findByBrandAndIsDisabledFalse(PhoneBrand brand, Pageable pageable);
    
    // 模糊搜索标题
    @Query("{'title': {$regex: ?0, $options: 'i'}, 'isDisabled': false}")
    Page<Phone> searchByTitle(String keyword, Pageable pageable);
    
    // 价格范围查询
    @Query("{'price': {$gte: ?0, $lte: ?1}, 'isDisabled': false}")
    Page<Phone> findByPriceRange(Double minPrice, Double maxPrice, Pageable pageable);
    
    // 综合查询：搜索 + 品牌 + 价格
    @Query("{'title': {$regex: ?0, $options: 'i'}, 'brand': ?1, 'price': {$lte: ?2}, 'isDisabled': false}")
    Page<Phone> findByTitleAndBrandAndPriceLessThanEqual(String keyword, PhoneBrand brand, Double maxPrice, Pageable pageable);
    
    // 搜索 + 价格
    @Query("{'title': {$regex: ?0, $options: 'i'}, 'price': {$lte: ?1}, 'isDisabled': false}")
    Page<Phone> findByTitleAndPriceLessThanEqual(String keyword, Double maxPrice, Pageable pageable);
    
    // 搜索 + 品牌
    @Query("{'title': {$regex: ?0, $options: 'i'}, 'brand': ?1, 'isDisabled': false}")
    Page<Phone> findByTitleAndBrand(String keyword, PhoneBrand brand, Pageable pageable);
    
    // 品牌 + 价格
    Page<Phone> findByBrandAndPriceLessThanEqualAndIsDisabledFalse(PhoneBrand brand, Double maxPrice, Pageable pageable);
    
    // 仅价格过滤
    Page<Phone> findByPriceLessThanEqualAndIsDisabledFalse(Double maxPrice, Pageable pageable);
    
    // 库存低于指定值的商品(用于 soldOutSoon)
    List<Phone> findTop5ByIsDisabledFalseOrderByStockAsc();
    
    // 低库存商品查询（库存 <= 指定值，按库存升序）
    List<Phone> findByStockLessThanEqualAndIsDisabledFalseOrderByStockAsc(int stock, Pageable pageable);
    
    // 删除卖家的所有商品
    void deleteBySellerId(String sellerId);
}