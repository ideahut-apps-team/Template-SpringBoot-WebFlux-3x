package net.ideahut.springboot.template.controller.test;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.crud.BulkList;
import net.ideahut.springboot.crud.BulkMap;
import net.ideahut.springboot.crud.BulkRequest;
import net.ideahut.springboot.crud.Condition;
import net.ideahut.springboot.crud.CrudAction;
import net.ideahut.springboot.crud.CrudHandler;
import net.ideahut.springboot.crud.CrudRequest;
import net.ideahut.springboot.crud.Filter;
import net.ideahut.springboot.crud.Join;
import net.ideahut.springboot.crud.Relation;
import net.ideahut.springboot.entity.EntityInfo;
import net.ideahut.springboot.entity.TrxManagerInfo;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.object.MapStringObject;
import net.ideahut.springboot.object.Page;
import net.ideahut.springboot.object.Result;
import net.ideahut.springboot.template.entity.AutoGenStrIdHardDel;
import net.ideahut.springboot.template.entity.CompositeHardDel;
import net.ideahut.springboot.template.entity.EmbeddedHardDel;
import net.ideahut.springboot.template.entity.EmbededId;
import net.ideahut.springboot.template.entity.LongIdJoinComposite;
import net.ideahut.springboot.template.entity.LongIdJoinEmbedded;

/*
 * Contoh API untuk CRUD bulk
 */
@Slf4j
@Public
@ComponentScan
@RestController
@RequestMapping("/test/bulk")
class BulkController {
	
	private final CrudHandler crudHandler;
	
	@Autowired
	BulkController(
		CrudHandler crudHandler
	) {
		this.crudHandler = crudHandler;
	}

	@GetMapping("/list")
	List<Result> list(
		@RequestParam(name = "oneSession", required = false) Boolean oneSession,
		@RequestParam(name = "stateless", required = false) Boolean stateless,
		@RequestParam(name = "useNative", required = false) Boolean useNative
	) {
		TrxManagerInfo trxManagerInfo = crudHandler.getEntityTrxManager().getDefaultTrxManagerInfo();
		EntityInfo eiCompositeHardDel = trxManagerInfo.getEntityInfo(CompositeHardDel.class);
		EntityInfo eiLongIdJoinComposite = trxManagerInfo.getEntityInfo(LongIdJoinComposite.class);
		EntityInfo eiAutoGenStrIdHardDel = trxManagerInfo.getEntityInfo(AutoGenStrIdHardDel.class);
		
		BulkList list = new BulkList()
		.setUseNative(useNative)
		.setStateless(!Boolean.FALSE.equals(stateless))
		.setTrxManagerInfo(ObjectHelper.callIf(Boolean.TRUE.equals(oneSession), () -> trxManagerInfo));
		
		CompositeHardDel compositeHardDel = new CompositeHardDel(1, "A");
		compositeHardDel.setName("COBA");
		compositeHardDel.setIsActive(ObjectHelper.TRUE);
		compositeHardDel.setDescription("description");
		
		list.addRequest(
			BulkRequest.list(eiCompositeHardDel)
			.setAction(CrudAction.SAVE)
			.addValue(CrudRequest.value(compositeHardDel))
		);
		
		LongIdJoinComposite longIdJoinComposite = new LongIdJoinComposite();
		longIdJoinComposite.setName("COBA");
		longIdJoinComposite.setIsActive(ObjectHelper.TRUE);
		longIdJoinComposite.setDescription("description");
		
		list.addRequest(
			BulkRequest.list(eiLongIdJoinComposite)
			.setAction(CrudAction.CREATE)
			.setAfter(0)
			.addValue(
				CrudRequest.value(longIdJoinComposite)
				.setValue("composite", new MapStringObject()
					.setValue("type", "{[0].type}")
					.setValue("code", "{[0].code}")
				)
			)
		);
		list.addRequest(
			BulkRequest.list(eiLongIdJoinComposite)
			.setAction(CrudAction.PAGE)
			.setAfter(1)
			.setPage(Page.of(1, 20, true))
			.addJoin(Join.of(eiCompositeHardDel, 
				Relation.of(null, "type").setValue("{[0].type}"), 
				Relation.of(null, "code").setValue("{[0].code}")
			).setStore("composite")
			)
			.addFilter(Filter.and("name", Condition.NOT_NULL))
			.addOrder("-createdOn")
		);
		
		list.addRequest(
			BulkRequest.list(eiAutoGenStrIdHardDel)
			.setAction(CrudAction.PAGE)
			.setAfter(1)
			.setPage(Page.of(1, 20, false))
			.addFilter(
				Filter.and("name", Condition.NOT_NULL)
				.addFilter(Filter.or("name", Condition.ANY_LIKE, "cobxx"))
				.addFilter(Filter.or("name", Condition.ANY_LIKE, "tesxx"))
			)
			.addFilter(Filter.and("date", Condition.BETWEEN, "2024-01-01 00:00:00", "2024-01-31 23:59:59"))
			.addOrder("-createdOn")
		);
		
		return crudHandler.bulk(list);
	}
	
