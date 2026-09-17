package net.ideahut.springboot.template.controller.test;


import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.cache.CacheGroupHandler;
import net.ideahut.springboot.cache.CacheGroupProperties;
import net.ideahut.springboot.cache.CacheHandler;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.helper.TimeHelper;
import net.ideahut.springboot.mapper.DataMapper;
import net.ideahut.springboot.object.Result;
import net.ideahut.springboot.object.StringList;
import net.ideahut.springboot.task.TaskListExecutor;
import net.ideahut.springboot.template.app.AppProperties;
import net.ideahut.springboot.template.object.CacheData;

/*
 * Contoh penggunaan CacheHandler
 */
@Slf4j
@Public
@ComponentScan
@RestController
@RequestMapping("/test/cache")
class CacheController {
	
	private static final String GROUP = "group";
	private static final String KEY = "key";
	
	private static final String DEFAULT_GROUP = "GROUP-01";

	private final AppProperties appProperties;
	private final DataMapper dataMapper;
	private final CacheGroupHandler cacheGroupHandler;
	private final CacheHandler cacheSingleHandler;
	
	@Autowired
	CacheController(
		AppProperties appProperties,
		DataMapper dataMapper,
		CacheGroupHandler cacheGroupHandler,
		CacheHandler cacheSingleHandler
	) {
		this.appProperties = appProperties;
		this.dataMapper = dataMapper;
		this.cacheGroupHandler = cacheGroupHandler;
		this.cacheSingleHandler = cacheSingleHandler;
	}
	
	@GetMapping("/groups")
	ArrayNode groups() {
		ArrayNode items = dataMapper.createArrayNode();
		List<CacheGroupProperties> groups = appProperties.getCache().getGroups();
		for (CacheGroupProperties group : groups) {
			if (0 != group.getLimit()) {
				Long size = cacheGroupHandler.size(group.getName());
				ObjectNode item = items.addObject();
				item.put("name", group.getName());
				item.put("limit", group.getLimit());
				item.put("size", size);
			}
		}
		return items;
	}
	
	@GetMapping("/get")
	Result get(
		@RequestParam(value = GROUP, required = false) String inGroup,
		@RequestParam(KEY) String key
	) {
		String group = getCacheGroup(inGroup);
		Boolean[] cached = { Boolean.TRUE };
		CacheData data = cacheGroupHandler.get(
			CacheData.class, 
			group, 
			key, 
			() -> {
				cached[0] = Boolean.FALSE;
				return createCacheData(group, key);
			}
		);
		return Result.success(data)
		.setInfo(GROUP, group)
		.setInfo("cached", cached[0]);
	}
	
	@GetMapping("/size")
	Result size(
		@RequestParam(value = GROUP, required = false) String inGroup
	) {
		String group = getCacheGroup(inGroup);
		Long size = cacheGroupHandler.size(group);
		return Result.success()
		.setInfo(GROUP, group)
		.setInfo("size", size);
	}
	
	@GetMapping("/keys")
	Result keys(
		@RequestParam(value = GROUP, required = false) String inGroup
	) {
		String group = getCacheGroup(inGroup);
		List<String> keys = cacheGroupHandler.keys(group);
		return Result.success(keys)
		.setInfo(GROUP, group);
	}
	
	@DeleteMapping("/delete")
	Result delete(
		@RequestParam(value = GROUP, required = false) String inGroup,
		@RequestParam(KEY) String key
	) {
		String group = getCacheGroup(inGroup);
		cacheGroupHandler.delete(group, key);
		return Result.success()
		.setInfo(GROUP, group)
		.setInfo(KEY, key);
	}
	
	@DeleteMapping("/clear")
	Result clear(
		@RequestParam(value = GROUP, required = false) String inGroup
	) {
		String group = getCacheGroup(inGroup);
		cacheGroupHandler.clear(group);
		return Result.success()
		.setInfo(GROUP, group);
	}
	
	@GetMapping("/any")
	CacheData any(
		@RequestParam(name = GROUP, required = false) String group, 
		@RequestParam(KEY) String key
	) {
		return ObjectHelper.callOrElse(
			!StringHelper.isBlank(group),
			() -> cacheGroupHandler.get(CacheData.class, group, key, () -> {
				log.info("--- Group-Callable, group: {}, key: {}", group, key);
				return createCacheData(group, key);
			}), 
			() -> cacheSingleHandler.get(CacheData.class, key, () -> {
				log.info("--- Single-Callable, key: {}", key);
				return createCacheData(group, key);
			})
		);
	}
	
	@GetMapping("/list")
	List<CacheData> list(
		@RequestParam(name = GROUP, required = false) String group, 
		@RequestParam(KEY) StringList keys
	) {
		return ObjectHelper.callOrElse(
			!StringHelper.isBlank(group),
			() -> cacheGroupHandler.multiList(CacheData.class, group, keys), 
			() -> cacheSingleHandler.multiList(CacheData.class, keys)
		);
	}
	
	@GetMapping("/map")
	Map<String, CacheData> map(
		@RequestParam(name = GROUP, required = false) String group, 
		@RequestParam(KEY) StringList keys
	) {
		return ObjectHelper.callOrElse(
			!StringHelper.isBlank(group),
			() -> cacheGroupHandler.multiMap(CacheData.class, group, keys), 
			() -> cacheSingleHandler.multiMap(CacheData.class, keys)
		);
	}
	
	/*
	 * Simulasi jika permintaan data cache secara bersamaan dalam banyak request
	 * Diharapkan tidak semua request akan melakukan input data ke cache
	 */
	@GetMapping("/concurrent")
	List<Object> concurrent(
		@RequestParam(name = GROUP, required = false) String group,
		@RequestParam(name = "concurrency", required = false) Integer concurrency
	) {
		String key = UUID.randomUUID().toString();
		int threads = ObjectHelper.useOrElse(concurrency != null && concurrency > 0, concurrency, 100);
		TaskListExecutor executor = TaskListExecutor.of(threads);
		for (int i = 0; i < threads; i++) {
			int fi = i;
			executor.add(() -> getCacheData(fi, group, key));
		}
		return executor.getObjects();
	}
	
	
	private String getCacheGroup(String inGroup) {
		return ObjectHelper.callOrElse(!StringHelper.isBlank(inGroup), () -> inGroup, () -> DEFAULT_GROUP);
	}
	
	private CacheData getCacheData(int index, String group, String key) {
		return ObjectHelper.callOrElse(
			!StringHelper.isBlank(group),
			() -> cacheGroupHandler.get(CacheData.class, group, key, () -> {
				TimeUnit.SECONDS.sleep(2);
				log.info("--- Group-Concurrent, group: {}, key: {}, index: {}", group, key, index);
				return createCacheData(group, key);
			}), 
			() -> cacheSingleHandler.get(CacheData.class, key, () -> {
				TimeUnit.SECONDS.sleep(2);
				log.info("--- Single-Concurrent: key: {}, index: {}", key, index);
				return createCacheData(group, key);
			})
		);
	}
	
	private CacheData createCacheData(String group, String key) {
		CacheData data = new CacheData();
		data.setContent("Contoh cache - " + UUID.randomUUID());
		data.setGroup(group);
		data.setKey(key);
		data.setTimestamp(TimeHelper.currentEpochMillis());
		return data;
	}
	
}
