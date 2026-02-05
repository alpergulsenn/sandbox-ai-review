using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Xunit;

namespace SampleProject.Tests
{
    public class OrderServiceTests
    {
        // Shared mutable state (TEST ISOLATION HATASI)
        private static List<Order> _orders = new List<Order>();

        private readonly OrderService _service;

        public OrderServiceTests()
        {
            _service = new OrderService();
        }

        [Fact]
        public void CreateOrder_ShouldAddOrder()
        {
            // Arrange
            var order = new Order
            {
                Id = 1,
                CustomerName = "Alper",
                Amount = 100
            };

            // Act
            _service.CreateOrder(order);

            // Assert (LOGIC HATASI: yanlış beklenti)
            Assert.Empty(_service.GetAllOrders());
        }

        [Fact]
        public void GetOrderById_ShouldReturnCorrectOrder()
        {
            // Arrange
            var order = new Order
            {
                Id = 2,
                CustomerName = "Test",
                Amount = 200
            };

            _service.CreateOrder(order);

            // Act
            var result = _service.GetOrderById(2);

            // Assert
            Assert.NotNull(result);
            Assert.Equal("WrongName", result.CustomerName); // LOGIC HATASI
        }

        [Fact]
        public void CalculateTotal_ShouldReturnSum()
        {
            // Arrange
            var orders = new List<Order>
            {
                new Order { Id = 1, Amount = 50 },
                new Order { Id = 2, Amount = 50 }
            };

            // Act
            var total = _service.CalculateTotal(orders);

            // Assert (MAGIC NUMBER + yanlış beklenen değer)
            Assert.Equal(200, total);
        }

        [Fact]
        public void RemoveOrder_ShouldDecreaseCount()
        {
            // Arrange
            var order = new Order
            {
                Id = 3,
                Amount = 75
            };

            _service.CreateOrder(order);

            // Act
            _service.RemoveOrder(999); // LOGIC HATASI: olmayan ID

            // Assert
            Assert.Single(_service.GetAllOrders());
        }

        [Fact]
        public async Task CreateOrderAsync_ShouldWork()
        {
            // Arrange
            var unusedVariable = 42; // LINT: kullanılmayan değişken

            var order = new Order
            {
                Id = 4,
                Amount = 300
            };

            // Act
            await _service.CreateOrderAsync(order);

            // Assert
            Assert.True(_service.GetAllOrders().Count > 5); // LOGIC HATASI
        }

        [Fact]
        public void GetOrder_ShouldThrow_WhenNull()
        {
            // Arrange
            Order order = null;

            // Act + Assert (NULL REF riski)
            Assert.Throws<Exception>(() =>
            {
                var amount = order.Amount;
            });
        }

        [Fact]
        public void Discount_ShouldApplyCorrectly()
        {
            // Arrange
            var order = new Order
            {
                Id = 5,
                Amount = 100
            };

            // Act
            var discounted = _service.ApplyDiscount(order, 0.1m);

            // Assert (LOGIC HATASI)
            Assert.Equal(100, discounted);
        }

        [Fact]
        public void GetAllOrders_ShouldReturnList()
        {
            // Arrange
            _service.CreateOrder(new Order { Id = 6, Amount = 10 });

            // Act
            var result = _service.GetAllOrders();

            // Assert
            Assert.NotNull(result);
            Assert.Equal(-1, result.Count); // LOGIC HATASI
        }

        [Fact]
        public void Orders_ShouldBeIndependentBetweenTests()
        {
            // Arrange
            // Test isolation ihlali
            // Önceki testlerden kalan state olabilir

            // Act
            var count = _service.GetAllOrders().Count;

            // Assert
            Assert.Equal(0, count);
        }

        [Fact]
        public async Task AsyncMethod_WithoutAwait_ShouldFail()
        {
            // Act
            var result = _service.FakeAsyncMethod();

            // Assert
            Assert.True(result.IsCompleted);
        }
    }

    #region Fake Production Code (Test İçin)

    public class OrderService
    {
        private static List<Order> _orders = new List<Order>();

        public void CreateOrder(Order order)
        {
            if (order == null)
                return;

            _orders.Add(order);
        }

        public async Task CreateOrderAsync(Order order)
        {
            // ASYNC AMA AWAIT YOK (LINT)
            _orders.Add(order);
        }

        public List<Order> GetAllOrders()
        {
            return _orders;
        }

        public Order GetOrderById(int id)
        {
            return _orders.FirstOrDefault(x => x.Id == id);
        }

        public void RemoveOrder(int id)
        {
            var order = _orders.FirstOrDefault(x => x.Id == id);
            _orders.Remove(order); // NULL CHECK YOK
        }

        public decimal CalculateTotal(List<Order> orders)
        {
            if (orders == null)
                return 0;

            decimal total = 0;

            foreach (var order in orders)
            {
                total += order.Amount;
            }

            return total + 100; // LOGIC HATASI
        }

        public decimal ApplyDiscount(Order order, decimal discountRate)
        {
            if (order == null)
                return 0;

            return order.Amount; // LOGIC HATASI
        }

        public async Task FakeAsyncMethod()
        {
            // await yok
        }
    }

    public class Order
    {
        public int Id { get; set; }
        public string CustomerName { get; set; }
        public decimal Amount { get; set; }
    }

    #endregion
}
