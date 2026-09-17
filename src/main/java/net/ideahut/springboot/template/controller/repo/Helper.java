package net.ideahut.springboot.template.controller.repo;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import net.ideahut.springboot.entity.EntityInfo;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.helper.StringHelper;

class Helper {

	private Helper() {}
	
	static Sort getSort(String text) {
		String str = ObjectHelper.useOrDefault(text, "").trim();
		return ObjectHelper.callIf(
			!StringHelper.isEmpty(str), 
			() -> {
				String[] split = StringHelper.split(str, ",", false, true);
				List<Order> orders = new ArrayList<>();
				for (String order : split) {
					ObjectHelper.callOrElse(
						order.startsWith("-"), 
						() -> orders.add(Order.desc(order.substring(1))), 
						() -> orders.add(Order.asc(order))
					);
				}
				return Sort.by(orders);
			}
		);
	}
	
	static <T> void loadLazy(EntityInfo entityInfo, T object) {
		entityInfo.getTrxManagerInfo().transaction((Session session) -> {
			entityInfo.loadLazy(object);
			return object;
		});
	}
	
}
