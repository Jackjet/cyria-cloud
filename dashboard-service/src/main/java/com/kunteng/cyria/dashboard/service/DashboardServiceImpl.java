package com.kunteng.cyria.dashboard.service;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Base64.Decoder;

import javax.imageio.ImageIO;

import org.bson.BasicBSONDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import com.kunteng.cyria.dashboard.domain.Config;
import com.kunteng.cyria.dashboard.domain.Dashboard;
import com.kunteng.cyria.dashboard.domain.Published;
import com.kunteng.cyria.dashboard.domain.Template;
import com.kunteng.cyria.dashboard.domain.Translation;
import com.kunteng.cyria.dashboard.repository.DashboardRepository;
import com.kunteng.cyria.dashboard.repository.PublishedRepository;
import com.kunteng.cyria.dashboard.repository.TemplateRepository;
import com.kunteng.cyria.dashboard.utils.Utils;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import sun.misc.BASE64Decoder;

@Service
public class DashboardServiceImpl implements DashboardService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private DashboardRepository dashboardRepository;
	
	@Autowired
	private TemplateRepository templateRepository;
	
	@Autowired
	private PublishedRepository publishedRepository;

	public Page<Dashboard> getAllDashboard(String user, Integer page, Integer size) {
		Sort sort = new Sort(Sort.Direction.ASC,"timestamp");
		PageRequest pageRequest = new PageRequest(page-1, size, sort);
		Page<Dashboard> dashboard = dashboardRepository.findByUser(user, pageRequest);
		return dashboard;
	}

	public Dashboard getDashboardById(String id) {
		Dashboard dashboard =  dashboardRepository.findByHash(id);
		return dashboard;
	}
	
	public static Dashboard NULLDashboard=new Dashboard();
	
	public Dashboard createNewDashboard(String id, Translation translation){
		log.debug("translation=" + translation);

		if(!translation.getIsTemplate()) {
			Dashboard dashboard = new Dashboard();
			
			dashboard.getConfig().setTitle(translation.getName());
			dashboard.getConfig().setAbout(translation.getAbout());
			if(!translation.getTemplateId().equals("")) {
				if(templateRepository.exists(translation.getTemplateId())) {
					Template template = templateRepository.findByHash(translation.getTemplateId());
					dashboard.getConfig().setWidth(template.getConfig().getWidth());
					dashboard.getConfig().setHeigth(template.getConfig().getHeight());
					dashboard.getConfig().setZoom(template.getConfig().getZoom());
					dashboard.getConfig().setBackgroupColor(template.getConfig().getBackgroupColor());
					dashboard.getConfig().setBackPic(template.getConfig().getBackPic());
					dashboard.setWidget(template.getWidget());
				}
			}
			dashboard.getConfig().setTimestamp(LocalDate.now());
			dashboard.getPublish().setStatus("unpublished");
			dashboard.getPublish().setTimestamp(LocalDate.now());
			dashboard.setUser(translation.getUser());

			dashboardRepository.save(dashboard);
			return dashboard;
		}
		return NULLDashboard;
	}
	
	public String deleteDashboardByHash(String id) {
		return dashboardRepository.deleteByHash(id);
	}

	public void publishDashboardById(String id, String option){
		System.out.println("option="+option);
		System.out.println("id="+id);
		option=option.trim();
		Dashboard dashboard = dashboardRepository.findByHash(id);

		dashboard.getPublish().setStatus(option);
		dashboard.getPublish().setTimestamp(LocalDate.now());
		System.out.println("hash1="+ dashboard.getPublish().getHash());
		if(option.equals("unpublished=")){
			publishedRepository.deleteByHash(dashboard.getPublish().getHash());
			System.out.println("hash2="+ dashboard.getPublish().getHash());
			dashboard.getPublish().setHash("");
		}
		
		if(option.equals("published=")){
			System.out.println("XXXXXXXXXX");
			Published published = new Published();
			dashboard.getPublish().setHash(published.getHash());
			published.setConfig(dashboard.getConfig());
			published.setWidget(dashboard.getWidget());
			publishedRepository.save(published);
		}
		
		if(option.equals("republished=")){
			dashboard.getPublish().setStatus("published");
			Published published = publishedRepository.findByHash(dashboard.getPublish().getHash());
			published.setConfig(dashboard.getConfig());
			published.setWidget(dashboard.getWidget());
			publishedRepository.save(published);
		}
		dashboardRepository.save(dashboard);
	}
	
/*	private static String uploadFile(byte[] file, String filePath, String fileName) throws Exception {
		File targetFile = new File(filePath);
		if(!targetFile.exists()) {
			targetFile.mkdirs();
		}
		
		FileOutputStream out = new FileOutputStream(filePath + fileName);
		out.write(file);
		out.flush();
		out.close();
		
		return filePath + fileName;
	}
	
	private  String createImage(String srcPath, String id) throws Exception {
		if(srcPath == null) {
			return null;
		}
		File path = null;
		try {
			path = new File(ResourceUtils.getURL("classpath:").getPath());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] decoderBytes = decoder.decodeBuffer(srcPath.split(",")[1]);
		
		String filePath = path.getAbsolutePath() + "static/images/dashboards/";
		String fileName = id + ".png";
		return uploadFile(decoderBytes,filePath, fileName);
	}*/

	public void updateDashboardById(String id, String db) throws Exception{
		JSONObject jso = JSONObject.fromObject(db);
		Dashboard dashboard = dashboardRepository.findByHash(id);
		if(jso.has("config")) {
			JSONObject configObject = jso.getJSONObject("config");
			Config config = (Config)JSONObject.toBean(configObject, Config.class);
			dashboard.setConfig(config);
		}
		if(jso.has("widget")) {
			JSONArray widget = jso.getJSONArray("widget");
			dashboard.setWidget(widget);
		}
		if(jso.has("imgData")) {
			//String path = ResourceUtils.getURL("classpath:").getPath();
			String imgUrl = Utils.createImage(jso.getString("imgData"),id);
	//		dashboard.setImgData(jso.getString("imgData"));
			dashboard.setImgUrl(imgUrl);
		}
		dashboardRepository.save(dashboard);
	}
	
	public Published getPublishedById(String id) {
		Published published = publishedRepository.findByHash(id);
		return published;
	}

	@Override
	public Object uploadImage(String id, MultipartFile file) throws IllegalStateException, IOException {
		String hash = null;
		if(id.isEmpty()) {
			hash = "anony";
		}else {
			hash = id;
		}
		
		String fileName = file.getOriginalFilename();
		String suffixName = fileName.substring(fileName.lastIndexOf("."));
		String hashName = Utils.hash(fileName)+suffixName;
		
		if(suffixName.equalsIgnoreCase(".jpg") || suffixName.equalsIgnoreCase(".jpeg") || suffixName.equalsIgnoreCase(".png")) {
			file.transferTo(new File(Utils.getRootPath() + Utils.getImagesPath() + "/" + id + "/img" + hashName));
		}
		return Utils.getImagesPath()+ "/" + id + "/img" + hashName;
	}

}