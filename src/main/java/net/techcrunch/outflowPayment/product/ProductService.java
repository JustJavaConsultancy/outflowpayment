package net.techcrunch.outflowPayment.product;

import net.techcrunch.outflowPayment.account.AuthenticationManager;
import net.techcrunch.outflowPayment.util.NotFoundException;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


@Service("productService")
public class ProductService {

    private final ProductRepository productRepository;
    private final AuthenticationManager authenticationManager;

    public ProductService(final ProductRepository productRepository, AuthenticationManager authenticationManager) {
        this.productRepository = productRepository;
        this.authenticationManager = authenticationManager;
    }

    public List<ProductDTO> findAll() {
        final List<Product> products = productRepository.findAll(Sort.by("id"));
        return products.stream()
                .map(product -> mapToDTO(product, new ProductDTO()))
                .toList();
    }
    public List<ProductDTO> findAllByMerchantId(String merchantId) {
        final List<Product> products = productRepository.findByMerchantId(merchantId);
        return products.stream()
                .map(product -> mapToDTO(product, new ProductDTO()))
                .toList();
    }

    public ProductDTO get(final Long id) {
        return productRepository.findById(id)
                .map(product -> mapToDTO(product, new ProductDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ProductDTO productDTO) {
        final Product product = new Product();
        productDTO.setMerchantId((String) authenticationManager.get("sub"));
        mapToEntity(productDTO, product);
        return productRepository.save(product).getId();
    }

    public void update(final Long id, final ProductDTO productDTO) {
        final Product product = productRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(productDTO, product);
        productRepository.save(product);
    }
    public Long updateProduct(DelegateExecution execution) {
        BigDecimal amount= (BigDecimal) execution.getVariable("amount");

        final Product product = productRepository
                .findById(Long.parseLong(execution
                        .getVariable("productId").toString()))
                .orElseThrow(NotFoundException::new);
        Integer quantity=amount.divide(product.getPrice()).intValue();

        System.out.println(" Quantity Sold Actually=="+quantity);

        product.setQuantityInStock(product.getQuantityInStock()-quantity);
        Integer currentQuantity=product.getQuantitySold()!=null?product.getQuantitySold().intValue():0;
        product.setQuantitySold(currentQuantity+quantity);
        System.out.println(" Product Updated Here Variables==....."+execution.getVariables());
        return 1L;
    }
    public void delete(final Long id) {
        productRepository.deleteById(id);
    }

    private ProductDTO mapToDTO(final Product product, final ProductDTO productDTO) {
        productDTO.setId(product.getId());
        productDTO.setCode(product.getCode());
        productDTO.setName(product.getName());
        productDTO.setPrice(product.getPrice());
        productDTO.setDescription(product.getDescription());
        productDTO.setContainsPhysicalGoods(product.getContainsPhysicalGoods());
        productDTO.setQuantityInStock(product.getQuantityInStock());
        productDTO.setMedia(product.getMedia());
        productDTO.setDateCreated(product.getDateCreated());
        productDTO.setMerchantId(product.getMerchantId());
        return productDTO;
    }

    private Product mapToEntity(final ProductDTO productDTO, final Product product) {
        product.setCode(productDTO.getCode());
        product.setName(productDTO.getName());
        product.setPrice(productDTO.getPrice());
        product.setDescription(productDTO.getDescription());
        product.setContainsPhysicalGoods(productDTO.getContainsPhysicalGoods());
        product.setQuantityInStock(productDTO.getQuantityInStock());
        product.setMedia(productDTO.getMedia());
        product.setMerchantId(productDTO.getMerchantId());
        return product;
    }

    public boolean codeExists(final String code) {
        return productRepository.existsByCodeIgnoreCase(code);
    }

}