	@GetMapping("/map")
	Map<String, Result> map(
		@RequestParam(name = "oneSession", required = false) Boolean oneSession,
		@RequestParam(name = "stateless", required = false) Boolean stateless,
		@RequestParam(name = "useNative", required = false) Boolean useNative
	) {
		TrxManagerInfo trxManagerInfo = crudHandler.getEntityTrxManager().getDefaultTrxManagerInfo();
		EntityInfo eiEmbeddedHardDel = trxManagerInfo.getEntityInfo(EmbeddedHardDel.class);
		EntityInfo eiLongIdJoinEmbedded = trxManagerInfo.getEntityInfo(LongIdJoinEmbedded.class);
		
		BulkMap map = new BulkMap()
		.setUseNative(useNative)
		.setStateless(!Boolean.FALSE.equals(stateless))
		.setTrxManagerInfo(ObjectHelper.callIf(Boolean.TRUE.equals(oneSession), () -> trxManagerInfo));
		
		EmbeddedHardDel embeddedHardDel = new EmbeddedHardDel(new EmbededId(1, "A"));
		embeddedHardDel.setName("COBA");
		embeddedHardDel.setIsActive(ObjectHelper.TRUE);
		embeddedHardDel.setDescription("description");
		
		map.putRequest(
			"EmbeddedHardDel-Save",
			BulkRequest.map(eiEmbeddedHardDel)
			.setAction(CrudAction.SAVE)
			.addValue(CrudRequest.value(embeddedHardDel))
		);
		
		
		LongIdJoinEmbedded longIdJoinEmbedded = new LongIdJoinEmbedded();
		longIdJoinEmbedded.setName("COBA");
		longIdJoinEmbedded.setIsActive(ObjectHelper.TRUE);
		longIdJoinEmbedded.setDescription("description");
		
		map.putRequest(
			"LongIdJoinEmbedded-Create",
			BulkRequest.map(eiLongIdJoinEmbedded)
			.setAction(CrudAction.CREATE)
			.setAfter("EmbeddedHardDel-Save")
			.addValue(
				CrudRequest.value(longIdJoinEmbedded)
				.setValue("embedded", new MapStringObject()
					.setValue("id", new MapStringObject()
						.setValue("type", "{[EmbeddedHardDel-Save].id.type}")
						.setValue("code", "{[EmbeddedHardDel-Save].id.code}")
					)
				)
			)
		);
		
		map.putRequest(
			"EmbeddedHardDel-Page",
			BulkRequest.map(eiEmbeddedHardDel)
			.setAction(CrudAction.PAGE)
			.setAfter("LongIdJoinEmbedded-Create")
			.setPage(Page.of(1, 5, true))
			.addFilter(Filter.or("name", Condition.NOT_NULL))
			.addOrder("-createdOn")
		);
		
		map.putRequest(
			"LongIdJoinEmbedded-Page",
			BulkRequest.map(eiLongIdJoinEmbedded)
			.setAction(CrudAction.PAGE)
			.setAfter("EmbeddedHardDel-Save")
			.setPage(Page.of(1, 5, true))
			.addJoin(Join.of(eiEmbeddedHardDel, 
				Relation.of("embedded.id", "id")
			))
			.addFilter(Filter.and("embedded.id.type", Condition.EQUAL, "{[EmbeddedHardDel-Save].id.type}"))
			.addFilter(Filter.and("embedded.id.code", Condition.EQUAL, "{[EmbeddedHardDel-Save].id.code}"))
			.addFilter(Filter.and("name", Condition.NOT_NULL))
			.addOrder("-createdOn")
		);
		
		return crudHandler.bulk(map);
	}
	
	@GetMapping("/select")
	List<Result> select(
		@RequestParam(name = "oneSession", required = false) Boolean oneSession,
		@RequestParam(name = "stateless", required = false) Boolean stateless,
		@RequestParam(name = "useNative", required = false) Boolean useNative
	) {
		TrxManagerInfo trxManagerInfo = crudHandler.getEntityTrxManager().getDefaultTrxManagerInfo();
		EntityInfo eiCompositeHardDel = trxManagerInfo.getEntityInfo(CompositeHardDel.class);
		EntityInfo eiLongIdJoinComposite = trxManagerInfo.getEntityInfo(LongIdJoinComposite.class);
		EntityInfo eiAutoGenStrIdHardDel = trxManagerInfo.getEntityInfo(AutoGenStrIdHardDel.class);
		
		Page page = Page.of(1, 100, true);
		
		BulkList list = new BulkList()
		.setUseNative(useNative)
		.setStateless(!Boolean.FALSE.equals(stateless))
		.setTrxManagerInfo(ObjectHelper.callIf(Boolean.TRUE.equals(oneSession), () -> trxManagerInfo))
		
		.addRequest(
			BulkRequest.list(eiCompositeHardDel)
			.setAction(CrudAction.PAGE)
			.setPage(page)
		)
		
		.addRequest(
			BulkRequest.list(eiLongIdJoinComposite)
			.setAction(CrudAction.MAP)
			.setLimit(100)
		)
		
		.addRequest(
			BulkRequest.list(eiAutoGenStrIdHardDel)
			.setAction(CrudAction.PAGE)
			.setPage(page)
		);
		
		return crudHandler.bulk(list);
	}
	
}
