package com.kunteng.cyria.dashboard.service;

import com.kunteng.cyria.dashboard.domain.Dashboard;
import com.kunteng.cyria.dashboard.domain.Published;
import com.kunteng.cyria.dashboard.domain.Translation;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface DashboardService {

	Page<Dashboard> getAllDashboard(String id, Integer page, Integer size);
	Dashboard getDashboardById(String id);
	Dashboard createNewDashboard(String id, Translation ts);
    String deleteDashboardByHash(String id);
	void publishDashboardById(String id, String option);
	Published getPublishedById(String id);
	void updateDashboardById(String id, String db) throws IOException, Exception;
	Object uploadImage(String id, MultipartFile file) throws IllegalStateException, IOException;
}