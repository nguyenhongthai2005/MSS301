package com.mss301.brand.repository;

import com.mss301.brand.entity.WatchBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// [TODO-5.1: repository WatchBrand]
@Repository
public interface BrandRepository extends JpaRepository<WatchBrand, Integer> {
    Optional<WatchBrand> findByBrandName(String brandName);
}
