package com.nhan.minisocial;

import com.nhan.minisocial.core.entity.Role;
import com.nhan.minisocial.core.entity.RoleName;
import com.nhan.minisocial.core.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EntityScan(basePackageClasses = {
        MinisocialApplication.class,
        Jsr310JpaConverters.class
})
public class MinisocialApplication {


    @PostConstruct
    void init(){
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }


    public static void main(String[] args) {
        SpringApplication.run(MinisocialApplication.class, args);
    }

}
