package com.ithxh.xhh.dao.impl;

import org.springframework.stereotype.Component;

import com.ithxh.baseCore.baseInterface.impl.BaseDaoImpl;
import com.ithxh.xhh.dao.JobDao;
import com.ithxh.xhh.entity.Job;

@Component("jobDao")
public class JobDaoImpl extends BaseDaoImpl<Job, Job> implements JobDao{

}
