package net.ideahut.springboot.template.controller.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
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

import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.bean.BeanConfigure;
import net.ideahut.springboot.entity.EntityInfo;
import net.ideahut.springboot.entity.EntityTrxManager;
import net.ideahut.springboot.entity.TrxManagerInfo;
import net.ideahut.springboot.object.Result;
import net.ideahut.springboot.template.entity.EmbeddedHardDel;
import net.ideahut.springboot.template.entity.EmbededId;
import net.ideahut.springboot.template.repo.EmbeddedHardDelRepo;

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
	Result page(
		@PathVariable("index") Integer index, 
		@PathVariable("size") Integer size,
		@RequestParam(value = "orders", required = false) String orders 
	) {
		Sort sort = Helper.getSort(orders);
		Pageable pageable = PageRequest.of(index - 1, size, sort);
		Page<EmbeddedHardDel> page = repo.findAll(pageable);
		Helper.loadLazy(entityInfo, page);
		return Result.success(page);
	}
	
	@GetMapping(value = "/id")
	Result byId(
		@ModelAttribute EmbededId id
	) {
		EmbeddedHardDel entity = repo.findById(id).orElse(null);
		Helper.loadLazy(entityInfo, entity);
		return Result.success(entity);
	}
	
	@PostMapping
	Result create(
		@RequestBody EmbeddedHardDel data
	) {
		EmbeddedHardDel entity = repo.save(data);
		return Result.success(entity);
	}
	
	@PutMapping(value = "")
	Result update(
		@ModelAttribute EmbededId id,
		@RequestBody EmbeddedHardDel data
	) {
		EmbeddedHardDel entity = repo.findById(id).orElse(null);
		Assert.notNull(entity, "Entity is not found");
		if (entityInfo.merge(data, entity, true, null)) {
			entity = repo.save(entity);
			Helper.loadLazy(entityInfo, entity);
		}
		return Result.success(entity);
	}
	
	@DeleteMapping(value = "")
	Result delete(
		@ModelAttribute EmbededId id
	) {
		repo.deleteById(id);
		return Result.success(id);
	}
	
}
