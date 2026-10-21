package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    UserRepository userRepository;


}
