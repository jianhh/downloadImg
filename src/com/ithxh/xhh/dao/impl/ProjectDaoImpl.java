package com.ithxh.xhh.dao.impl;

import org.springframework.stereotype.Component;

import com.ithxh.baseCore.baseInterface.impl.BaseDaoImpl;
import com.ithxh.xhh.dao.ProjectDao;
import com.ithxh.xhh.entity.Project;

@Component("projectDao")
public class ProjectDaoImpl extends BaseDaoImpl<Project, Project> implements ProjectDao{

}
