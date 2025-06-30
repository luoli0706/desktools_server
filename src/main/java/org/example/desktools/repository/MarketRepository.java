package org.example.desktools.repository;

import org.example.desktools.entity.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketRepository extends JpaRepository<Plugin, Long> {
    Plugin findByName(String name);
}
