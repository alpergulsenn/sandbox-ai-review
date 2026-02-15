package com.abralabs.ecommerce.service;

import com.abralabs.ecommerce.dto.OrderDTO;
import com.abralabs.ecommerce.dto.CreateOrderRequest;
import com.abralabs.ecommerce.entity.Order;
import com.abralabs.ecommerce.entity.OrderItem;
import com.abralabs.ecommerce.entity.Customer;
import com.abralabs.ecommerce.enums.OrderStatus;
import com.abralabs.ecommerce.exception.OrderNotFoundException;
import com.abralabs.ecommerce.exception.InsufficientStockException;
import com.abralabs.ecommerce.repository.OrderRepository;
import com.abralabs.ecommerce.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Order Service - Handles order business logic
 * 
 * This service demonstrates best practices:
 * - Constructor injection with Lombok
 * - Proper error handling with try-catch
 * - Modern Java features (Stream API, Optional, BigDecimal)
 * - Immutable patterns where appropriate
 * - SLF4J logging instead of System.out
 * - Transaction management
 * - Null safety with Objects.requireNonNull
 * - No magic numbers (constants defined)
 * 
 * @author Abralabs Engineering Team
 * @version 1.0
 * @since 2024-02-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleanOrderService {

    // Constants - No magic numbers
    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");
    private static final BigDecimal DISCOUNT_THRESHOLD = new BigDecimal("1000.00");
    private static final BigDecimal DISCOUNT_PERCENTAGE = new BigDecimal("0.10");
    private static final int MAX_ITEMS_PER_ORDER = 50;
    private static final int DEFAULT_CACHE_TTL_SECONDS = 3600;

    // Constructor injection with final fields (immutability)
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    /**
     * Creates a new order with full validation
     * 
     * @param request Order creation request (validated with @Valid)
     * @return Created order DTO
     * @throws IllegalArgumentException if request is invalid
     * @throws InsufficientStockException if products are out of stock
     */
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        // Defensive programming - null check
        Objects.requireNonNull(request, "Order request cannot be null");
        
        log.info("Creating order for customer ID: {}", request.getCustomerId());
        
        try {
            // Validate request
            validateOrderRequest(request);
            
            // Fetch customer with proper error handling
            Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Customer not found with ID: %d", request.getCustomerId())
                ));
            
            // Reserve inventory first (prevents overselling)
            reserveInventoryForOrder(request);
            
            // Calculate order totals
            OrderTotals totals = calculateOrderTotals(request, customer);
            
            // Build order entity with builder pattern
            Order order = Order.builder()
                .customer(customer)
                .orderDate(ZonedDateTime.now())  // Modern date/time API
                .status(OrderStatus.PENDING)
                .subtotal(totals.subtotal())
                .taxAmount(totals.tax())
                .discountAmount(totals.discount())
                .totalAmount(totals.total())
                .shippingAddress(request.getShippingAddress())
                .build();
            
            // Save order
            Order savedOrder = orderRepository.save(order);
            
            log.info("Order created successfully with ID: {}", savedOrder.getId());
            
            // Process payment asynchronously (non-blocking)
            processPaymentAsync(savedOrder);
            
            // Send notifications (error doesn't fail order creation)
            sendOrderConfirmation(savedOrder);
            
            return convertToDTO(savedOrder);
            
        } catch (InsufficientStockException e) {
            log.error("Insufficient stock for order creation: {}", e.getMessage());
            throw e;  // Re-throw business exception
            
        } catch (Exception e) {
            log.error("Unexpected error creating order", e);
            throw new RuntimeException("Failed to create order", e);
        }
    }

    /**
     * Retrieves order by ID with caching
     * Uses @Cacheable to prevent N+1 queries on repeated access
     * 
     * @param orderId Order ID to retrieve
     * @return Order DTO
     * @throws OrderNotFoundException if order doesn't exist
     */
    @Cacheable(value = "orders", key = "#orderId")
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        
        log.debug("Fetching order with ID: {}", orderId);
        
        // Proper Optional handling
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new OrderNotFoundException(
                String.format("Order not found with ID: %d", orderId)
            ));
        
        return convertToDTO(order);
    }

    /**
     * Retrieves all orders for a customer
     * Uses JOIN FETCH to prevent N+1 query problem
     * 
     * @param customerId Customer ID
     * @return List of order DTOs (never null - empty list if none found)
     */
    @Cacheable(value = "customerOrders", key = "#customerId")
    @Transactional(readOnly = true)
    public List<OrderDTO> getCustomerOrders(Long customerId) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        
        log.debug("Fetching orders for customer ID: {}", customerId);
        
        // Efficient query with JOIN FETCH (no N+1 problem)
        List<Order> orders = orderRepository.findByCustomerIdWithItems(customerId);
        
        // Stream API for clean transformation
        return orders.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Cancels an order with proper state validation
     * 
     * @param orderId Order ID to cancel
     * @param reason Cancellation reason
     * @return Cancelled order DTO
     * @throws OrderNotFoundException if order doesn't exist
     * @throws IllegalStateException if order cannot be cancelled
     */
    @Transactional
    @CacheEvict(value = {"orders", "customerOrders"}, allEntries = true)
    public OrderDTO cancelOrder(Long orderId, String reason) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(reason, "Cancellation reason cannot be null");
        
        log.info("Cancelling order ID: {} with reason: {}", orderId, reason);
        
        try {
            Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                    String.format("Order not found with ID: %d", orderId)
                ));
            
            // Business rule validation
            if (!order.isCancellable()) {
                throw new IllegalStateException(
                    String.format("Order in status %s cannot be cancelled", order.getStatus())
                );
            }
            
            // Restore inventory
            restoreInventory(order);
            
            // Process refund if payment was made
            if (order.isPaid()) {
                processRefund(order);
            }
            
            // Update order status
            order.cancel(reason);
            
            Order cancelledOrder = orderRepository.save(order);
            
            log.info("Order {} cancelled successfully", orderId);
            
            // Notify customer (non-critical - doesn't fail cancellation)
            notifyOrderCancellation(cancelledOrder);
            
            return convertToDTO(cancelledOrder);
            
        } catch (OrderNotFoundException | IllegalStateException e) {
            // Re-throw business exceptions as-is
            throw e;
            
        } catch (Exception e) {
            log.error("Error cancelling order {}", orderId, e);
            throw new RuntimeException("Failed to cancel order", e);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validates order request with business rules
     */
    private void validateOrderRequest(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        
        if (request.getItems().size() > MAX_ITEMS_PER_ORDER) {
            throw new IllegalArgumentException(
                String.format("Order cannot contain more than %d items", MAX_ITEMS_PER_ORDER)
            );
        }
        
        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            throw new IllegalArgumentException("Shipping address is required");
        }
    }

    /**
     * Reserves inventory for all items in order
     */
    private void reserveInventoryForOrder(CreateOrderRequest request) {
        request.getItems().forEach(item -> {
            boolean reserved = inventoryService.reserveStock(
                item.getProductId(), 
                item.getQuantity()
            );
            
            if (!reserved) {
                throw new InsufficientStockException(
                    String.format("Insufficient stock for product ID: %d", item.getProductId())
                );
            }
        });
    }

    /**
     * Calculates order totals with tax and discount
     * Using record for immutable result (Java 14+)
     */
    private OrderTotals calculateOrderTotals(CreateOrderRequest request, Customer customer) {
        // Calculate subtotal
        BigDecimal subtotal = request.getItems().stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Apply discount for VIP customers or high-value orders
        BigDecimal discount = BigDecimal.ZERO;
        if (customer.isVipMember() || subtotal.compareTo(DISCOUNT_THRESHOLD) >= 0) {
            discount = subtotal.multiply(DISCOUNT_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);
        }
        
        // Calculate tax on discounted amount
        BigDecimal discountedAmount = subtotal.subtract(discount);
        BigDecimal tax = discountedAmount.multiply(TAX_RATE)
            .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal total = discountedAmount.add(tax);
        
        return new OrderTotals(subtotal, tax, discount, total);
    }

    /**
     * Restores inventory for cancelled order
     */
    private void restoreInventory(Order order) {
        order.getItems().forEach(item -> 
            inventoryService.releaseStock(item.getProductId(), item.getQuantity())
        );
        
        log.info("Inventory restored for order: {}", order.getId());
    }

    /**
     * Processes payment asynchronously to avoid blocking
     */
    private void processPaymentAsync(Order order) {
        try {
            paymentService.processPaymentAsync(order.getId(), order.getTotalAmount());
        } catch (Exception e) {
            log.error("Payment processing failed for order: {}", order.getId(), e);
            // Don't fail order creation - payment will retry
        }
    }

    /**
     * Processes refund for cancelled order
     */
    private void processRefund(Order order) {
        try {
            paymentService.processRefund(order.getId(), order.getTotalAmount());
            log.info("Refund processed for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Refund processing failed for order: {}", order.getId(), e);
            // Log error but continue with cancellation
        }
    }

    /**
     * Sends order confirmation email
     */
    private void sendOrderConfirmation(Order order) {
        try {
            notificationService.sendOrderConfirmation(order);
        } catch (Exception e) {
            log.error("Failed to send order confirmation for order: {}", order.getId(), e);
            // Don't fail order creation if notification fails
        }
    }

    /**
     * Notifies customer of order cancellation
     */
    private void notifyOrderCancellation(Order order) {
        try {
            notificationService.sendCancellationNotification(order);
        } catch (Exception e) {
            log.error("Failed to send cancellation notification for order: {}", order.getId(), e);
        }
    }

    /**
     * Converts Order entity to DTO (prevents entity exposure)
     */
    private OrderDTO convertToDTO(Order order) {
        return OrderDTO.builder()
            .id(order.getId())
            .customerId(order.getCustomer().getId())
            .customerName(order.getCustomer().getName())
            .orderDate(order.getOrderDate())
            .status(order.getStatus())
            .subtotal(order.getSubtotal())
            .taxAmount(order.getTaxAmount())
            .discountAmount(order.getDiscountAmount())
            .totalAmount(order.getTotalAmount())
            .shippingAddress(order.getShippingAddress())
            .build();
    }

    // ==================== INNER CLASSES ====================

    /**
     * Immutable record for order calculation results (Java 14+)
     * Records are perfect for DTOs - immutable by default
     */
    private record OrderTotals(
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal total
    ) {
        // Compact constructor with validation
        public OrderTotals {
            Objects.requireNonNull(subtotal, "Subtotal cannot be null");
            Objects.requireNonNull(tax, "Tax cannot be null");
            Objects.requireNonNull(discount, "Discount cannot be null");
            Objects.requireNonNull(total, "Total cannot be null");
        }
    }
}
