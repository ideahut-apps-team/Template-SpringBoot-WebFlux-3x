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
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.helper.TimeHelper;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.mapper.DataMapper;
import net.ideahut.springboot.object.ListObject;
import net.ideahut.springboot.object.ListString;
import net.ideahut.springboot.object.Result;
import net.ideahut.springboot.task.TaskListExecutor;
import net.ideahut.springboot.template.app.AppProperties;
import net.ideahut.springboot.template.object.CacheData;
import reactor.core.publisher.Mono;

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
	public Mono<ArrayNode> groups() {
		return WebFluxHelper.fromCallable(() -> {
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
		});
	}
	
	@GetMapping("/get")
	public Mono<Result> get(
		@RequestParam(value = GROUP, required = false) String inGroup,
		@RequestParam(KEY) String key
	) {
		return WebFluxHelper.fromCallable(() -> {
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
		});
	}
	
	@GetMapping("/size")
	public Mono<Result> size(
		@RequestParam(value = GROUP, required = false) String inGroup
	) {
		return WebFluxHelper.fromCallable(() -> {
			String group = getCacheGroup(inGroup);
			Long size = cacheGroupHandler.size(group);
			return Result.success()
			.setInfo(GROUP, group)
			.setInfo("size", size);
		});
	}
	
	@GetMapping("/keys")
	public Mono<Result> keys(
		@RequestParam(value = GROUP, required = false) String inGroup,
		@RequestParam(value = "start", required = false) Long inStart,
		@RequestParam(value = "end", required = false) Long inEnd
	) {
		return WebFluxHelper.fromCallable(() -> {
			String group = getCacheGroup(inGroup);
			Long start = inStart != null && inStart > 0 ? inStart : 0L;
			Long end = inEnd != null && inEnd > 0 ? inEnd : cacheGroupHandler.size(group);
			List<String> keys = cacheGroupHandler.keys(group, start, end);
			return Result.success(keys)
			.setInfo(GROUP, group);
		});
	}
	
	@DeleteMapping("/delete")
	public Mono<Result> delete(
		@RequestParam(value = GROUP, required = false) String inGroup,
		@RequestParam(KEY) String key
	) {
		return WebFluxHelper.fromCallable(() -> {
			String group = getCacheGroup(inGroup);
			cacheGroupHandler.delete(group, key);
			return Result.success()
			.setInfo(GROUP, group)
			.setInfo(KEY, key);
		});
	}
	
	@DeleteMapping("/clear")
	public Mono<Result> clear(
		@RequestParam(value = GROUP, required = false) String inGroup
	) {
		return WebFluxHelper.fromCallable(() -> {
			String group = getCacheGroup(inGroup);
			cacheGroupHandler.clear(group);
			return Result.success()
			.setInfo(GROUP, group);
		});
	}
	
	@GetMapping("/any")
	public Mono<CacheData> any(
		@RequestParam(name = GROUP, required = false) String group, 
		@RequestParam(KEY) String key
	) {
		return WebFluxHelper.fromCallable(() -> {
			if (!StringHelper.isBlank(group)) {
				return cacheGroupHandler.get(CacheData.class, group, key, () -> {
					log.info("--- Group-Callable, group: {}, key: {}", group, key);
					return createCacheData(group, key);
				});
			} else {
				return cacheSingleHandler.get(CacheData.class, key, () -> {
					log.info("--- Single-Callable, key: {}", key);
					return createCacheData(group, key);
				});
			}
		});
	}
	
	@GetMapping("/list")
	public Mono<List<CacheData>> list(
		@RequestParam(name = GROUP, required = false) String group, 
		@RequestParam(KEY) ListString keys
	) {
		return WebFluxHelper.fromCallable(() ->  
			!StringHelper.isBlank(group) ?
			cacheGroupHandler.multiList(CacheData.class, group, keys) : 
			cacheSingleHandler.multiList(CacheData.class, keys)
		);
	}
	
	@GetMapping("/map")
	public Mono<Map<String, CacheData>> map(
		@RequestParam(name = GROUP, required = false) String group, 
		@RequestParam(KEY) ListString keys
	) {
		return WebFluxHelper.fromCallable(() -> 
			!StringHelper.isBlank(group) ?
			cacheGroupHandler.multiMap(CacheData.class, group, keys) : 
			cacheSingleHandler.multiMap(CacheData.class, keys)
		);
	}
	
	/*
	 * Simulasi jika permintaan data cache secara bersamaan dalam banyak request
	 * Diharapkan tidak semua request akan melakukan input data ke cache
	 */
	@GetMapping("/concurrent")
	public Mono<ListObject> concurrent(
		@RequestParam(name = GROUP, required = false) String group,
		@RequestParam(name = "concurrency", required = false) Integer concurrency,
		@RequestParam(name = "disableVirtualThread", required = false) Boolean disableVirtualThread
	) {
		return WebFluxHelper.fromCallable(() -> {
			String key = UUID.randomUUID().toString();
			long sleep = 4; // detik
			int threads = concurrency != null && concurrency > 0 ? concurrency : 100;
			TaskListExecutor executor = TaskListExecutor.of(Boolean.TRUE.equals(disableVirtualThread), threads, true);
			for (int i = 0; i < threads; i++) {
				int fi = i;
				executor.add(() -> getCacheData(fi, group, key, sleep));
			}
			return executor.getObjects();
		});
	}
	
	
	private String getCacheGroup(String inGroup) {
		return !StringHelper.isBlank(inGroup) ? inGroup : DEFAULT_GROUP;
	}
	
	private CacheData getCacheData(int index, String group, String key, long sleep) {
		if (!StringHelper.isBlank(group)) {
			return cacheGroupHandler.get(CacheData.class, group, key, () -> {
				TimeUnit.SECONDS.sleep(sleep);
				log.info("--- Group-Concurrent, group: {}, key: {}, index: {}", group, key, index);
				return createCacheData(group, key);
			});
		} else {
			return cacheSingleHandler.get(CacheData.class, key, () -> {
				TimeUnit.SECONDS.sleep(sleep);
				log.info("--- Single-Concurrent: key: {}, index: {}", key, index);
				return createCacheData(group, key);
			});
		}
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
