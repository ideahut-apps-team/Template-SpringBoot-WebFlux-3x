package net.ideahut.springboot.template.controller.test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.crud.Condition;
import net.ideahut.springboot.crud.CrudAction;
import net.ideahut.springboot.crud.CrudHandler;
import net.ideahut.springboot.crud.CrudParam;
import net.ideahut.springboot.crud.CrudRequest;
import net.ideahut.springboot.crud.Filter;
import net.ideahut.springboot.entity.EntityHelper;
import net.ideahut.springboot.entity.EntityInfo;
import net.ideahut.springboot.entity.EntityNative;
import net.ideahut.springboot.entity.EntityTrxManager;
import net.ideahut.springboot.entity.TrxManagerInfo;
import net.ideahut.springboot.helper.FrameworkHelper;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.object.Page;
import net.ideahut.springboot.object.TimeValue;
import net.ideahut.springboot.template.entity.AutoGenStrIdHardDel;
import net.ideahut.springboot.template.entity.CompositeHardDel;
import net.ideahut.springboot.template.entity.EmbeddedHardDel;
import net.ideahut.springboot.template.entity.EmbeddedSoftDel;
import net.ideahut.springboot.template.entity.EmbededId;
import net.ideahut.springboot.template.entity.Information;

/*
 * Contoh API untuk EntityTrxManager
 */
@Slf4j
@Public
@ComponentScan
@RestController
@RequestMapping("/test/entity")
class EntityController {

	private final EntityTrxManager entityTrxManager;
	private final CrudHandler crudHandler;
	
	@Autowired
	EntityController(
		EntityTrxManager entityTrxManager,
		CrudHandler crudHandler
	) {
		this.entityTrxManager = entityTrxManager;
		this.crudHandler = crudHandler;
	}
	
	@GetMapping("/request")
	Map<String, Object> request() {
		EmbeddedSoftDel entity = new EmbeddedSoftDel(new EmbededId(1, "X"));
		entity.setName("NAME");
		entity.setDescription("DESCRIIPTION");
		entity.setIsActive(ObjectHelper.TRUE);
		
		Map<String, Object> map = new LinkedHashMap<>();
		
		EntityInfo info = entityTrxManager.getDefaultTrxManagerInfo().getEntityInfo(EmbeddedSoftDel.class);
		CrudRequest request = CrudRequest.of(info)
		.addValue(CrudRequest.value(entity));
		map.put("REQUEST", request);
		
		CrudParam crudParam = new CrudParam()
		.setAction(CrudAction.SAVE)
		.setPermission(crudHandler.getDefaultCrudPermission())
		.setRequest(request);
		
		Object hentity = crudHandler.execute(crudParam);
		map.put("ENTITY", hentity);
		
		request.getProperties().setUseNative(true);
		Object nentity = crudHandler.execute(crudParam);
		map.put("NATIVE", nentity);
		
		return map;
	}
	
	@GetMapping("/lock")
	Map<String, Object> lock() {
		TrxManagerInfo trxManagerInfo = entityTrxManager.getDefaultTrxManagerInfo();
		return trxManagerInfo.transaction((Session session) -> {
			Map<String, Object> result = new LinkedHashMap<>();
			
			AutoGenStrIdHardDel o1 = EntityHelper.get(
				session, 
				AutoGenStrIdHardDel.class, 
				"2026-205307-1505-00654-25200-0006", 
				LockMode.PESSIMISTIC_WRITE, 
				TimeValue.ofSeconds(5L)
			);
			result.put("STANDARD", o1);
			
			EmbeddedHardDel o2 = EntityNative.get(
				session, 
				trxManagerInfo.getEntityInfo(EmbeddedHardDel.class), 
				null, 
				new EmbededId(1, "A"), 
				LockMode.PESSIMISTIC_WRITE, 
				TimeValue.ofSeconds(5L)
			);
			result.put("EMBEDDED", o2);
			
			CompositeHardDel o3 = EntityNative.get(
				session, 
				trxManagerInfo.getEntityInfo(CompositeHardDel.class), 
				null, 
				new CompositeHardDel(1, "A"), 
				LockMode.PESSIMISTIC_WRITE, 
				TimeValue.ofSeconds(5L)
			);
			result.put("COMPOSITE", o3);
			
			Information o4 = EntityNative.get(
				session, 
				trxManagerInfo.getEntityInfo(Information.class), 
				2, 
				"INF2026-206023-0055-21951-25200-0001", 
				LockMode.PESSIMISTIC_WRITE, 
				TimeValue.ofSeconds(5L)
			);
			result.put("INFORMATION", o4);
			
			return result;
		});
	}
	
	@GetMapping("/update")
	Object update() {
		AutoGenStrIdHardDel entity = new AutoGenStrIdHardDel();
		entity.setId("2026-205167-1817-52019-25200-0001");
		entity.setName("cfcfcfcf (Edited)");
		entity.setCondition(Condition.BETWEEN);
		entity.setDate(LocalDateTime.now());
		entity.setIsActive(ObjectHelper.TRUE);
		return entityTrxManager.getDefaultTrxManagerInfo().transaction(true, (StatelessSession session) -> EntityHelper.update(session, entity));
	}
	
	@GetMapping("/AutoGenStrIdHardDel-Page")
	Page autoGenStrIdHardDelPage(
		@RequestParam(name = "useNative", required = false) Boolean useNative
	) {
		TrxManagerInfo trxManagerInfo = entityTrxManager.getDefaultTrxManagerInfo();
		EntityInfo eiAutoGenStrIdHardDel = trxManagerInfo.getEntityInfo(AutoGenStrIdHardDel.class);
		CrudRequest request = CrudRequest.of(eiAutoGenStrIdHardDel)
		.setPage(Page.of(1, 20, false))
		.addFilter(new Filter()
			.addFilter(Filter.or("name", Condition.ANY_LIKE, "cob"))
			.addFilter(Filter.or("name", Condition.ANY_LIKE, "tesxx"))
		)
		.addFilter(Filter.and("date", Condition.BETWEEN, "2024-01-01 00:00:00", "2024-01-31 23:59:59"))
		.addOrder("-createdOn");
		request.getProperties().setUseNative(useNative);
		log.info("\n" + FrameworkHelper.defaultDataMapper().writeAsString(request, 0, true));
		return crudHandler.execute(new CrudParam().setAction(CrudAction.PAGE).setRequest(request));
	}
	
}
