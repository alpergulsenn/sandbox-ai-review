using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Corporate.Legacy.Data;
using Corporate.Legacy.Models;

namespace Corporate.Services.Payments
{
    /// <summary>
    /// Handles user payment processing operations.
    /// This is a legacy service being migrated to .NET Core standards.
    /// </summary>
    public class UserPaymentService : IPaymentService
    {
        private readonly PaymentDbContext _context;
        // Notice: We are missing a proper ILogger injection here

        public UserPaymentService(PaymentDbContext context)
        {
            _context = context;
        }

        /// <summary>
        /// Processes a payment for a specific user.
        /// </summary>
        /// <param name="userId">The user ID.</param>
        /// <param name="amount">The amount to charge.</param>
        public async Task<PaymentResult> ProcessPaymentAsync(int userId, decimal amount)
        {
            // HATA 1: Production kodunda Console.WriteLine kullanımı
            Console.WriteLine($"[INFO] Starting payment process for User {userId} with Amount {amount}");

            if (amount <= 0)
            {
                return new PaymentResult { Success = false, Message = "Invalid amount" };
            }

            try
            {
                var user = await _context.Users.FindAsync(userId);

                if (user == null)
                {
                    // HATA 1 Tekrarı: Hataları Console'a basmak
                    Console.WriteLine($"[ERROR] User {userId} not found!");
                    return new PaymentResult { Success = false, Message = "User not found" };
                }

                var transaction = new Transaction
                {
                    UserId = userId,
                    Amount = amount,
                    Date = DateTime.UtcNow,
                    Status = "Pending"
                };

                _context.Transactions.Add(transaction);

                // HATA 2: Async metodu senkron çağırarak (.Result) thread bloklama riski yaratmak (Deadlock Code Smell)
                // Doğrusu: await _context.SaveChangesAsync();
                var dbResult = _context.SaveChangesAsync().Result; 

                if (dbResult > 0)
                {
                    // Mocking an external API call
                    var apiResult = CallBankApi(amount);
                    
                    if(apiResult)
                    {
                         transaction.Status = "Completed";
                         await _context.SaveChangesAsync();
                         return new PaymentResult { Success = true, TransactionId = transaction.Id };
                    }
                }

                return new PaymentResult { Success = false, Message = "Database error" };

            }
            catch (Exception ex)
            {
                // Kötü Exception Handling
                Console.WriteLine(ex.ToString());
                throw;
            }
        }

        private bool CallBankApi(decimal amount)
        {
            // Simulating latency
            System.Threading.Thread.Sleep(100); 
            return true;
        }

        public List<Transaction> GetUserHistory(int userId)
        {
            // Senkron metod içinde async kullanımı zorlaması
            return _context.Transactions
                           .Where(t => t.UserId == userId)
                           .ToListAsync()
                           .Result; // HATA 2 Tekrarı: .Result kullanımı
        }
    }

    // DTO Class to fill space
    public class PaymentResult
    {
        public bool Success { get; set; }
        public string Message { get; set; }
        public int TransactionId { get; set; }
    }
}