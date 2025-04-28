package net.techcrunch.outflowPayment.product;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductResource {

    private final ProductService productService;

    public ProductResource(final ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(productService.get(id));
    }

    @PostMapping
    //@ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createProduct(@RequestBody @Valid final ProductDTO productDTO) {
        System.out.println("Product name="+productDTO.getName());
        final Long createdId = productService.create(productDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateProduct(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final ProductDTO productDTO) {
        productService.update(id, productDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    //@ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteProduct(@PathVariable(name = "id") final Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
