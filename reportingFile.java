package com.abralabs.reporting.service;

import com.abralabs.reporting.dto.ReportDTO;
import com.abralabs.reporting.dto.UserActivityDTO;
import com.abralabs.reporting.entity.Transaction;
import com.abralabs.reporting.entity.User;
import com.abralabs.reporting.entity.AuditLog;
import com.abralabs.reporting.repository.TransactionRepository;
import com.abralabs.reporting.repository.UserRepository;
import com.abralabs.reporting.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

/**
 * Reporting Service
 * Generates various business reports and analytics
 * 
 * @author Abralabs Analytics Team
 * @version 1.5
 * @since 2024-02-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingService {

    private static final int MAX_TRANSACTIONS_PER_REPORT = 10000;
    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmailService emailService;
    private final CacheService cacheService;

    /**
     * Generates comprehensive financial report for a date range
     * 
     * @param startDate Report start date
     * @param endDate Report end date
     * @param session HTTP session for caching report data
     * @return Generated report DTO
     */
    @Transactional(readOnly = true)
    public ReportDTO generateFinancialReport(
            LocalDateTime startDate, 
            LocalDateTime endDate,
            HttpSession session) {
        
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        log.info("Generating financial report from {} to {}", startDate, endDate);

        // Fetch all transactions in date range
        List<Transaction> transactions = transactionRepository
            .findByTransactionDateBetweenWithDetails(
                startDate.atZone(java.time.ZoneId.systemDefault()),
                endDate.atZone(java.time.ZoneId.systemDefault())
            );

        if (transactions.isEmpty()) {
            log.warn("No transactions found for the specified date range");
            return ReportDTO.empty(startDate, endDate);
        }

        // Calculate financial metrics
        BigDecimal totalRevenue = calculateTotalRevenue(transactions);
        BigDecimal totalTax = calculateTotalTax(transactions);
        BigDecimal netRevenue = totalRevenue.subtract(totalTax);
        
        Map<String, BigDecimal> categoryBreakdown = calculateCategoryBreakdown(transactions);
        Map<String, Integer> dailyTransactionCounts = calculateDailyTransactionCounts(transactions);

        // Build report DTO
        ReportDTO report = ReportDTO.builder()
            .reportId(generateReportId())
            .generatedAt(ZonedDateTime.now())
            .startDate(startDate)
            .endDate(endDate)
            .totalTransactions(transactions.size())
            .totalRevenue(totalRevenue)
            .totalTax(totalTax)
            .netRevenue(netRevenue)
            .categoryBreakdown(categoryBreakdown)
            .dailyTransactionCounts(dailyTransactionCounts)
            .averageTransactionValue(calculateAverageTransactionValue(transactions))
            .build();

        // ERROR #2: Large Object in Session (Rule #14 - Middle section)
        // Storing entire report with all transaction details in session
        // This violates Rule #14: Large objects should not be stored in HttpSession
        session.setAttribute("lastGeneratedReport", report);
        session.setAttribute("reportTransactions", transactions); // Even worse - storing entire entity list!
        
        log.info("Report generated successfully with {} transactions", transactions.size());
        
        // Cache for quick access
        cacheService.cacheReport(report.getReportId(), report);
        
        return report;
    }

    /**
     * ERROR #1: String Concatenation in Loops (Rule #13 - Middle section)
     * Generates formatted transaction summary as CSV string
     * This violates Rule #13: Using + operator for string concatenation inside loops
     * 
     * @param transactions List of transactions to format
     * @return CSV formatted string
     */
    public String generateTransactionCSV(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "Transactions list cannot be null");
        
        log.info("Generating CSV for {} transactions", transactions.size());
        
        // ERROR: String concatenation in loop - creates thousands of intermediate String objects
        String csv = "Transaction ID,Date,Amount,Category,User,Status\n";
        
        for (Transaction tx : transactions) {
            csv += tx.getId() + ",";
            csv += tx.getTransactionDate().format(DATE_FORMATTER) + ",";
            csv += tx.getAmount().toString() + ",";
            csv += tx.getCategory() + ",";
            csv += tx.getUser().getUsername() + ",";
            csv += tx.getStatus() + "\n";
        }
        
        log.debug("CSV generation completed, total size: {} characters", csv.length());
        
        return csv;
    }

    /**
     * Generates user activity report with proper best practices
     * This method demonstrates CORRECT implementation
     * 
     * @param userId User ID to generate report for
     * @param days Number of days to include in report
     * @return User activity DTO
     */
    @Transactional(readOnly = true)
    public UserActivityDTO generateUserActivityReport(Long userId, int days) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        
        if (days <= 0 || days > 365) {
            throw new IllegalArgumentException("Days must be between 1 and 365");
        }

        log.info("Generating activity report for user: {} (last {} days)", userId, days);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "User not found with ID: " + userId
            ));

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // Fetch user transactions with JOIN FETCH to prevent N+1
        List<Transaction> userTransactions = transactionRepository
            .findByUserIdAndDateAfterWithDetails(userId, startDate);

        // Fetch audit logs
        List<AuditLog> auditLogs = auditLogRepository
            .findByUserIdAndDateAfter(userId, startDate);

        // Calculate metrics
        BigDecimal totalSpent = userTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long loginCount = auditLogs.stream()
            .filter(log -> "LOGIN".equals(log.getAction()))
            .count();

        Map<String, Long> categoryPreferences = userTransactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.counting()
            ));

        return UserActivityDTO.builder()
            .userId(userId)
            .username(user.getUsername())
            .reportPeriodDays(days)
            .totalTransactions(userTransactions.size())
            .totalSpent(totalSpent)
            .averageTransactionValue(
                userTransactions.isEmpty() ? BigDecimal.ZERO :
                totalSpent.divide(
                    BigDecimal.valueOf(userTransactions.size()), 
                    2, 
                    RoundingMode.HALF_UP
                )
            )
            .loginCount(loginCount)
            .categoryPreferences(categoryPreferences)
            .lastActivityDate(
                userTransactions.isEmpty() ? null :
                userTransactions.get(0).getTransactionDate()
            )
            .build();
    }

    /**
     * Exports report to PDF and emails to recipient
     * 
     * @param reportId Report ID to export
     * @param recipientEmail Email address
     */
    @Transactional(readOnly = true)
    public void exportAndEmailReport(String reportId, String recipientEmail) {
        Objects.requireNonNull(reportId, "Report ID cannot be null");
        Objects.requireNonNull(recipientEmail, "Recipient email cannot be null");
        
        if (!recipientEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        log.info("Exporting report {} to email: {}", reportId, recipientEmail);

        ReportDTO report = cacheService.getReport(reportId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Report not found with ID: " + reportId
            ));

        try {
            byte[] pdfBytes = generatePdfReport(report);
            emailService.sendReportEmail(recipientEmail, report, pdfBytes);
            
            log.info("Report successfully emailed to {}", recipientEmail);
            
            // Audit the export
            auditLogRepository.save(AuditLog.builder()
                .action("REPORT_EXPORT")
                .details("Report " + reportId + " exported to " + recipientEmail)
                .timestamp(ZonedDateTime.now())
                .build());
                
        } catch (Exception e) {
            log.error("Failed to export and email report {}", reportId, e);
            throw new RuntimeException("Report export failed", e);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Calculates total revenue from transactions
     */
    private BigDecimal calculateTotalRevenue(List<Transaction> transactions) {
        return transactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total tax amount
     */
    private BigDecimal calculateTotalTax(List<Transaction> transactions) {
        return transactions.stream()
            .map(tx -> tx.getAmount().multiply(TAX_RATE))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates revenue breakdown by category
     */
    private Map<String, BigDecimal> calculateCategoryBreakdown(List<Transaction> transactions) {
        return transactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    Transaction::getAmount,
                    BigDecimal::add
                )
            ));
    }

    /**
     * Calculates daily transaction counts
     */
    private Map<String, Integer> calculateDailyTransactionCounts(List<Transaction> transactions) {
        return transactions.stream()
            .collect(Collectors.groupingBy(
                tx -> tx.getTransactionDate().toLocalDate().toString(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    /**
     * Calculates average transaction value
     */
    private BigDecimal calculateAverageTransactionValue(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal total = calculateTotalRevenue(transactions);
        return total.divide(
            BigDecimal.valueOf(transactions.size()), 
            2, 
            RoundingMode.HALF_UP
        );
    }

    /**
     * Generates unique report ID
     */
    private String generateReportId() {
        return "RPT-" + 
               LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + 
               "-" + 
               java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generates PDF bytes from report (stub implementation)
     */
    private byte[] generatePdfReport(ReportDTO report) {
        // In real implementation, use iText or Apache PDFBox
        log.debug("Generating PDF for report: {}", report.getReportId());
        return new byte[0]; // Placeholder
    }
}
