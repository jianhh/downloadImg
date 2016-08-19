package com.ithxh.xhh.entity;

import java.io.Serializable;

import com.ithxh.baseCore.annotation.DB.PrimaryKey;
import com.ithxh.baseCore.annotation.DB.Table;

@Table("job")
public class Job implements Serializable{

	@PrimaryKey("id")
	private String id;
	private String website;
	private String forum;
	private String adaptor;
	private String updatetime;
	private String status;
	private String type;
	private String createtime;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getWebsite() {
		return website;
	}
	public void setWebsite(String website) {
		this.website = website;
	}
	public String getForum() {
		return forum;
	}
	public void setForum(String forum) {
		this.forum = forum;
	}
	public String getAdaptor() {
		return adaptor;
	}
	public void setAdaptor(String adaptor) {
		this.adaptor = adaptor;
	}
	public String getUpdatetime() {
		return updatetime;
	}
	public void setUpdatetime(String updatetime) {
		this.updatetime = updatetime;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getCreatetime() {
		return createtime;
	}
	public void setCreatetime(String createtime) {
		this.createtime = createtime;
	}
}
