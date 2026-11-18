package net.ideahut.springboot.template.controller.test;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.entity.EntityInfo;
import net.ideahut.springboot.entity.EntityReplica;
import net.ideahut.springboot.entity.EntityTrxManager;
import net.ideahut.springboot.entity.TrxManagerInfo;
import net.ideahut.springboot.entity.replica.ReplicaInfo;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.object.Result;
import net.ideahut.springboot.template.entity.Information;
import net.ideahut.springboot.template.entity.InformationLink;
import reactor.core.publisher.Mono;

/*
 * Contoh untuk EntityReplica
 */
@Public
@ComponentScan
@RestController
@RequestMapping("/test/replica")
class ReplicaController {
	
	private final EntityTrxManager entityTrxManager;
	
	@Autowired
	ReplicaController(
		EntityTrxManager entityTrxManager	
	) {
		this.entityTrxManager = entityTrxManager;
	}

	@PostMapping(value = "/create/information")
	public Mono<Result> createInformationReplica() {
		return WebFluxHelper.fromCallable(() -> {
			TrxManagerInfo trxManagerInfo = entityTrxManager.getDefaultTrxManagerInfo();
			EntityInfo entityInfo = trxManagerInfo.getEntityInfo(Information.class);
			List<EntityReplica.Creation> creations = EntityReplica.create(entityInfo, 2);
			return Result.success(creations);
		});
	}
	
	@GetMapping(value = "/sql/information")
	public Mono<Result> sqlInformationReplica() {
		return WebFluxHelper.fromCallable(() -> {
			TrxManagerInfo trxManagerInfo = entityTrxManager.getDefaultTrxManagerInfo();
			EntityInfo entityInfo = trxManagerInfo.getEntityInfo(Information.class);
			List<String> sqls = EntityReplica.getSQL(entityInfo, 2);
			return Result.success(sqls);
		});
	}
	
	@PostMapping(value = "/create/information/link")
	public Mono<Result> createInformationLinkReplica() {
		return WebFluxHelper.fromCallable(() -> {
			TrxManagerInfo trxManagerInfo = entityTrxManager.getDefaultTrxManagerInfo();
			EntityInfo entityInfo = trxManagerInfo.getEntityInfo(InformationLink.class);
			EntityInfo refEntityInfo = trxManagerInfo.getEntityInfo(Information.class);
			List<EntityReplica.Creation> creations = EntityReplica.create(entityInfo, 2, refEntityInfo);
			return Result.success(creations);
		});
	}
	
	@GetMapping(value = "/sql/information/link")
	public Mono<Result> sqlInformationLinkReplica() {
		return WebFluxHelper.fromCallable(() -> {
			TrxManagerInfo trxManagerInfo = entityTrxManager.getDefaultTrxManagerInfo();
			EntityInfo entityInfo = trxManagerInfo.getEntityInfo(InformationLink.class);
			EntityInfo refEntityInfo = trxManagerInfo.getEntityInfo(Information.class);
			List<String> sqls = EntityReplica.getSQL(entityInfo, 2, refEntityInfo);
			return Result.success(sqls);
		});
	}
	
	@GetMapping("/latest")
	public Mono<Integer> latest() {
		return WebFluxHelper.fromCallable(() -> ReplicaInfo.getLatestReplica(entityTrxManager.getDefaultTrxManagerInfo(), Information.class));
	}
	
}
