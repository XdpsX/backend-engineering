package dev.backendengineering.cacheaside;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

	private final JdbcTemplate jdbc;

	public ProductRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<Product> findById(long id) {
		return jdbc.query("SELECT id, name FROM products WHERE id = ?",
				(rs, rowNum) -> new Product(rs.getLong("id"), rs.getString("name")), id)
			.stream()
			.findFirst();
	}

	public Product save(Product product) {
		jdbc.update("MERGE INTO products (id, name) KEY (id) VALUES (?, ?)",
				product.id(), product.name());
		return product;
	}
}
