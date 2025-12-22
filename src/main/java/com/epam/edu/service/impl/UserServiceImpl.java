package com.epam.edu.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epam.edu.dao.UserRepository;
import com.epam.edu.entity.News;
import com.epam.edu.entity.RegistrationInfo;
import com.epam.edu.entity.User;
import com.epam.edu.entity.UserRole;
import com.epam.edu.service.ServiceException;
import com.epam.edu.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService, UserDetailsService {

	private final UserRepository repository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional(readOnly = true)
	public User findByEmail(String email) {
		log.info("Searching user by email: {}", email);
		return repository.findByEmail(email).orElseThrow(() -> {
			log.warn("User not found by email: {}", email);
			return new UsernameNotFoundException("User not found: " + email);
		});
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		log.info("Attempting to load user by email: {}", username);

		User user = repository.findByEmail(username).orElseThrow(() -> {
			log.warn("User not found with email: {}", username);
			return new UsernameNotFoundException("User not found with email: " + username);
		});

		log.info("User loaded: id={}, email={}, roleId={}, activ={}", user.getId(), user.getEmail(), user.getRoleId(),
				user.isActiv());

		UserRole role = UserRole.fromId(user.getRoleId());

		log.debug("Creating UserDetails: username={}, role={}, disabled={}", user.getEmail(), role.name(),
				!user.isActiv());

		return org.springframework.security.core.userdetails.User.builder().username(user.getEmail())
				.password(user.getPassword()).roles(role.name()).disabled(!user.isActiv()).build();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> checkCredentials(String login, String password) {
		log.info("Checking credentials for email: {}", login);
		Optional<User> userOpt = repository.findByEmail(login)
				.filter(user -> passwordEncoder.matches(password, user.getPassword()));

		if (userOpt.isPresent()) {
			log.info("Credentials verified for user: {}", login);
		} else {
			log.warn("Credentials invalid for user: {}", login);
		}

		return userOpt;
	}

	@Override
	@Transactional(readOnly = true)
	public User findById(Long id) {
		log.info("Searching user by ID: {}", id);
		User user = repository.findById(id).orElse(null);
		if (user == null) {
			log.warn("User not found with ID: {}", id);
		} else {
			log.debug("User found: id={}, email={}", user.getId(), user.getEmail());
		}
		return user;
	}

	@Override
	@Transactional
	public boolean addNew(RegistrationInfo info) {
		log.info("Registering new user: email={}", info.getEmail());

		if (repository.existsByEmail(info.getEmail())) {
			log.warn("Email already in use: {}", info.getEmail());
			throw new ServiceException("Email is already in use");
		}

		User user = new User();
		user.setEmail(info.getEmail());
		user.setName(info.getName());
		user.setPassword(passwordEncoder.encode(info.getPassword()));
		user.setRegistrationDate(LocalDate.now());
		user.setRoleId(UserRole.USER.getId());
		user.setActiv(true);
		user.setAuthor(false);

		repository.save(user);
		log.info("User successfully registered: id={}, email={}", user.getId(), user.getEmail());

		return true;
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAllAuthors() {
		log.info("Retrieving all authors");
		List<User> authors = repository.findAllAuthors();
		log.debug("Number of authors found: {}", authors.size());
		return authors;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEmail(String email) {
		boolean exists = repository.existsByEmail(email);
		log.debug("Email {} exists: {}", email, exists);
		return exists;
	}

	@Override
	@Transactional
	public User save(User user) {
		log.info("Saving user: id={}, email={}", user.getId(), user.getEmail());
		User saved = repository.save(user);
		log.info("User saved successfully: id={}, email={}", saved.getId(), saved.getEmail());
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAll() {
		log.info("Retrieving all users");
		List<User> users = repository.findAll();
		log.debug("Number of users found: {}", users.size());
		return users;
	}

	@Override
	@Transactional
	public void deleteUserByAdmin(Long userId, User admin) {
		log.info("Admin {} attempting to delete user {}", admin != null ? admin.getId() : null, userId);

		if (admin == null) {
			log.warn("Unauthorized delete attempt. No admin provided");
			throw new ServiceException("User not authorized");
		}

		if (admin.getRole() != UserRole.ADMIN) {
			log.warn("User {} has insufficient rights to delete user {}", admin.getId(), userId);
			throw new ServiceException("Insufficient rights");
		}

		User user = repository.findById(userId).orElseThrow(() -> {
			log.warn("User to delete not found: {}", userId);
			return new ServiceException("User not found");
		});

		if (user.getId().equals(admin.getId())) {
			log.warn("Admin {} attempted to delete themselves", admin.getId());
			throw new ServiceException("Cannot delete self");
		}

		if (Boolean.TRUE.equals(user.getAuthor())) {
			log.warn("Attempt to delete author user: userId={}", userId);
			throw new ServiceException("Cannot delete an author");
		}
		for (News news : user.getNews()) {
			news.getAuthors().remove(user);
		}
		user.getNews().clear();

		repository.delete(user);
		log.info("User deleted successfully: userId={}, deletedByAdminId={}", userId, admin.getId());
	}
}
