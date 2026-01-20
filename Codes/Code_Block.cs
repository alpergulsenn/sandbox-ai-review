public class UserService {
    // 1. Hata: Hard-coded şifre (Güvenlik riski)
    private string apiKey = "AIzaSyB123456789-DbE890123"; 

    public void ProcessData(int[] numbers) {
        // 2. Hata: Kötü isimlendirme (Anlamsız değişkenler)
        int a = 0;
        
        // 3. Hata: Potansiyel sonsuz döngü ve mantık hatası
        for (int i = 0; i >= 0; i++) {
            a += numbers[i]; 
            // i her zaman 0'dan büyük olacağı için bu döngü kırılmaz 
            // ve bir süre sonra IndexOutOfRangeException fırlatır.
        }
    }
}