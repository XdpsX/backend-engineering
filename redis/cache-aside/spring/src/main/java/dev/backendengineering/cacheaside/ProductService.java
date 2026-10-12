package dev.backendengineering.cacheaside;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

	private static final Logger log = LoggerFactory.getLogger(ProductService.class);
	private static final Duration CACHE_TTL = Duration.ofSeconds(60);

	private final ProductRepository repository;
	private final StringRedisTemplate redis;

	public ProductService(ProductRepository repository, StringRedisTemplate redis) {
		this.repository = repository;
		this.redis = redis;
	}

	public Product findById(long id) {
		String key = cacheKey(id);
		String cachedName = redis.opsForValue().get(key);
		if (cachedName != null) {
			log.info("Cache HIT for {}", key);
			return new Product(id, cachedName);
		}

		log.info("Cache MISS for {}; loading from database", key);
		Product product = repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Product " + id + " was not found"));
		redis.opsForValue().set(key, product.name(), CACHE_TTL);
		return product;
	}

	public Product save(Product product) {
		Product saved = repository.save(product);
		redis.delete(cacheKey(saved.id()));
		return saved;
	}

	private String cacheKey(long id) {
		return "product:" + id;
	}
}
