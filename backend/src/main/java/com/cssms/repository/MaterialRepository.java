package com.cssms.repository;

import com.cssms.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MaterialRepository extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {

    boolean existsByMaterialNameIgnoreCase(String materialName);

    boolean existsByMaterialNameIgnoreCaseAndIdNot(String materialName, Long id);
}
