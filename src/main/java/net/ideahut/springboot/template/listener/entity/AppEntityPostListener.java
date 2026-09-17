package net.ideahut.springboot.template.listener.entity;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import net.ideahut.springboot.api.ApiHandler;
import net.ideahut.springboot.audit.AuditHandler;
import net.ideahut.springboot.bean.BeanConfigure;
import net.ideahut.springboot.entity.EntityPostListener;
import net.ideahut.springboot.module.ModuleApi;
import net.ideahut.springboot.module.ModuleSysParam;
import net.ideahut.springboot.sysparam.SysParamHandler;
import net.ideahut.springboot.task.TaskHandler;
import net.ideahut.springboot.template.app.AppConstant;

@Component
@ComponentScan
class AppEntityPostListener implements EntityPostListener, BeanConfigure {
	
	private final AuditHandler auditHandler;
	private final TaskHandler taskHandler;
	private final ApiHandler apiHandler;
	private final SysParamHandler sysParamHandler;
	
	private Map<Class<?>, EntityPostListener> listeners = new HashMap<>();
	
	@Autowired
	AppEntityPostListener(
		AuditHandler auditHandler,
		@Qualifier(AppConstant.Bean.Task.AUDIT)
		TaskHandler taskHandler,
		ApiHandler apiHandler,
		SysParamHandler sysParamHandler
	) {
		this.auditHandler = auditHandler;
		this.taskHandler = taskHandler;
		this.apiHandler = apiHandler;
		this.sysParamHandler = sysParamHandler;
	}
	
	@Override
	public void onConfigureBean(ApplicationContext applicationContext) throws Exception {
		listeners.clear();
		
		// SysParam
		listeners.putAll(ModuleSysParam.getEntityPostListeners(sysParamHandler));
		
		// Api
		listeners.putAll(ModuleApi.getEntityPostListeners(apiHandler));
	}

	@Override
	public void onPostInsert(Object entity) {
		auditHandler.save("INSERT", entity);
		taskHandler.execute(() -> {
			EntityPostListener listener = listeners.get(entity.getClass());
			if (listener != null) {
				listener.onPostInsert(entity);
			}
		});
	}

	@Override
	public void onPostUpdate(Object entity) {
		auditHandler.save("UPDATE", entity);
		taskHandler.execute(() -> {
			EntityPostListener listener = listeners.get(entity.getClass());
			if (listener != null) {
				listener.onPostUpdate(entity);
			}
		});
	}
	
	@Override
	public void onPostDelete(Object entity) {
		auditHandler.save("DELETE", entity);
		taskHandler.execute(() -> {
			EntityPostListener listener = listeners.get(entity.getClass());
			if (listener != null) {
				listener.onPostDelete(entity);
			}
		});
	}
	
}
