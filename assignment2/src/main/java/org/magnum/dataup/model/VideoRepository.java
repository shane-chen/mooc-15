package org.magnum.dataup.model;

import java.util.Collection;

public interface VideoRepository {

    Video save(Video video);

    Video findById(long id);

    Collection<Video> findAll();

	float updateRating(long id, float rating);

}
