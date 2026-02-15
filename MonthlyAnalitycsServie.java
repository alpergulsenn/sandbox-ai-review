package com.abralabs.analytics.service;

import com.abralabs.analytics.repository.CustomerRepository;
import com.abralabs.analytics.model.Customer;
import com.abralabs.analytics.model.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class MonthlyAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyAnalyticsService.class);
    private final CustomerRepository customerRepository;

    // 💣 TUZAK 1 (Rule 9 - Thread Safety Violation): 
    // Spring @Service varsayılan olarak Singleton'dır (Tekil nesne).
    // Bu değişken tüm kullanıcılar/istekler arasında ORTAK paylaşılır.
    // Aynı anda iki kişi rapor çekerse bu değer birbirine karışır (Race Condition).
    // Linter araçları bunu yakalayamaz, sadece mimari zeka yakalar.
    private double totalRevenueAccumulator = 0.0;

    public MonthlyAnalyticsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public void processMonthlyReport(String month) {
        logger.info("Starting analysis for month: {}", month);
        
        // 1. Veritabanından müşterileri çekiyoruz
        List<Customer> customers = customerRepository.findAllActiveCustomers();
        
        // Raporlama başlangıcı (Her istekte sıfırlansa bile thread-safe değildir!)
        this.totalRevenueAccumulator = 0.0; 

        for (Customer customer : customers) {
            
            // 💣 TUZAK 2 (Rule 11 - N+1 Query Problem):
            // JPA'da ilişkiler (getOrders) genelde LAZY yüklenir.
            // Bu satır, DÖNGÜ İÇİNDE veritabanına sorgu atar.
            // Eğer 1000 müşteri varsa, sistem veritabanına 1001 sorgu atar ve kilitler.
            List<Order> orders = customer.getOrders();

            for (Order order : orders) {
                calculateTax(order); 
                // Thread-unsafe değişkene yazma işlemi
                totalRevenueAccumulator += order.getAmount();
            }
        }
        
        logger.info("Report completed. Total Revenue: {}", totalRevenueAccumulator);
    }

    private void calculateTax(Order order) {
        // ... karmaşık vergi hesaplamaları simülasyonu ...
    }
}
