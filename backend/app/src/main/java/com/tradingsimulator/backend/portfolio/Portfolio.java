package com.tradingsimulator.backend.portfolio;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "portfolio")
public class Portfolio extends PortfolioJpa {

	public static Portfolio named(Long userId, String name) {
		Portfolio portfolio = new Portfolio();
		portfolio.setUserId(userId);
		portfolio.setName(name.trim());
		portfolio.setArchived(false);
		return portfolio;
	}
}
