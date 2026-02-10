package com.abralabs.ecommerce.service;

import com.abralabs.ecommerce.dto.OrderDTO;
import com.abralabs.ecommerce.dto.OrderItemDTO;
import com.abralabs.ecommerce.dto.CreateOrderRequest;
import com.abralabs.ecommerce.dto.UpdateOrderStatusRequest;
import com.abralabs.ecommerce.entity.Order;
import com.abralabs.ecommerce.entity.OrderItem;
import com.abralabs.ecommerce.entity.Product;
import com.abralabs.ecommerce.entity.Customer;
import com.abralabs.ecommerce.enums.OrderStatus;
import com.abralabs.ecommerce.enums.PaymentMethod;
import com.abralabs.ecommerce.exception.InsufficientStockException;
import com.abralabs.ecommerce.exception.OrderNotFoundException;
import com.abralabs.ecommerce.exception.InvalidOrderStateException;
import com.abralabs.ecommerce.repository.OrderRepository;
import com.abralabs.ecommerce.repository.ProductRepository;
import com.abralabs.ecommerce.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Order Management Service
 * Handles order creation, updates, cancellations, and reporting
 * 
 * @author Abralabs Engineering Team
 * @version 2.0
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderManagementService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");
    private static final BigDecimal DISCOUNT_THRESHOLD = new BigDecimal("1000.00");
    private static final BigDecimal DISCOUNT_PERCENTAGE = new BigDecimal("0.10");
    private static final int MAX_ITEMS_PER_ORDER = 50;
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
        OrderStatus.PENDING,
        OrderStatus.CONFIRMED,
        OrderStatus.PROCESSING
    );

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final DataSource dataSource;

    /**
     * Creates a new order with validation and inventory checks
     * 
     * @param request Order creation request containing customer and items
     * @return Created order DTO
     * @throws InsufficientStockException if any product is out of stock
     * @throws IllegalArgumentException if request is invalid
     */
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        Objects.requireNonNull(request, "Order request cannot be null");
        validateOrderRequest(request);

        log.info("Creating order for customer: {}", request.getCustomerId());

        // Fetch customer with validation
        Customer customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Customer not found with ID: " + request.getCustomerId()
            ));

        // Validate and reserve inventory
        List<OrderItem> orderItems = prepareOrderItems(request.getItems());
        
        // Calculate totals
        OrderTotals totals = calculateOrderTotals(orderItems, customer);

        // Create order entity
        Order order = Order.builder()
            .customer(customer)
            .orderDate(ZonedDateTime.now())
            .status(OrderStatus.PENDING)
            .items(orderItems)
            .subtotal(totals.subtotal())
            .taxAmount(totals.tax())
            .discountAmount(totals.discount())
            .totalAmount(totals.total())
            .paymentMethod(request.getPaymentMethod())
            .shippingAddress(request.getShippingAddress())
            .billingAddress(request.getBillingAddress())
            .build();

        // Set bidirectional relationship
        orderItems.forEach(item -> item.setOrder(order));

        // Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());

        // Process payment asynchronously
        try {
            paymentService.processPayment(savedOrder.getId(), totals.total(), request.getPaymentMethod());
        } catch (Exception e) {
            log.error("Payment processing failed for order: {}", savedOrder.getId(), e);
            // Don't fail order creation - payment will retry
        }

        // Send confirmation email
        sendOrderConfirmation(savedOrder, customer);

        return convertToDTO(savedOrder);
    }

    /**
     * Retrieves order by ID with full details
     * 
     * @param orderId Order ID
     * @return Order DTO
     * @throws OrderNotFoundException if order doesn't exist
     */
    @Cacheable(value = "orders", key = "#orderId")
    public OrderDTO getOrderById(Long orderId) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");

        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new OrderNotFoundException(
                "Order not found with ID: " + orderId
            ));

        return convertToDTO(order);
    }

    /**
     * Retrieves all orders for a specific customer
     * Uses JOIN FETCH to prevent N+1 query problem
     * 
     * @param customerId Customer ID
     * @return List of order DTOs
     */
    @Cacheable(value = "customerOrders", key = "#customerId")
    public List<OrderDTO> getCustomerOrders(Long customerId) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");

        log.debug("Fetching orders for customer: {}", customerId);

        List<Order> orders = orderRepository.findByCustomerIdWithItems(customerId);
        
        return orders.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Updates order status with validation
     * 
     * @param orderId Order ID
     * @param request Status update request
     * @return Updated order DTO
     * @throws InvalidOrderStateException if transition is not allowed
     */
    @Transactional
    @CacheEvict(value = {"orders", "customerOrders"}, allEntries = true)
    public OrderDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(request, "Update request cannot be null");

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(
                "Order not found with ID: " + orderId
            ));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getNewStatus();

        // Validate status transition
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new InvalidOrderStateException(
                String.format("Cannot transition from %s to %s", currentStatus, newStatus)
            );
        }

        order.setStatus(newStatus);
        order.setLastModifiedDate(ZonedDateTime.now());

        if (newStatus == OrderStatus.SHIPPED) {
            order.setShippedDate(ZonedDateTime.now());
            order.setTrackingNumber(request.getTrackingNumber());
        } else if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredDate(ZonedDateTime.now());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated from {} to {}", orderId, currentStatus, newStatus);

        // Send status update notification
        notifyStatusChange(updatedOrder, currentStatus, newStatus);

        return convertToDTO(updatedOrder);
    }

    /**
     * Cancels an order if it's in a cancellable state
     * 
     * @param orderId Order ID
     * @param reason Cancellation reason
     * @return Cancelled order DTO
     * @throws InvalidOrderStateException if order cannot be cancelled
     */
    @Transactional
    @CacheEvict(value = {"orders", "customerOrders"}, allEntries = true)
    public OrderDTO cancelOrder(Long orderId, String reason) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");

        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new OrderNotFoundException(
                "Order not found with ID: " + orderId
            ));

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderStateException(
                "Order in status " + order.getStatus() + " cannot be cancelled"
            );
        }

        // Restore inventory
        restoreInventory(order.getItems());

        // Process refund if payment was made
        if (order.getStatus() != OrderStatus.PENDING) {
            try {
                paymentService.processRefund(order.getId(), order.getTotalAmount());
            } catch (Exception e) {
                log.error("Refund processing failed for order: {}", orderId, e);
                // Continue with cancellation - refund will retry
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancelledDate(ZonedDateTime.now());
        order.setLastModifiedDate(ZonedDateTime.now());

        Order cancelledOrder = orderRepository.save(order);
        log.info("Order {} cancelled. Reason: {}", orderId, reason);

        // Send cancellation notification
        notificationService.sendOrderCancellation(cancelledOrder, reason);

        return convertToDTO(cancelledOrder);
    }

    /**
     * Generates sales report for a date range
     * 
     * @param startDate Report start date
     * @param endDate Report end date
     * @return Sales statistics
     */
    public SalesReport generateSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        log.info("Generating sales report from {} to {}", startDate, endDate);

        List<Order> orders = orderRepository.findByOrderDateBetween(
            startDate.atZone(java.time.ZoneId.systemDefault()),
            endDate.atZone(java.time.ZoneId.systemDefault())
        );

        // Calculate statistics
        long totalOrders = orders.size();
        BigDecimal totalRevenue = orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = totalOrders > 0
            ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        long cancelledOrders = orders.stream()
            .filter(order -> order.getStatus() == OrderStatus.CANCELLED)
            .count();

        return SalesReport.builder()
            .startDate(startDate)
            .endDate(endDate)
            .totalOrders(totalOrders)
            .totalRevenue(totalRevenue)
            .averageOrderValue(averageOrderValue)
            .cancelledOrders(cancelledOrders)
            .cancellationRate(totalOrders > 0 
                ? (double) cancelledOrders / totalOrders * 100 
                : 0.0)
            .build();
    }

    /**
     * ERROR #1: SQL INJECTION VULNERABILITY
     * Gets top customers by total spend using raw SQL
     * VULNERABILITY: String concatenation in SQL query
     * 
     * @param limit Number of customers to return
     * @return List of customer IDs and total spend
     */
    public List<CustomerSpend> getTopCustomers(int limit) {
        log.info("Fetching top {} customers", limit);

        try {
            // ERROR: Resource leak - Connection not closed properly
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            
            // ERROR: SQL Injection - String concatenation instead of PreparedStatement
            String sql = "SELECT customer_id, SUM(total_amount) as total_spend " +
                        "FROM orders " +
                        "WHERE status != 'CANCELLED' " +
                        "GROUP BY customer_id " +
                        "ORDER BY total_spend DESC " +
                        "LIMIT " + limit;
            
            ResultSet rs = stmt.executeQuery(sql);
            
            List<CustomerSpend> topCustomers = new java.util.ArrayList<>();
            while (rs.next()) {
                topCustomers.add(new CustomerSpend(
                    rs.getLong("customer_id"),
                    rs.getBigDecimal("total_spend")
                ));
            }
            
            return topCustomers;
            
        } catch (Exception e) {
            log.error("Error fetching top customers", e);
            return List.of();
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validates order request
     */
    private void validateOrderRequest(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        if (request.getItems().size() > MAX_ITEMS_PER_ORDER) {
            throw new IllegalArgumentException(
                "Order cannot contain more than " + MAX_ITEMS_PER_ORDER + " items"
            );
        }

        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }

        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            throw new IllegalArgumentException("Shipping address is required");
        }
    }

    /**
     * Prepares order items with inventory validation
     */
    private List<OrderItem> prepareOrderItems(List<OrderItemDTO> itemDTOs) {
        return itemDTOs.stream()
            .map(dto -> {
                Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found with ID: " + dto.getProductId()
                    ));

                // Check inventory
                if (!inventoryService.checkAvailability(product.getId(), dto.getQuantity())) {
                    throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName()
                    );
                }

                // Reserve inventory
                inventoryService.reserveStock(product.getId(), dto.getQuantity());

                return OrderItem.builder()
                    .product(product)
                    .quantity(dto.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity())))
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Calculates order totals including tax and discount
     */
    private OrderTotals calculateOrderTotals(List<OrderItem> items, Customer customer) {
        BigDecimal subtotal = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Apply customer loyalty discount
        BigDecimal discount = BigDecimal.ZERO;
        if (subtotal.compareTo(DISCOUNT_THRESHOLD) >= 0 || customer.isVipMember()) {
            discount = subtotal.multiply(DISCOUNT_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal discountedAmount = subtotal.subtract(discount);
        BigDecimal tax = discountedAmount.multiply(TAX_RATE)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = discountedAmount.add(tax);

        return new OrderTotals(subtotal, tax, discount, total);
    }

    /**
     * Validates order status transition
     */
    private boolean isValidStatusTransition(OrderStatus current, OrderStatus newStatus) {
        return switch (current) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    /**
     * Restores inventory for cancelled order
     */
    private void restoreInventory(List<OrderItem> items) {
        items.forEach(item -> 
            inventoryService.releaseStock(item.getProduct().getId(), item.getQuantity())
        );
        log.info("Inventory restored for {} items", items.size());
    }

    /**
     * Sends order confirmation notification
     */
    private void sendOrderConfirmation(Order order, Customer customer) {
        try {
            notificationService.sendOrderConfirmation(order);
            log.info("Confirmation email sent for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send confirmation for order: {}", order.getId(), e);
            // Don't fail the order if email fails
        }
    }

    /**
     * Sends status change notification
     */
    private void notifyStatusChange(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        try {
            notificationService.sendStatusUpdate(order, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to send status update notification", e);
        }
    }

    /**
     * Converts Order entity to DTO
     */
    private OrderDTO convertToDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
            .map(item -> OrderItemDTO.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build())
            .collect(Collectors.toList());

        return OrderDTO.builder()
            .id(order.getId())
            .customerId(order.getCustomer().getId())
            .customerName(order.getCustomer().getName())
            .orderDate(order.getOrderDate())
            .status(order.getStatus())
            .items(itemDTOs)
            .subtotal(order.getSubtotal())
            .taxAmount(order.getTaxAmount())
            .discountAmount(order.getDiscountAmount())
            .totalAmount(order.getTotalAmount())
            .paymentMethod(order.getPaymentMethod())
            .shippingAddress(order.getShippingAddress())
            .trackingNumber(order.getTrackingNumber())
            .build();
    }

    // ==================== INNER CLASSES ====================

    private record OrderTotals(
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal total
    ) {}

    public record CustomerSpend(
        Long customerId,
        BigDecimal totalSpend
    ) {}

    /**
     * Calculates estimated delivery date based on shipping method
     * 
     * @param order Order to calculate delivery for
     * @return Estimated delivery date
     */
    private LocalDateTime calculateEstimatedDelivery(Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        
        LocalDateTime shippedDate = order.getShippedDate() != null 
            ? order.getShippedDate().toLocalDateTime()
            : LocalDateTime.now();
        
        // Standard shipping: 5-7 business days
        // Express shipping: 2-3 business days
        // Overnight: 1 business day
        int daysToAdd = switch (order.getPaymentMethod()) {
            case CREDIT_CARD -> 3; // Express for credit card
            case PAYPAL -> 5;      // Standard for PayPal
            case BANK_TRANSFER -> 7; // Slower for bank transfer
            default -> 5;
        };
        
        return shippedDate.plusDays(daysToAdd);
    }

    /**
     * Checks if order qualifies for free shipping
     * 
     * @param totalAmount Order total amount
     * @return true if qualifies for free shipping
     */
    private boolean qualifiesForFreeShipping(BigDecimal totalAmount) {
        BigDecimal freeShippingThreshold = new BigDecimal("500.00");
        return totalAmount.compareTo(freeShippingThreshold) >= 0;
    }

    /**
     * Calculates shipping cost based on order details
     * 
     * @param order Order to calculate shipping for
     * @return Shipping cost
     */
    private BigDecimal calculateShippingCost(Order order) {
        if (qualifiesForFreeShipping(order.getTotalAmount())) {
            return BigDecimal.ZERO;
        }
        
        // Base shipping cost
        BigDecimal baseShippingCost = new BigDecimal("15.00");
        
        // Add cost per item
        int totalItems = order.getItems().stream()
            .mapToInt(OrderItem::getQuantity)
            .sum();
        
        BigDecimal perItemCost = new BigDecimal("2.00");
        BigDecimal itemsCharge = perItemCost.multiply(BigDecimal.valueOf(totalItems));
        
        return baseShippingCost.add(itemsCharge);
    }

    /**
     * Validates customer's credit limit for the order
     * 
     * @param customer Customer placing the order
     * @param orderTotal Total order amount
     * @return true if customer has sufficient credit
     */
    private boolean validateCreditLimit(Customer customer, BigDecimal orderTotal) {
        if (customer.getCreditLimit() == null) {
            return true; // No credit limit set
        }
        
        BigDecimal currentCreditUsed = orderRepository
            .findPendingOrdersTotalByCustomerId(customer.getId())
            .orElse(BigDecimal.ZERO);
        
        BigDecimal availableCredit = customer.getCreditLimit().subtract(currentCreditUsed);
        
        return availableCredit.compareTo(orderTotal) >= 0;
    }

    /**
     * Generates unique order reference number
     * Format: ORD-YYYYMMDD-XXXXX
     * 
     * @return Generated order reference
     */
    private String generateOrderReference() {
        String datePart = LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        String randomPart = String.format("%05d", 
            new java.util.Random().nextInt(100000));
        
        return String.format("ORD-%s-%s", datePart, randomPart);
    }
}
