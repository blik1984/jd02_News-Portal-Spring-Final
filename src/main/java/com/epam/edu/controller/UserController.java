package com.epam.edu.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.epam.edu.entity.RegistrationInfo;
import com.epam.edu.entity.User;
import com.epam.edu.service.ServiceException;
import com.epam.edu.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class UserController {

	private final UserService userService;

	@PostMapping("/registration")
	public String doRegistration(@RequestParam String name, @RequestParam String email, @RequestParam String password,
			@RequestParam String passwordConfirm, RedirectAttributes redirectAttributes) {

		log.info("Attempting registration: email={}", email);

		if (!password.equals(passwordConfirm)) {
			log.warn("Passwords do not match for email={}", email);
			redirectAttributes.addAttribute("errorMessage", "Passwords do not match");
			return "redirect:/page_registration";
		}

		if (userService.existsByEmail(email)) {
			log.warn("User with this email already exists: email={}", email);
			redirectAttributes.addAttribute("errorMessage", "User with this email already exists");
			return "redirect:/page_registration";
		}

		RegistrationInfo info = new RegistrationInfo(email, password, name);
		if (!userService.addNew(info)) {
			log.error("Error creating user: email={}", email);
			redirectAttributes.addAttribute("errorMessage", "Something went wrong. Please start over.");
			return "redirect:/page_registration";
		}

		log.info("Registration successful: email={}", email);
		return "redirect:/page_auth";
	}

	@PostMapping("/update_profile")
	public String saveInfo(@RequestParam String name, @RequestParam String surname,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirthday,
			Authentication auth, RedirectAttributes redirectAttributes) {
		System.out.println(dateOfBirthday);
		String email = auth.getName();
		log.info("Updating user profile: email={}", email);

		User user = userService.findByEmail(email);

		if (user != null) {
			user.setName(name);
			user.setSurname(surname);
			user.setDateOfBirthday(dateOfBirthday);
			userService.save(user);
			log.info("Profile updated successfully: email={}", email);
			redirectAttributes.addFlashAttribute("successMessage", "Data saved successfully");
		} else {
			log.warn("Profile not found for update: email={}", email);
			redirectAttributes.addFlashAttribute("errorMessage", "Unable to save data. User not found.");
		}
		return "redirect:/page_profile";
	}

	@PostMapping("/update_admin_user/{id}")
	public String updateAdminUser(@PathVariable Long id, @RequestParam(name = "active", required = false) Boolean activ,
			@RequestParam(name = "author", required = false) Boolean author, Authentication auth,
			RedirectAttributes redirectAttributes) {

		if (activ == null)
			activ = false;
		if (author == null)
			author = false;

		User currentAdmin = userService.findByEmail(auth.getName());
		User user = userService.findById(id);

		log.info("Admin attempts to update user: adminId={}, targetUserId={}, active={}, author={}",
				currentAdmin != null ? currentAdmin.getId() : null, id, activ, author);

		if (user == null) {
			log.warn("User not found: id={}", id);
			redirectAttributes.addFlashAttribute("errorMessage", "User not found");
			return "redirect:/page_admin_panel";
		}

		if (currentAdmin != null && user.getId().equals(currentAdmin.getId()) && activ == false) {
			log.warn("Attempted self-deactivation by admin: id={}", id);
			redirectAttributes.addFlashAttribute("errorMessage", "Cannot deactivate own account");
			return "redirect:/page_admin_panel";
		}

		user.setActiv(activ);
		user.setAuthor(author);
		userService.save(user);

		log.info("User updated successfully: id={}", id);
		redirectAttributes.addFlashAttribute("successMessage", "User data updated successfully");

		return "redirect:/admin_panel";
	}

	@PostMapping("/delete")
	public String deleteUser(@RequestParam Long userId, Authentication auth, RedirectAttributes redirectAttributes) {

		User currentAdmin = userService.findByEmail(auth.getName());

		log.info("Request to delete user: adminId={}, targetUserId={}",
				currentAdmin != null ? currentAdmin.getId() : null, userId);

		try {
			userService.deleteUserByAdmin(userId, currentAdmin);
			redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully");
		} catch (ServiceException ex) {
			log.warn("Error deleting user: {}", ex.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}

		return "redirect:/page_admin_panel";
	}

}
