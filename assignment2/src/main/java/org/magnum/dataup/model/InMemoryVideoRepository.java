package org.magnum.dataup.model;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class InMemoryVideoRepository implements VideoRepository {

	private static final AtomicLong currentId = new AtomicLong();
	private Map<Long, Video> videos = new ConcurrentHashMap<>();
	private Map<Long, Integer> ratings = new ConcurrentHashMap<>();

	private void checkAndSetId(Video entity) {
		if (entity.getId() == 0) {
			entity.setId(currentId.incrementAndGet());
		}
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String base = "http://" + request.getServerName()
				+ ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
		return base;
	}

	@Override
	public Video save(Video entity) {
		checkAndSetId(entity);
		videos.put(entity.getId(), entity);
		ratings.put(entity.getId(), 0);
		return entity;
	}

	@Override
	public Video findById(long id) {
		return videos.get(id);
	}

	@Override
	public Collection<Video> findAll() {
		return videos.values();
	}

	@Override
	public float updateRating(long id, float rating) {

		float newRating = 0f;

		synchronized (this) {
			Video video = findById(id);
			int c = ratings.get(id);
			float oldRating = video.getRating();
			newRating = (oldRating * c + rating) / (c + 1);
			video.setRating(newRating);
			ratings.put(id, ++c);
		}
		return newRating;
	}

}
