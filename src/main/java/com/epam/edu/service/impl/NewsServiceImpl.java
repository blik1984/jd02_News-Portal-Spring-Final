package com.epam.edu.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epam.edu.dao.NewsRepository;
import com.epam.edu.entity.News;
import com.epam.edu.entity.NewsGroup;
import com.epam.edu.entity.UserRole;
import com.epam.edu.service.NewsService;
import com.epam.edu.service.PagedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsServiceImpl implements NewsService {

	private final NewsRepository newsRepository;

	@Override
	@Transactional
	public News saveNews(News news) {
		log.info("Saving news: id={}, title={}", news.getId(), news.getTitle());

		String filePath = saveContentToFile(news.getContent());
		news.setContentPath(filePath);

		LocalDateTime now = LocalDateTime.now();
		if (news.getId() == null) {
			news.setCreateDateTime(now);
			log.info("Setting creation date: {}", now);
		}
		news.setUpdateDateTime(now);
		log.info("Setting update date: {}", now);

		News saved = newsRepository.save(news);
		log.info("News saved successfully: id={}, title={}", saved.getId(), saved.getTitle());
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public News getNewsById(Long id) {
		log.info("Fetching news by id: {}", id);

		return newsRepository.findById(id).map(n -> {
			if (n.getContentPath() != null) {
				try {
					n.setContent(Files.readString(Path.of(n.getContentPath())));
					log.debug("News content loaded from file: {}", n.getContentPath());
				} catch (IOException e) {
					log.error("Error reading news content file: {}", n.getContentPath(), e);
					throw new RuntimeException(e);
				}
			}
			return n;
		}).orElseGet(() -> {
			log.warn("News not found: id={}", id);
			return null;
		});
	}

	@Override
	@Transactional(readOnly = true)
	public PagedResult<News> getNewsForPage(int page, int size, UserRole userRole, Integer newsGroupId) {
		log.info("Fetching news page: page={}, size={}, newsGroupId={}, userRole={}", page, size, newsGroupId,
				userRole);

		int pageSize = validatePageSize(size);
		boolean onlyPublished = (userRole != UserRole.ADMIN);
		long total = newsRepository.countFilteredNews(newsGroupId, null, onlyPublished);

		List<News> content = newsRepository.findFilteredNews(newsGroupId, null, onlyPublished, page, pageSize);
		log.debug("News page fetched: totalItems={}, pageSize={}, itemsInPage={}", total, pageSize, content.size());

		return new PagedResult<>(content, page, pageSize, total);
	}

	@Transactional
	@Override
	public boolean deleteNews(Long id) {
		log.info("Deleting news: id={}", id);
		boolean deleted = newsRepository.deleteById(id);
		if (deleted) {
			log.info("News deleted successfully: id={}", id);
		} else {
			log.warn("News deletion failed or not found: id={}", id);
		}
		return deleted;
	}

	private int validatePageSize(int size) {
		if (size <= 3)
			return 3;
		if (size <= 6)
			return 6;
		return 9;
	}

	@Override
	@Transactional(readOnly = true)
	public List<NewsGroup> findAllNewsGroups() {
		log.info("Fetching all news groups");
		List<NewsGroup> groups = newsRepository.findAllNewsGroups();
		log.debug("Number of news groups found: {}", groups.size());
		return groups;
	}

	@Override
	@Transactional(readOnly = true)
	public NewsGroup findNewsGroupById(Long newsGroupId) {
		log.info("Fetching news group by id: {}", newsGroupId);
		NewsGroup group = newsRepository.findNewsGroupById(newsGroupId);
		if (group == null) {
			log.warn("News group not found: id={}", newsGroupId);
		} else {
			log.debug("News group found: id={}, name={}", group.getId(), group.getName());
		}
		return group;
	}

	private String saveContentToFile(String text) {
		try {
			Path dir = Path.of("resources/news/content");
			Files.createDirectories(dir);
			Path file = dir.resolve("news_" + System.currentTimeMillis() + ".txt");
			Files.writeString(file, text);
			log.debug("News content saved to file: {}", file.toString());
			return file.toString();
		} catch (IOException e) {
			log.error("Error saving news content to file", e);
			throw new RuntimeException(e);
		}
	}
}
