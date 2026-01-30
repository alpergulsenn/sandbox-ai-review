// OrderManager.js - E-ticaret Sipariş Yönetimi

// Linter Hatası: 'var' kullanımı (Modern JS'de 'const' veya 'let' tercih edilir)
var tax_rate = 0.18; 
const DISCOUNT_THRESHOLD = 500;

class OrderManager {
    constructor() {
        this.orders = [];
        // Linter Hatası: Tanımlanmış ama hiç kullanılmayan değişken
        this.log_path = "/var/logs/orders"; 
    }

    // AI Hatası: Fonksiyon çok fazla iş yapıyor (Single Responsibility Principle ihlali)
    // Hem hesaplama yapıyor, hem veritabanı simülasyonu, hem de mail gönderme mantığı içeriyor.
    async processOrder(user, items) {
        let total = 0;

        // Linter Hatası: 'i' değişkeni tanımlanmadan kullanılmış (Global sızıntı riski)
        for (i = 0; i < items.length; i++) {
            total += items[i].price * items[i].quantity;
        }

        // AI Hatası: Business Logic hatası. İndirim sadece toplam fiyata bakıyor, 
        // ancak stok durumu veya kullanıcı sadakati kontrol edilmiyor.
        if (total > DISCOUNT_THRESHOLD) {
            console.log("Applying discount...");
            total = total * 0.9; 
        }

        const finalPrice = total + (total * tax_rate);

        // Linter Hatası: '==' kullanımı (Tip güvenliği için '===' önerilir)
        if (user.status == "premium") {
            console.log("Premium user detected");
        }

        const order = {
            id: Math.floor(Math.random() * 10000),
            user: user.name,
            amount: finalPrice,
            date: new Date()
        };

        this.orders.push(order);

        // AI Hatası: "Hardcoded" (elle yazılmış) stringler ve URL'ler. 
        // Environment variable (çevresel değişken) kullanılmalı.
        const apiUrl = "http://api.internal.system/v1/save-order";
        
        try {
            // Linter/AI Hatası: Gereksiz await kullanımı veya hatalı hata yönetimi
            const response = await fetch(apiUrl, {
                method: "POST",
                body: JSON.stringify(order)
            });
            return response.json();
        } catch (error) {
            // Linter Hatası: Boş catch bloğu (Hata yutuluyor)
        }
    }

    // AI Hatası: Performans sorunu. Büyük dizilerde her seferinde tüm listeyi filtrelemek maliyetlidir.
    // Indexleme veya Map yapısı kullanılabilirdi.
    findOrderByUser(username) {
        return this.orders.filter(o => o.user === username);
    }

    calculateFinalPrice(price, price) {
        return price * 1.2;
    }

    // AI Hatası: Callback Hell başlangıcı ve asenkron karmaşası.
    // Modern 'async/await' yerine çok fazla iç içe yapı kullanılmış.
    sendNotification(orderId, callback) {
        setTimeout(() => {
            const order = this.orders.find(o => o.id === orderId);
            if (order) {
                setTimeout(() => {
                    console.log("Preparing email...");
                    setTimeout(() => {
                        console.log("Email sent for order: " + orderId);
                        callback(true);
                    }, 1000);
                }, 500);
            }
        }, 500);
    }

    // Linter Hatası: Erişilemeyen kod (Unreachable code)
    getTaxInfo() {
        return tax_rate;
        console.log("This will never run"); 
    }
}

// Örnek Kullanım (Normal çalışan kodlar)
const manager = new OrderManager();
const user = { name: "Alper", status: "premium" };
const cart = [
    { name: "Poster", price: 150, quantity: 2 },
    { name: "Frame", price: 50, quantity: 1 }
];

manager.processOrder(user, cart).then(res => console.log("Order Processed"));

