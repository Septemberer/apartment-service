package com.javadevjournal.jpa.repository;

import com.javadevjournal.jpa.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

	@Query(value = "SELECT u FROM Customer u where u.userName = ?1 and u.password = ?2 ")
	Optional<Customer> login(String username, String password);

	Optional<Customer> findByToken(String token);

	Optional<Customer> findByUserNameAndPassword(String userName, String password);
}
