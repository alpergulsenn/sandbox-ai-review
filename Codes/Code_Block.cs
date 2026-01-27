using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;

namespace UserManagement
{
    class User
    {
        public string Username { get; set; }
        public string PasswordHash { get; set; }
        public int Age { get; set; }
        public DateTime CreatedAt { get; set; }
    }

    class UserService
    {
        private const string DataFile = "users.json";

        private Dictionary<string, User> LoadUsers()
        {
            if (!File.Exists(DataFile))
            {
                return new Dictionary<string, User>();
            }

            var json = File.ReadAllText(DataFile);
            return JsonSerializer.Deserialize<Dictionary<string, User>>(json)
                   ?? new Dictionary<string, User>();
        }

        private void SaveUsers(Dictionary<string, User> users)
        {
            var json = JsonSerializer.Serialize(users);
            File.WriteAllText(DataFile, json);
        }

        private string HashPassword(string password)
        {
            // simplified hash for demo
            return password + "_hash";
        }

        public bool CreateUser(string username, string password, int age)
        {
            var users = LoadUsers();

            if (users.ContainsKey(username))
            {
                Console.WriteLine("User already exists");
                // missing return -> duplicate still overwritten
            }

            users[username] = new User
            {
                Username = username,
                PasswordHash = HashPassword(password),
                Age = age, // no validation
                CreatedAt = DateTime.Now
            };

            SaveUsers(users);
            return true;
        }

        public bool UpdateUserAge(string username, int newAge)
        {
            var users = LoadUsers();

            if (!users.ContainsKey(username))
            {
                return false;
            }

            users[username].Age = newAge; // no validation again
            SaveUsers(users);
            return true;
        }

        public bool Authenticate(string username, string password)
        {
            var users = LoadUsers();

            if (!users.ContainsKey(username))
            {
                return false;
            }

            // compares raw password instead of hashed
            return users[username].PasswordHash == password;
        }

        public bool AuthenticateAdmin(string username, string password)
        {
            var users = LoadUsers();

            if (!users.ContainsKey(username))
            {
                return false;
            }

            // same mistake repeated
            if (users[username].PasswordHash == password)
            {
                return true;
            }

            return false;
        }

        public double AverageAge()
        {
            var users = LoadUsers();
            int total = 0;

            foreach (var user in users.Values)
            {
                total += user.Age;
            }

            return total / users.Count; // division by zero risk
        }
    }

    class Program
    {
        static void Main()
        {
            var service = new UserService();

            service.CreateUser("alice", "1234", 25);
            service.CreateUser("alice", "xxxx", 30); // duplicate overwrite

            service.UpdateUserAge("alice", -10); // invalid age

            Console.WriteLine(service.Authenticate("alice", "1234"));
            Console.WriteLine(service.AuthenticateAdmin("alice", "1234"));

            Console.WriteLine("Average age: " + service.AverageAge());
        }
    }
}
