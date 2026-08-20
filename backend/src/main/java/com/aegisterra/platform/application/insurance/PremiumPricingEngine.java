package com.aegisterra.platform.application.insurance;

import com.aegisterra.platform.application.insurance.pricing.PremiumPricingContext;
import com.aegisterra.platform.application.insurance.pricing.PremiumPricingResult;
import com.aegisterra.platform.application.insurance.pricing.PricingFactor;
import com.aegisterra.platform.application.insurance.pricing.PricingStrategy;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.agriculture.CropEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.CropRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmEntity;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.CoveragePackageEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.CoveragePackageRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsuranceProductEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsuranceProductRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.PremiumPricingRuleEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.PremiumPricingRuleRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.PremiumQuoteEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.PremiumQuoteRepository;
import com.aegisterra.platform.infrastructure.persistence.platform.ConfigurationRepository;
import com.aegisterra.platform.application.contracts.PremiumQuoteRequest;
import com.aegisterra.platform.application.contracts.PremiumQuoteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PremiumPricingEngine {

    private final InsuranceProductRepository productRepository;
    private final CoveragePackageRepository packageRepository;
    private final PremiumPricingRuleRepository ruleRepository;
    private final PremiumQuoteRepository quoteRepository;
    private final FarmerRepository farmerRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final ConfigurationRepository configurationRepository;
    private final List<PricingStrategy> strategies;
    private final List<PricingFactor> factors;
    private final ObjectMapper objectMapper;
    private final InsuranceAuditHelper auditHelper;

    public PremiumPricingEngine(
        InsuranceProductRepository productRepository,
        CoveragePackageRepository packageRepository,
        PremiumPricingRuleRepository ruleRepository,
        PremiumQuoteRepository quoteRepository,
        FarmerRepository farmerRepository,
        FarmRepository farmRepository,
        CropRepository cropRepository,
        ConfigurationRepository configurationRepository,
        List<PricingStrategy> strategies,
        List<PricingFactor> factors,
        ObjectMapper objectMapper,
        InsuranceAuditHelper auditHelper
    ) {
        this.productRepository = productRepository;
        this.packageRepository = packageRepository;
        this.ruleRepository = ruleRepository;
        this.quoteRepository = quoteRepository;
        this.farmerRepository = farmerRepository;
        this.farmRepository = farmRepository;
        this.cropRepository = cropRepository;
        this.configurationRepository = configurationRepository;
        this.strategies = strategies;
        this.factors = factors;
        this.objectMapper = objectMapper;
        this.auditHelper = auditHelper;
    }

    @Transactional
    public PremiumQuoteResponse quote(PremiumQuoteRequest request, UUID actorId) {
        InsuranceProductEntity product = productRepository.findByIdAndDeletedFalse(request.productId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));
        if (!"ACTIVE".equals(product.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Product is not active");
        }
        CoveragePackageEntity pkg = packageRepository.findByIdAndDeletedFalse(request.coveragePackageId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coverage package not found"));
        if (!pkg.getProductId().equals(product.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Package does not belong to product");
        }
        farmerRepository.findByIdAndDeletedFalse(request.farmerId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farmer not found"));
        FarmEntity farm = farmRepository.findByIdAndDeletedFalse(request.farmId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farm not found"));
        if (!farm.getFarmerId().equals(request.farmerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farm does not belong to farmer");
        }

        String cropCode = null;
        if (request.cropId() != null) {
            CropEntity crop = cropRepository.findById(request.cropId()).filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Crop not found"));
            cropCode = crop.getCode();
        }

        BigDecimal areaHa = request.areaHa() != null ? request.areaHa()
            : (farm.getFarmSizeHa() != null ? farm.getFarmSizeHa() : BigDecimal.ONE);
        BigDecimal sumInsuredPerHa = packageConfigDecimal(pkg.getConfigJson(), "sumInsuredPerHa", new BigDecimal("600000"));
        String currency = productConfigText(product.getConfigJson(), "currency", "RWF");
        String riskZone = request.riskZoneCode() == null || request.riskZoneCode().isBlank() ? "MEDIUM" : request.riskZoneCode();

        PremiumPricingContext context = new PremiumPricingContext(
            product.getId(),
            pkg.getId(),
            request.farmerId(),
            request.farmId(),
            request.cropId(),
            request.seasonId(),
            request.partnerId(),
            cropCode,
            riskZone,
            areaHa,
            pkg.getCoverageLevelPct(),
            sumInsuredPerHa,
            currency,
            request.weatherMultiplier() == null ? BigDecimal.ONE : request.weatherMultiplier()
        );

        Map<String, PricingStrategy> strategyMap = strategies.stream()
            .collect(Collectors.toMap(PricingStrategy::code, Function.identity(), (a, b) -> a));
        PricingStrategy strategy = strategyMap.get(product.getPricingStrategyCode());
        if (strategy == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Unknown pricing strategy: " + product.getPricingStrategyCode());
        }

        List<PremiumPricingRuleEntity> rules = ruleRepository
            .findByProductIdAndDeletedFalseAndStatusOrderByPriorityAsc(product.getId(), "ACTIVE");
        PremiumPricingResult result = strategy.price(context, rules, factors);

        PremiumQuoteEntity quote = new PremiumQuoteEntity();
        quote.setProductId(product.getId());
        quote.setCoveragePackageId(pkg.getId());
        quote.setFarmerId(request.farmerId());
        quote.setFarmId(request.farmId());
        quote.setCropId(request.cropId());
        quote.setSeasonId(request.seasonId());
        quote.setCurrency(result.currency());
        quote.setBaseAmount(result.baseAmount());
        quote.setGrossAmount(result.grossAmount());
        quote.setNetAmount(result.netAmount());
        quote.setCoverageAmount(result.coverageAmount());
        try {
            quote.setBreakdownJson(objectMapper.writeValueAsString(result.adjustments()));
        } catch (Exception ex) {
            quote.setBreakdownJson("[]");
        }
        quote.setFactorsHash(hash(quote.getBreakdownJson() + "|" + result.netAmount()));
        quote.setExpiresAt(Instant.now().plus(quoteTtlDays(), ChronoUnit.DAYS));
        quote.setStatus("ACTIVE");
        quote.setDeleted(false);
        quote.setCreatedBy(actorId);
        quoteRepository.save(quote);

        auditHelper.record(AuditAction.PREMIUM_QUOTED, actorId, "premium_quote", quote.getId(), null, toResponse(quote), null);
        return toResponse(quote);
    }

    @Transactional(readOnly = true)
    public PremiumQuoteResponse get(UUID id) {
        return toResponse(require(id));
    }

    public PremiumQuoteEntity requireValidQuote(UUID id) {
        PremiumQuoteEntity quote = require(id);
        if (quote.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Premium quote has expired");
        }
        return quote;
    }

    private PremiumQuoteEntity require(UUID id) {
        return quoteRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Premium quote not found"));
    }

    private PremiumQuoteResponse toResponse(PremiumQuoteEntity quote) {
        return new PremiumQuoteResponse(
            quote.getId(),
            quote.getProductId(),
            quote.getCoveragePackageId(),
            quote.getFarmerId(),
            quote.getFarmId(),
            quote.getCropId(),
            quote.getSeasonId(),
            quote.getCurrency(),
            quote.getBaseAmount(),
            quote.getGrossAmount(),
            quote.getNetAmount(),
            quote.getCoverageAmount(),
            quote.getBreakdownJson(),
            quote.getFactorsHash(),
            quote.getExpiresAt(),
            quote.getStatus()
        );
    }

    private int quoteTtlDays() {
        return configurationRepository.findByConfigKeyAndDeletedFalse("insurance.premium.quote_ttl_days")
            .map(c -> {
                try {
                    return Integer.parseInt(c.getValueJson().replace("\"", "").trim());
                } catch (Exception ex) {
                    return 7;
                }
            })
            .orElse(7);
    }

    private BigDecimal packageConfigDecimal(String json, String field, BigDecimal fallback) {
        try {
            JsonNode node = objectMapper.readTree(json == null ? "{}" : json);
            if (node.has(field)) {
                return node.path(field).decimalValue();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private String productConfigText(String json, String field, String fallback) {
        try {
            JsonNode node = objectMapper.readTree(json == null ? "{}" : json);
            if (node.hasNonNull(field)) {
                return node.path(field).asText();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return UUID.randomUUID().toString();
        }
    }
}
