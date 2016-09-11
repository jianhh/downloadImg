package com.ithxh.xhh.service.impl;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.ithxh.baseCore.baseUtils.DateUtils;
import com.ithxh.baseCore.baseUtils.IDGenerator;
import com.ithxh.baseCore.baseUtils.ListUtil;
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
@SuppressWarnings("serial")
@Service("lsmAdaptor")
public class LSMAdaptor extends BaseParser implements Serializable {

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

	private static final String WEBSITE = "http://www.lesmao.com";

	private static final String websiteUrl = "http://www.lesmao.com/forum.php?page=";
	private static final String websiteUrl1 = "http://www.lesmao.com/portal.php?page=";

	private static final String forum = "#hd a[href]";

	private static final String url = "#ul-pic a[href]";

	private static final String imgurl = "#pic-post img[src]";

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
	public void parseForum(){
		// 判断版块是否为空
		System.out.println(jobDao==null);
		try {
			List<Job> jobs = jobDao.findByFields(
					new String[] { "website", "type" }, new String[] { WEBSITE,
							"img" }, AndOrOr.AND);
			Job job = new Job();
			if (ListUtil.isEmpty(jobs)) {
				job.setForum(websiteUrl);
				job.setAdaptor("com.ithxh.xhh.service.impl.LSMAdaptor");
				job.setCreatetime(DateUtils.getNowDateTime());
				job.setId(IDGenerator.uuidGenerate());
				job.setType("img");
				job.setStatus("0");
				job.setWebsite(WEBSITE);
				jobDao.insert(job);
			} else {
				job = jobs.get(0);
				/*if(job.getStatus().equals("2")){
					return;
				}*/
			}
			// 扒取版块中的所有item地址
			// parseItem(jobs);
			parseItem(job);
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
	private void parseItem(Job job) throws SQLException {
		final String forumUrl = job.getForum();

		// 每一页
		for (int i = 1; i < 3; i++) {
			final StringBuilder sb = new StringBuilder();
			sb.append(forumUrl).append(i);
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					String pageUrl = sb.toString();
					try {
						System.out.println("正在扒取：========》"+pageUrl);
						Document doc = Jsoup.connect(pageUrl).get();
						Elements items = doc.select(url);
						int sum = 0;
						for (Element e : items) {
							String href = e.attr("href");
							String tid = href.substring(href.lastIndexOf("=") + 1);
							String itemurl = "thread-" + tid + "-1-1.html";
							String title = e.select("h2").text();
							System.out.println(title + "---" + (sum++)
									+ "---url=" + itemurl);
							Project project = projectDao.findByFieldName("url",
									itemurl);
							if (project != null) {
								break;
							}
							// 存到数据库
							project = new Project();
							project.setCreatetime(DateUtils.getNowDateTime());
							project.setId(IDGenerator.uuidGenerate());
							project.setForum(sb.toString());
							project.setIsopen(true);
							project.setStatus("0");
							project.setTitle(title);
							project.setWebsite(WEBSITE);
							project.setUrl(itemurl);
							projectDao.insert(project);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			poolTaskExecutor.execute(thread);
		}

		while (poolTaskExecutor.getActiveCount() > 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// 都执行完毕，更新当前任务状态为已完成 ：2 ，没办法只能一个任务一个任务地跑
		/*job.setStatus("2");
		job.setUpdatetime(DateUtils.getNowDateTime());
		jobDao.updateTable(job);*/
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
		Pager page = new Pager();
		page.addCondition("website", "=", WEBSITE, AndOrOr.AND);
		page.addCondition("isopen", "=", "1", AndOrOr.AND);
		page.addCondition("status", "<", "1", AndOrOr.AND);
		page.setOrderBy(" createtime desc ");
		page.setPageSize(10);
		int sum = 5;
		
		do {
			Pager pager = projectDao.findForPager(page, AndOrOr.AND);
			if(pager.getTotalCount()==0){
				System.out.println("扒取图片：没有任务可做，结束任务----------------");
				return;
			}
			final List<?> list = pager.getList();
			for (int i = 0; i < list.size(); i++) {
				Project pro = (Project) list.get(i);
				pro.setStatus("1");
				projectDao.updateTable(pro);
			}
			if (!ListUtil.isEmpty(list)) {
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						Project pro = null;
						try {
							for (int i = 0; i < list.size(); i++) {
								pro = (Project) list.get(i);
								for (int j = 1; j < 100; j++) {
									String url2 = pro.getUrl();
									String[] split = url2.split("-1-");
									url2 = split[0] + "-" + j + "-" + split[1];

									Document doc = Jsoup.connect(WEBSITE+"/"+url2).get();
									Elements imgElements = doc.select(imgurl);
									// 一张以上才正确
									if (imgElements.size() > 1) {
										for (Element imge : imgElements) {
											String picpath = imge.attr("src");
											UploadFileVo uf = uploadFileService.getByFieldName("uploadfileoldpath", picpath);
											picpath = "http://i.lesmao.cc/i/les/"+picpath.substring(32);
											if(uf!=null){
												continue;
											}else {
												uf = uploadFileService.getByFieldName("uploadfileoldpath", picpath);
												if (uf!=null) {
													continue;
												}
											}
											
											System.out.println("--------------------开始扒取图片:"+pro.getTitle()+"---->"+picpath);
											UploadFileVo uploadfile = new UploadFileVo();
											
											uploadfile.setUploadfileoldpath(picpath);
											// 构造文件实体
											uploadfile.setUploadid(IDGenerator.uuidGenerate());
											// 获取原文件名
											int index = picpath
													.lastIndexOf("/");
											String oldname = picpath
													.substring(index + 1);
											uploadfile
													.setUploadoldfilename(oldname);
											// 获取文件后缀
											String ext = picpath.substring(picpath
													.lastIndexOf(".") + 1);
											uploadfile.setUploadfileext(ext);
											// 构造保存路径
											uploadfile
													.setUploadnewfilename(IDGenerator
															.uuidGenerate()
															+ "." + ext);
											uploadfile.setUploadfiletype("img");
											uploadfile.setUploadtime(DateUtils
													.getNowDateTime());
											uploadfile.setUploadstatus("0");
											uploadfile.setStatus(0);
											uploadfile.setProjectid(pro.getId());
											// 根据配置文件，决定保存到哪个文件服务器。
											if (SysConfigUtil.getSysConfig()
													.isOpenQiniuUpload()) {
												uploadfile
														.setUploaddest("qiniuyun");
											} else {
												uploadfile
														.setUploaddest("local");
												String savepath = StaticConst.JD_BOOKPIC_PATH+DateUtils.getNowDate();
												uploadfile.setUploadfilepath(savepath
														+ "/"
														+ pro.getTitle()
														+ "/"
														+ uploadfile
																.getUploadoldfilename());
											}
											
											UploadFileVo uff = uploadFileService.getByFieldName("uploadfileoldpath", uploadfile.getUploadfileoldpath());
											if(uff==null){
												uploadFileService.add(uploadfile);
											}
										}
									} else {
										break;
									}

								}
								pro.setStatus("2");
								projectDao.updateTable(pro);
							}
						} catch (Exception e) {
							pro.setStatus("0");
							try {
								projectDao.updateTable(pro);
							} catch (SQLException e1) {
								e1.printStackTrace();
							}
						}
					}
				});
				poolTaskExecutor.execute(thread);
			}
			sum--;
		} while (sum > 0);

	}

	public void downloadImg() throws SQLException {
		Pager page = new Pager();
		page.setPageSize(100);
		page.addCondition("status", "=", "0", AndOrOr.AND);
		page.setOrderBy(" uploadtime desc ");

		
		int threadnum = 3;
		do {
			Pager pager = uploadFileDao.findForPager(page, AndOrOr.AND);
			System.out.println("total======"+pager.getTotalCount());
			if(pager.getTotalCount()==0){
				System.out.println(Thread.currentThread()+" 结束任务======等待下一次下载");
				return;
			}
			final List<?> list = pager.getList();
			for (int i = 0; i < list.size(); i++) {
				UploadFileVo obj = (UploadFileVo) list.get(i);
				uploadFileDao.updateTableOneField("img", "status", "1", obj.getUploadid());
			}
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					UploadFileVo obj = null;
					try {
						System.out.println("开启线程下载图片---------------");
						for (int i = 0; i < list.size(); i++) {
							obj = (UploadFileVo) list.get(i);
							boolean flag = fetch(obj);
							if (flag) {
								uploadFileDao.updateTableOneField("img", "status", "2", obj.getUploadid());
								DownloadImgTask(obj);
							}
						}
					} catch (SQLException e) {
						try {
							uploadFileDao.updateTableOneField("img", "status", "0", obj.getUploadid());
						} catch (SQLException e1) {
							e1.printStackTrace();
						}
						e.printStackTrace();
					}
				}
			});
			poolTaskExecutor.execute(thread);
			threadnum--;
		} while (threadnum > 0);
	}

	public static void main(String[] args) throws IOException {
		/*Document doc = Jsoup.connect(
				"http://www.lesmao.com/thread-14057-4-1.html").get();
		Elements elements = doc.select(imgurl);
		// 一张以上才正确
		if (elements.size() > 1) {
			for (Element element : elements) {
				System.out.println(element.attr("src"));
			}
		}*/
		/*Document document = Jsoup.connect(websiteUrl1).get();
		System.out.println(document.html());*/
		// System.out.println(doc.html());
		// Elements items = doc.select(url);
	}
}
