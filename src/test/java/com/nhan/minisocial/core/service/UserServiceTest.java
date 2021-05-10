package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.User;
import com.nhan.minisocial.core.repository.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@Import({
        UserService.class,
})
public class UserServiceTest {

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    public void testGetUser() {
        //given
        User user = new User();
        user.setAvatar("avatar.jpg");
        user.setEmail("test@test.test");
        user.setFirstname("First");
        user.setLastname("Last");
        user.setId(1L);
        user.setUsername("username");
        //when
        when(userRepository.getOne(1L)).thenReturn(user);
        //then
        User actual = userService.getUser(1L);

        verify(userRepository).getOne(eq(1L));

        assertEquals(actual, user);
    }
}
