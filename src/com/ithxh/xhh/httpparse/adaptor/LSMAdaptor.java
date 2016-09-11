package com.ithxh.xhh.httpparse.adaptor;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.ithxh.baseCore.baseUtils.DateUtils;
import com.ithxh.baseCore.baseUtils.IDGenerator;
import com.ithxh.baseCore.baseUtils.ListUtil;
import com.ithxh.baseCore.baseUtils.StringUtil;
import com.ithxh.baseCore.model.Pager;
import com.ithxh.xhh.constant.StaticConst;
import com.ithxh.xhh.constant.StaticConst.AndOrOr;
import com.ithxh.xhh.dao.JobDao;
import com.ithxh.xhh.dao.ProjectDao;
import com.ithxh.xhh.dao.UploadFileDao;
import com.ithxh.xhh.dao.htmlparser.BaseParser;
import com.ithxh.xhh.entity.Job;
import com.ithxh.xhh.entity.Project;
import com.ithxh.xhh.entity.UploadFile;
import com.ithxh.xhh.service.UploadFileService;
import com.ithxh.xhh.tools.SysConfigUtil;
import com.ithxh.xhh.vo.formbean.UploadFileVo;

/**
 * 蕾丝猫适配器
 * 
 * @description
 * @author hxh
 * @date 2016年8月16日 上午12:42:51
 */
public class LSMAdaptor extends BaseParser{

	@Autowired
	JobDao jobDao;

	@Autowired
	ThreadPoolTaskExecutor poolTaskExecutor;
	
	@Autowired
	ProjectDao projectDao;
	
	@Autowired
	UploadFileService uploadFileService;
	
	@Autowired
	UploadFileDao uploadFileDao;

	private static final String websiteUrl = "http://www.lesmao.com";

	private static final String forum = "#hd a[href]";

	private static final String url = "#show-forum .title a[href]";

	private static final String imgurl = "#ls-content-pic-post img[src]";

	/**
	 * 扒取版块
	 * 
	 * @Description:
	 * @author: 何建辉
	 * @date 2016年8月16日 上午1:09:41
	 * @param
	 * @throws IOException
	 * @throws SQLException
	 */
	public void parseForum() throws IOException, SQLException {
		// 判断版块是否为空
		List<Job> jobs = jobDao.findByFields(
				new String[] { "website", "type" }, new String[] { websiteUrl,
						"img" }, AndOrOr.AND);
		List<String> allForum = new ArrayList<String>();
		String forumstr = "";
		if (ListUtil.isEmpty(jobs)
				|| StringUtil.isEmpty(jobs.get(0).getForum())) {
			// 开始扒取版块
			Document doc = Jsoup.connect(websiteUrl).get();
			Elements forumElements = doc.select(forum);

			for (Element forumElement : forumElements) {
				String forumhref = forumElement.attr("href");
				if (forumhref.indexOf(websiteUrl + "/forum") > 0) {
					allForum.add(forumhref);
					forumstr = forumstr + "," + forumhref;
				}
			}
			if (ListUtil.isEmpty(jobs)) {
				// 存到数据库
				Job job = new Job();
				job.setAdaptor(LSMAdaptor.class.getName());
				job.setCreatetime(DateUtils.getNowDateTime());
				job.setForum(forumstr);
				job.setId(IDGenerator.uuidGenerate());
				job.setStatus("0");
				job.setWebsite(websiteUrl);
				job.setType("img");
				jobDao.insert(job);
			} else {
				Job job = jobs.get(0);
				job.setForum(forumstr);
				job.setStatus("0");
				job.setAdaptor(LSMAdaptor.class.getName());
				job.setUpdatetime(DateUtils.getNowDateTime());
				jobDao.updateTable(job);
			}
			jobs = jobDao.findByFields(new String[] { "website", "type" },
					new String[] { websiteUrl, "img" }, AndOrOr.AND);
		}
		// 扒取版块中的所有item地址
		parseItem(jobs);
	}

	/**
	 * 扒取版块下所有item
	 * 
	 * @Description:
	 * @author: 何建辉
	 * @date 2016年8月16日 上午1:46:51
	 * @param @param list
	 * @throws SQLException 
	 */
	private void parseItem(List<Job> jobs) throws SQLException {
		for (Job job : jobs) {
			String forums = job.getForum();
			String[] forumarr = null;
			if (forums.indexOf(",") > 0) {
				forumarr = forums.split(",");
			} else {
				forumarr = new String[] { forums };
			}
			job.setStatus("1");
			job.setUpdatetime(DateUtils.getNowDateTime());
			jobDao.updateTable(job);
			// 每个版块开一个线程
			for (final String forumUrl : forumarr) {
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						// 获取分页数字
						int start = forumUrl.lastIndexOf("-");
						int end = forumUrl.lastIndexOf(".");
						if (start > 0 && end > 0) {
							// 可以翻页
							String page = forumUrl.substring(start + 1, end);
							int pagenum = Integer.valueOf(page);
							
							try {
								for (int i = pagenum; i < 200; i++) {
									String newurl = forumUrl.substring(0, start+1)+i+forumUrl.substring(end);
									Document doc = Jsoup.connect(newurl).get();
									Elements items = doc.select(url);
									for (Element e : items) {
										String itemurl = e.attr("href");
										String title = e.val();
										Project project = projectDao.findByFieldName("url", itemurl);
										if(project!=null){
											break;
										}
										//存到数据库
										project = new Project();
										project.setCreatetime(DateUtils.getNowDateTime());
										project.setId(IDGenerator.uuidGenerate());
										project.setForum(newurl);
										project.setIsopen(true);
										project.setStatus("0");
										project.setTitle(title);
										project.setWebsite(websiteUrl);
										project.setUrl(itemurl);
										projectDao.insert(project);
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}

						} else {
							// 不可以翻页，没空去搞
						}
					}
				});
				
				poolTaskExecutor.execute(thread);
			}
			
			while(poolTaskExecutor.getActiveCount()>0){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			//都执行完毕，更新当前任务状态为已完成 ：2 ，没办法只能一个任务一个任务地跑
			job.setStatus("2");
			job.setUpdatetime(DateUtils.getNowDateTime());
			jobDao.updateTable(job);
		}
	}

