package com.ithxh.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.ithxh.xhh.constant.StaticConst;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;

public class TestDownload {

	private static String ACCESS_KEY = "9qX1KZ3TGwmTn8MRH3VW9EnCxlAMhTQmLsWWzjNY";
	private static String SECRET_KEY = "dusTmT5BHbHSDutTrwdxLHx67sHlLOhdlogRd5dk";

	public static void main(String args[]) {
		// 设置需要操作账号的AK和SK
		Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);

		// 实例化一个BucketManager对象
		BucketManager bucketManager = new BucketManager(auth);
		// 要上传的空间
		String bucket = "rldhxh";
		String key = "777.jpg";

		// 要fetch的url
		String url = "http://i.lesmao.cc/i/les/T/UGirls/196/196_001_rfn_h.jpg";

		try {
			// 调用fetch方法抓取文件
			DefaultPutRet fetch = bucketManager.fetch(url, bucket, key);
			System.out.println(fetch.key);
		} catch (QiniuException e) {
			// 捕获异常信息
			Response r = e.response;
			System.out.println(r.toString());
		}
	}
	
	@Test
	public void DownloadImgTask(){
		try {
			URL picUrl = new URL("http://7xs7s6.com1.z0.glb.clouddn.com/00000.jpg");
			URLConnection openConnection = picUrl.openConnection();
			InputStream is = openConnection.getInputStream();
			FileUtils.copyInputStreamToFile(is, new File(StaticConst.SYS_FILE_PATH+File.separatorChar+"00000.jpg"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
