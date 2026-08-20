package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.insurance.InsuranceProductService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.InsuranceProductRequest;
import com.aegisterra.platform.application.contracts.InsuranceProductResponse;
import com.aegisterra.platform.application.contracts.PolicyTypeRequest;
import com.aegisterra.platform.application.contracts.PolicyTypeResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Insurance Products")
public class InsuranceProductController {

    private final InsuranceProductService productService;

    public InsuranceProductController(InsuranceProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/insurance-products")
    @PreAuthorize("hasAuthority('policies:read')")
    public List<InsuranceProductResponse> listProducts() {
        return productService.listProducts();
    }

    @GetMapping("/insurance-products/{id}")
    @PreAuthorize("hasAuthority('policies:read')")
    public InsuranceProductResponse getProduct(@PathVariable UUID id) {
        return productService.getProduct(id);
    }

    @PostMapping("/insurance-products")
    @PreAuthorize("hasAuthority('policies:write')")
    public ResponseEntity<InsuranceProductResponse> createProduct(
        @Valid @RequestBody InsuranceProductRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request, actor.id()));
    }

    @PutMapping("/insurance-products/{id}")
    @PreAuthorize("hasAuthority('policies:write')")
    public InsuranceProductResponse updateProduct(
        @PathVariable UUID id,
        @Valid @RequestBody InsuranceProductRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return productService.updateProduct(id, request, actor.id());
    }

    @GetMapping("/policy-types")
    @PreAuthorize("hasAuthority('policies:read')")
    public List<PolicyTypeResponse> listTypes() {
        return productService.listPolicyTypes();
    }

    @PostMapping("/policy-types")
    @PreAuthorize("hasAuthority('policies:write')")
    public ResponseEntity<PolicyTypeResponse> createType(
        @Valid @RequestBody PolicyTypeRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createPolicyType(request, actor.id()));
    }

    @PutMapping("/policy-types/{id}")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyTypeResponse updateType(
        @PathVariable UUID id,
        @Valid @RequestBody PolicyTypeRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return productService.updatePolicyType(id, request, actor.id());
    }
}
