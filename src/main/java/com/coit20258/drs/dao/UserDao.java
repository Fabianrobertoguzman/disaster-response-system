package com.coit20258.drs.dao;

import java.util.Optional;

import com.coit20258.drs.model.User;

public interface UserDao {
    public User register(User user);
    
    public boolean emailExists(String email);
    
    public boolean setActiveStatus(int userId, boolean isActive);
    
    public Optional<User> login(String email, String password);
    
    public Optional<User> findByEmail(String email);
}
