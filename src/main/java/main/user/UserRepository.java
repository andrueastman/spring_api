package main.user;

import org.springframework.data.repository.CrudRepository;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface UserRepository extends CrudRepository<User, Long> {

    boolean existsByUserPhone(String phone);
    User findByUserPhone(String phone);
    Iterable<User> findAllByAvailability(boolean availability);
}
