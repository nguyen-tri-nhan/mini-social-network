package com.nhan.minisocial;

import com.nhan.minisocial.core.entity.Role;
import com.nhan.minisocial.core.entity.RoleName;
import com.nhan.minisocial.core.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EntityScan(basePackageClasses = {
        MinisocialApplication.class,
        Jsr310JpaConverters.class
})
public class MinisocialApplication {

    @Autowired
    RoleRepository roleRepository;

    @PostConstruct
    void init(){
        roleRepository.save(new Role(RoleName.ROLE_USER));
        roleRepository.save(new Role(RoleName.ROLE_ADMIN));
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }


    public static void main(String[] args) {
        SpringApplication.run(MinisocialApplication.class, args);
    }

}
