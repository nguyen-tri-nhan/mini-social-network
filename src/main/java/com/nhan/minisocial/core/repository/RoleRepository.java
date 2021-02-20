package com.nhan.minisocial.core.repository;

import com.nhan.minisocial.core.entity.RoleEntity;
import com.nhan.minisocial.core.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(RoleName name);
}
