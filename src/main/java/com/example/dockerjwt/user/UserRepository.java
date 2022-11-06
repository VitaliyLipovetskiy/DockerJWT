package com.example.dockerjwt.user;

import java.util.Optional;

public interface UserRepository {
    Optional<User> addUser(User user);

    Optional<User> getUser(String userId);

    Optional<User> getUserByEmail(String email);

    User saveUser(User user);
}
