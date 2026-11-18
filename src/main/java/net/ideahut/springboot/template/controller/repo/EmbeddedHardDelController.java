package net.ideahut.springboot.template.controller.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Min;
import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.bean.BeanConfigure;
import net.ideahut.springboot.entity.EntityInfo;
import net.ideahut.springboot.entity.EntityTrxManager;
import net.ideahut.springboot.entity.TrxManagerInfo;
import net.ideahut.springboot.helper.ErrorHelper;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.object.Result;
import net.ideahut.springboot.template.entity.EmbeddedHardDel;
import net.ideahut.springboot.template.entity.EmbededId;
import net.ideahut.springboot.template.repo.EmbeddedHardDelRepo;
import reactor.core.publisher.Mono;

@Public
@ComponentScan
@RestController
@RequestMapping("/repo/EmbeddedHardDel")
class EmbeddedHardDelController implements BeanConfigure {
	
	private final EmbeddedHardDelRepo repo;
	private EntityInfo entityInfo;
	
	@Autowired
	EmbeddedHardDelController(
		EmbeddedHardDelRepo repo
	) {
		this.repo = repo;
	}
	
	@Override
	public void onConfigureBean(ApplicationContext applicationContext) throws Exception {
		TrxManagerInfo trxManagerInfo = applicationContext.getBean(EntityTrxManager.class).getDefaultTrxManagerInfo();
		entityInfo = trxManagerInfo.getEntityInfo(EmbeddedHardDel.class);
	}
	
	@GetMapping(value = "/{index}/{size}")
	public Mono<Result> page(
		@PathVariable("index") @Min(1) Integer index, 
		@PathVariable("size") @Min(1) Integer size,
		@RequestParam(value = "orders", required = false) String orders 
	) {
		return WebFluxHelper.fromCallable(() -> {
			Sort sort = Helper.getSort(orders);
			Pageable pageable = PageRequest.of(index - 1, size, sort);
			Page<EmbeddedHardDel> page = repo.findAll(pageable);
			Helper.loadLazy(entityInfo, page);
			return Result.success(page);
		});
	}
	
	@GetMapping(value = "/id")
	public Mono<Result> byId(
		@ModelAttribute EmbededId id
	) {
		return WebFluxHelper.fromCallable(() -> {
			EmbeddedHardDel entity = repo.findById(id).orElse(null);
			Helper.loadLazy(entityInfo, entity);
			return Result.success(entity);
		});
	}
	
	@PostMapping
	public Mono<Result> create(
		@RequestBody EmbeddedHardDel data
	) {
		return WebFluxHelper.fromCallable(() -> {
			EmbeddedHardDel entity = repo.save(data);
			return Result.success(entity);
		});
	}
	
	@PutMapping
	public Mono<Result> update(
		@ModelAttribute EmbededId id,
		@RequestBody EmbeddedHardDel data
	) {
		return WebFluxHelper.fromCallable(() -> {
			EmbeddedHardDel entity = repo.findById(id).orElse(null);
			ErrorHelper.throwNull(entity, () -> "Entity not found");
			if (entityInfo.merge(data, entity, true, null)) {
				entity = repo.save(entity);
				Helper.loadLazy(entityInfo, entity);
			}
			return Result.success(entity);
		});
	}
	
	@DeleteMapping
	public Mono<Result> delete(
		@ModelAttribute EmbededId id
	) {
		return WebFluxHelper.fromCallable(() -> {
			repo.deleteById(id);
			return Result.success(id);
		});
	}
	
}
