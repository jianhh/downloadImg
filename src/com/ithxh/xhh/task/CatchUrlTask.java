package com.ithxh.xhh.task;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.ithxh.xhh.service.impl.LSMAdaptor;

public class CatchUrlTask extends QuartzJobBean{
	

	@Override
	protected void executeInternal(JobExecutionContext context)
			throws JobExecutionException {
		
		String type = (String) context.getJobDetail().getJobDataMap().get("type");
		System.out.println("任务开始：扒取网页地址-->"+type);
		try {
			SchedulerContext schCtx = context.getScheduler().getContext();
			//获取Spring中的上下文    
			ApplicationContext appCtx = (ApplicationContext)schCtx.get("applicationContext");
			LSMAdaptor lsmAdaptor = (LSMAdaptor) appCtx.getBean("lsmAdaptor");
			if (lsmAdaptor==null) {
				System.out.println("我真的操你妹");
			}else {
				lsmAdaptor.parseForum();
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
		}  
		
	}
}