	/**
	 * 扒取图片
	 * 
	 * @Description:
	 * @author: 何建辉
	 * @date 2016年8月16日 上午1:10:38
	 * @param
	 * @throws SQLException 
	 */
	public void parseImg() throws SQLException {
		Pager pager = new Pager();
		pager.addCondition("website","=", websiteUrl, AndOrOr.AND);
		pager.addCondition("isopen", "=", "1", AndOrOr.AND);
		pager.addCondition("status", "<", "1", AndOrOr.AND);
		pager.setOrderBy(" createtime desc ");
		pager.setPageSize(10);
		do {
			pager = projectDao.findForPager(pager, AndOrOr.AND);
			final List<?> list = pager.getList();
			for (int i = 0; i < list.size(); i++) {
				Project pro = (Project) list.get(i);
				pro.setStatus("1");
				projectDao.updateTable(pro);
			}
			if(!ListUtil.isEmpty(list)){
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
							try {
								for (int i = 0; i < list.size(); i++) {
									Project pro = (Project) list.get(i);
									Document doc = Jsoup.connect(pro.getUrl()).get();
									Elements imgElements = doc.select(imgurl);
									for (Element imge : imgElements) {
										String picpath = imge.attr("src");
										UploadFileVo uploadfile = new UploadFileVo();
										uploadfile.setUploadfileoldpath(picpath);
										//构造文件实体
										uploadfile.setUploadid(IDGenerator.uuidGenerate());
										//获取原文件名
										int index = picpath.lastIndexOf("/");
										String oldname = picpath.substring(index+1);
										uploadfile.setUploadoldfilename(oldname);
										//获取文件后缀
										String ext = picpath.substring(picpath.lastIndexOf(".")+1);
										uploadfile.setUploadfileext(ext);
										//构造保存路径
										uploadfile.setUploadnewfilename(IDGenerator.uuidGenerate()+"."+ext);
										uploadfile.setUploadfiletype("img");
										uploadfile.setUploadtime(DateUtils.getNowDateTime());
										uploadfile.setUploadstatus("0");
										uploadfile.setStatus(0);
										uploadfile.setProjectid(pro.getId());
										//根据配置文件，决定保存到哪个文件服务器。
										if(SysConfigUtil.getSysConfig().isOpenQiniuUpload()){
											uploadfile.setUploaddest("qiniuyun");
							    		}else{
							    			uploadfile.setUploaddest("localhost");
							    			String savepath = StaticConst.JD_BOOKPIC_PATH;
							    			uploadfile.setUploadfilepath(savepath+"/"+pro.getTitle()+"/"+uploadfile.getUploadnewfilename());
							    		}
										uploadFileService.add(uploadfile);
									}
									pro.setStatus("2");
									projectDao.updateTable(pro);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						
					}
				});
				
				poolTaskExecutor.execute(thread);
				
			}
		} while (pager.getTotalCount()>0);
		
		
	}
	
	
	public void downloadImg() throws SQLException{
		Pager pager = new Pager();
		pager.setPageSize(300);
		pager.addCondition("status", "<", "2", AndOrOr.AND);
		pager.addCondition("uploadtime", "<", DateUtils.getDateTimeToString(DateUtils.getDate(System.currentTimeMillis()-1000*60*60*5)), AndOrOr.AND);
		pager.setOrderBy(" uploadtime desc ");
		
		pager = uploadFileDao.findForPager(pager, AndOrOr.AND);
		
		do {
			final List<?> list = pager.getList();
			for (int i = 0; i < list.size(); i++) {
				UploadFile obj = (UploadFile) list.get(i);
				obj.setStatus(1);
				uploadFileDao.updateTable(obj);
			}
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						for (int i = 0; i < list.size(); i++) {
							UploadFile obj = (UploadFile) list.get(i);
							UploadFileVo vo = parseImg(obj.getUploadoldfilename());
							if ("qiniuyun".equals(obj.getUploaddest())) {
								obj.setUploadfilepath(vo.getUploadfilepath());
							}
							obj.setStatus(2);
							uploadFileDao.updateTable(obj);
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
			poolTaskExecutor.execute(thread);
		} while (pager.getTotalCount()>0);
	}
}
