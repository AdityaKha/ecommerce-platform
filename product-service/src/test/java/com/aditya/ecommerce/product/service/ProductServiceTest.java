package com.aditya.ecommerce.product.service;

import com.aditya.ecommerce.product.domain.Product;
import com.aditya.ecommerce.product.dto.ProductRequest;
import com.aditya.ecommerce.product.dto.ProductResponse;
import com.aditya.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private Product existingProduct;

    @BeforeEach
    void setUp() {
        existingProduct = Product.builder()
                .id(1L)
                .name("Old Name")
                .description("Old description")
                .sku("SKU-OLD")
                .price(new BigDecimal("10.00"))
                .category("Old Category")
                .active(true)
                .build();
    }

    @Test
    void findAll_mapsAllEntitiesToResponses() {
        Product second = Product.builder()
                .id(2L)
                .name("Second")
                .description("Second description")
                .sku("SKU-2")
                .price(new BigDecimal("20.00"))
                .category("Category B")
                .active(false)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(existingProduct, second));

        List<ProductResponse> result = productService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(new ProductResponse(1L, "Old Name", "Old description", "SKU-OLD",
                new BigDecimal("10.00"), "Old Category", true));
        assertThat(result.get(1)).isEqualTo(new ProductResponse(2L, "Second", "Second description", "SKU-2",
                new BigDecimal("20.00"), "Category B", false));
    }

    @Test
    void findById_returnsResponse_whenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        ProductResponse response = productService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Old Name");
        assertThat(response.sku()).isEqualTo("SKU-OLD");
    }

    @Test
    void findById_throws_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void search_delegatesToRepositoryWithSpecification() {
        when(productRepository.findAll(any(Specification.class))).thenReturn(List.of(existingProduct));

        List<ProductResponse> result = productService.search("Old", "Old Category",
                new BigDecimal("5.00"), new BigDecimal("15.00"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Old Name");
        verify(productRepository).findAll(any(Specification.class));
    }

    @Test
    void create_buildsActiveProductAndSaves() {
        ProductRequest request = new ProductRequest("New Product", "New description", "SKU-NEW",
                new BigDecimal("42.50"), "New Category");

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product toSave = invocation.getArgument(0);
            toSave.setId(5L);
            return toSave;
        });

        ProductResponse response = productService.create(request);

        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();

        assertThat(saved.getName()).isEqualTo("New Product");
        assertThat(saved.getDescription()).isEqualTo("New description");
        assertThat(saved.getSku()).isEqualTo("SKU-NEW");
        assertThat(saved.getPrice()).isEqualTo(new BigDecimal("42.50"));
        assertThat(saved.getCategory()).isEqualTo("New Category");
        assertThat(saved.isActive()).isTrue();

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("New Product");
        assertThat(response.active()).isTrue();
    }

    @Test
    void update_mutatesAndSavesExistingProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductRequest request = new ProductRequest("Updated Name", "Updated description", "SKU-UPD",
                new BigDecimal("99.99"), "Updated Category");

        ProductResponse response = productService.update(1L, request);

        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getName()).isEqualTo("Updated Name");
        assertThat(saved.getDescription()).isEqualTo("Updated description");
        assertThat(saved.getSku()).isEqualTo("SKU-UPD");
        assertThat(saved.getPrice()).isEqualTo(new BigDecimal("99.99"));
        assertThat(saved.getCategory()).isEqualTo("Updated Category");

        assertThat(response.name()).isEqualTo("Updated Name");
    }

    @Test
    void update_throws_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest("X", "Y", "SKU-X", BigDecimal.ONE, "Cat");

        assertThatThrownBy(() -> productService.update(99L, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void delete_deletesFoundEntity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        productService.delete(1L);

        verify(productRepository, times(1)).delete(existingProduct);
    }

    @Test
    void delete_throws_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).delete(any(Product.class));
    }
}
