package net.ideahut.springboot.template.job;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.quartz.JobExecutionContext;

import net.ideahut.springboot.helper.FrameworkHelper;
import net.ideahut.springboot.job.JobBase;

public class PageScrapperJob extends JobBase {

	@Override
	protected void run(JobExecutionContext context) throws Exception {
		String source = getConfigValue(context, String.class, "URL", "https://google.com");
		URL url = FrameworkHelper.getURL(source);
		byte[] bytes = IOUtils.toByteArray(url);
		String content = new String(bytes);
		logger().info(content);
	}

}
