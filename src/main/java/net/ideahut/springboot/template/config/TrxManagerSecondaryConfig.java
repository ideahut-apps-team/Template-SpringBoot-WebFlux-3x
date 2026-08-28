package net.ideahut.springboot.template.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import net.ideahut.springboot.helper.HibernateHelper;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.template.app.AppProperties;
import net.ideahut.springboot.template.app.AppProperties.TrxDatasource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories
(
	entityManagerFactoryRef = TrxManagerSecondaryConfig.PREFIX + "EntityManagerFactory",
	transactionManagerRef = TrxManagerSecondaryConfig.PREFIX + "TransactionManager"
)
class TrxManagerSecondaryConfig {
	
	static final String PREFIX = "secondary";
	
	@Bean(PREFIX + "EntityManagerFactory")
	EntityManagerFactory entityManagerFactory(
		AppProperties appProperties
	) throws Exception {
		AppProperties.TrxMain trxMain = appProperties.getTrxManager().getSecondary();
		AppProperties.TrxAudit trxAudit = ObjectHelper.useOrDefault(trxMain.getAudit(), AppProperties.TrxAudit::new);
		return HibernateHelper.createEntityManagerFactory(
			TrxDatasource.getDefinition(trxMain.getDatasource()), 
			trxMain, 
			trxAudit.getId(), 
			TrxDatasource.getDefinition(trxAudit.getDatasource()), 
			trxAudit
		);
	}

	@Bean(PREFIX + "TransactionManager")
	PlatformTransactionManager transactionManager(
		@Qualifier(PREFIX + "EntityManagerFactory")	
		EntityManagerFactory entityManagerFactory	
	) {
		return new JpaTransactionManager(entityManagerFactory);
	}
	
}
