package dev.backendengineering.cacheaside;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

	private final ProductService service;

	public ProductController(ProductService service) {
		this.service = service;
	}

	@GetMapping("/{id}")
	public Product get(@PathVariable long id) {
		return service.findById(id);
	}

	@PutMapping("/{id}")
	public Product put(@PathVariable long id, @RequestBody UpdateProductRequest request) {
		return service.save(new Product(id, request.name()));
	}

	public record UpdateProductRequest(String name) {
	}
}
