package net.ideahut.springboot.template.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import net.ideahut.springboot.task.TaskHandler;
import net.ideahut.springboot.task.TaskHandlerImpl;
import net.ideahut.springboot.template.app.AppConstant;
import net.ideahut.springboot.template.app.AppProperties;

/*
 * Konfigurasi TaskHandler
 * Untuk proses asynchronous
 */
@Configuration
class TaskConfig {
	
	@Primary
	@Bean(name = AppConstant.Bean.Task.PRIMARY)
    TaskHandler commonTask(
    	AppProperties appProperties		
    ) {
		return new TaskHandlerImpl()
		.setTaskProperties(appProperties.getTask().getPrimary());
    }
	
	@Bean(name = AppConstant.Bean.Task.AUDIT)
	TaskHandler auditTask(
		AppProperties appProperties		
	) {
		return new TaskHandlerImpl()
		.setTaskProperties(appProperties.getTask().getAudit());
    }
	
	@Bean(name = AppConstant.Bean.Task.REST)
	TaskHandler restTask(
		AppProperties appProperties		
	) {
		return new TaskHandlerImpl()
		.setTaskProperties(appProperties.getTask().getRest());
    }
	
	@Bean(name = AppConstant.Bean.Task.WEB_ASYNC)
	TaskHandler webAsyncTask(
		AppProperties appProperties		
	) {
		return new TaskHandlerImpl()
		.setTaskProperties(appProperties.getTask().getWebAsync());
    }
	
}
