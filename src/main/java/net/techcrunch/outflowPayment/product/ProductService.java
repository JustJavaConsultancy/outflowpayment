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

    public void delete(final Long id) {
        productRepository.deleteById(id);
    }

    private ProductDTO mapToDTO(final Product product, final ProductDTO productDTO) {
        productDTO.setId(product.getId());
        productDTO.setCode(product.getCode());
        productDTO.setName(product.getName());
        productDTO.setAmount(product.getAmount());
        productDTO.setDescription(product.getDescription());
        productDTO.setContainsPhysicalGoods(product.getContainsPhysicalGoods());
        productDTO.setQuantityInStock(product.getQuantityInStock());
        productDTO.setUnlimited(product.isUnlimited());
        productDTO.setQuantitySold(product.getQuantitySold() != null ? product.getQuantitySold() : 0);
        productDTO.setMedia(product.getMedia());
        productDTO.setDateCreated(product.getDateCreated());
        productDTO.setSubscribe(product.getSubscribe());
        productDTO.setMerchantId(product.getMerchantId());
        productDTO.setAccountId(product.getAccountId());
        productDTO.setRevenueCode(product.getRevenueCode());
        productDTO.setCategory(product.getCategory());
        return productDTO;
    }

    private Product mapToEntity(final ProductDTO productDTO, final Product product) {
        product.setCode(productDTO.getCode());
        product.setName(productDTO.getName());
        product.setAmount(productDTO.getAmount());
        product.setDescription(productDTO.getDescription());
        product.setContainsPhysicalGoods(productDTO.getContainsPhysicalGoods());
        product.setQuantityInStock(productDTO.getQuantityInStock());
        product.setUnlimited(productDTO.isUnlimited());
        product.setMedia(productDTO.getMedia());
        product.setQuantitySold(productDTO.getQuantitySold() != null ? productDTO.getQuantitySold().intValue() : 0);
        product.setSubscribe(productDTO.getSubscribe());
        product.setMerchantId(productDTO.getMerchantId());
        product.setAccountId(productDTO.getAccountId());
        product.setRevenueCode(productDTO.getRevenueCode());
        product.setCategory(productDTO.getCategory());
        return product;
    }


    public boolean codeExists(final String code) {
        return productRepository.existsByCodeIgnoreCase(code);
    }

}
