package com.cssms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "materials")
@SQLRestriction("deleted = false")
public class Material extends BaseEntity {

    @Column(name = "material_code", nullable = false, length = 20)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    private String unit = "TON";
}
