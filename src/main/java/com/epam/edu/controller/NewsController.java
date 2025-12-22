package com.epam.edu.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.epam.edu.entity.News;
import com.epam.edu.entity.NewsGroup;
import com.epam.edu.entity.User;
import com.epam.edu.service.NewsService;
import com.epam.edu.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
@Slf4j
public class NewsController {

	private final NewsService newsService;
	private final UserService userService;

	@PostMapping("/save_news")
	public String saveNews(@ModelAttribute("news") News news, @RequestParam Long newsGroupId,
			@RequestParam(required = false) List<Long> authorIds, Authentication authentication) {

		log.info("Saving news. newsId={}, newsGroupId={}, authors={}, publisher={}", news.getId(), newsGroupId,
				authorIds, authentication != null ? authentication.getName() : null);

		NewsGroup group = newsService.findNewsGroupById(newsGroupId);
		news.setNewsGroup(group);

		if (authorIds != null && !authorIds.isEmpty()) {
			List<User> authors = authorIds.stream().map(userService::findById).collect(Collectors.toList());
			news.setAuthors(authors);
			log.debug("Assigned authors to news: {}", authors.stream().map(User::getEmail).toList());
		} else {
			news.setAuthors(Collections.emptyList());
			log.debug("No authors assigned to news. newsId={}", news.getId());
		}

		if (authentication != null && authentication.isAuthenticated()) {
			User currentUser = userService.findByEmail(authentication.getName());
			if (currentUser != null) {
				news.setPublisher(currentUser);
				log.debug("Publisher set: {}", currentUser.getEmail());
			} else {
				log.warn("Authenticated user not found in database: {}", authentication.getName());
			}
		} else {
			log.warn("No authenticated user. newsId={}", news.getId());
		}

		newsService.saveNews(news);
		log.info("News saved successfully. newsId={}", news.getId());

		return "redirect:/";
	}

	@PostMapping("/delete_news")
	public String deleteNews(@RequestParam Long newsId, @RequestParam(required = false) Integer newsGroupId,
			@RequestParam(defaultValue = "0") int currentPage, Authentication auth) {

		log.info("Deleting news. newsId={}, newsGroupId={}, requestedBy={}", newsId, newsGroupId,
				auth != null ? auth.getName() : null);

		newsService.deleteNews(newsId);

		log.info("News deleted successfully. newsId={}", newsId);

		return "redirect:/?page=" + currentPage + (newsGroupId != null ? "&newsGroupId=" + newsGroupId : "");
	}

}
