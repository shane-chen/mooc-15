/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoRepository;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

	@Autowired
	private VideoRepository videoRepository;

	@Autowired
	private VideoFileManager videoDataMgr;

	public VideoController() throws IOException {
		videoDataMgr = VideoFileManager.get();
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videoRepository.findAll();
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video video) {
		Video v = videoRepository.save(video);
		v.setDataUrl(getDataUrl(v.getId()));
		return v;
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
			@RequestParam(VideoSvcApi.DATA_PARAMETER) MultipartFile videoData) {
		Video video = videoRepository.findById(id);
		try {
			video.setContentType(videoData.getContentType());
			videoDataMgr.saveVideoData(video, videoData.getInputStream());
		} catch (Throwable e) {
			throw new ResourceNotFoundException();
		}
		return new VideoStatus(VideoState.READY);
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
	public void getData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id, HttpServletResponse response) {
		Video video = videoRepository.findById(id);
		try {
			if (videoDataMgr.hasVideoData(video)) {
				response.setContentType(video.getContentType());
				videoDataMgr.copyVideoData(video, response.getOutputStream());
			}
		} catch (Throwable e) {
			throw new ResourceNotFoundException();
		}
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_RATING_PATH, method = RequestMethod.GET)
	public @ResponseBody float updateRating(@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
			@PathVariable(VideoSvcApi.RATING_PARAMETER) float rating) {
		return videoRepository.updateRating(id, rating);
	}


	private String getDataUrl(long videoId) {
		String url = getUrlBaseForLocalServer()
				+ VideoSvcApi.VIDEO_DATA_PATH.replace("{" + VideoSvcApi.ID_PARAMETER + "}", "" + videoId);
		return url;
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String base = "http://" + request.getServerName()
				+ ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
		return base;
	}

}
