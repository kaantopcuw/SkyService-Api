# API Gateway

Bu proje, **Bahamasair**, **Sunrise Air** ve **Skyalps**'ın API'lerini kullanarak uçuşları listeleyen ve takip eden bir REST API'dir. Kullanıcıların belirledikleri uçuşların fiyatı istedikleri bir seviyeye düştüğünde, uygulama otomatik olarak bir e-posta bildirimi gönderir.

## Kullanılan Teknolojiler ve Kütüphaneler

Projenin temelini oluşturan başlıca kütüphaneler şunlardır:

*   **Spring Boot:** 
    *   `spring-boot-starter-data-jpa`: Veritabanı işlemleri için JPA (Java Persistence API) entegrasyonu.
    *   `spring-boot-starter-web` ve `spring-boot-starter-webflux`: RESTful API'ler oluşturmak için kullanılır.
    *   `spring-boot-starter-cache`: Performansı artırmak için önbellekleme desteği.
*   **H2 Database:** Geliştirme ve test aşamaları için kullanılan bir in-memory (bellek içi) veritabanı.
*   **Lombok:** Kod tekrarını azaltmak ve daha temiz bir kod yapısı oluşturmak için kullanılır.
*   **Jsoup:** HTML parse etmek için kullanılır, havayolu sitelerinden availability çekmek için.
*   **Spring Security:** Uygulama güvenliğini sağlamak için kullanılır.

## Docker ile Çalıştırma

Uygulamayı Docker kullanarak kolayca ayağa kaldırabilirsiniz. Aşağıdaki adımları takip etmeniz yeterlidir:

1.  **Docker Image'ını Oluşturma:**
    Projenin kök dizininde aşağıdaki komutu çalıştırarak Docker imajını oluşturun.
    ```bash
    docker build -t api-gateway-app .
    ```

2.  **Container'ı Başlatma:**
    Oluşturduğunuz imajı kullanarak container'ı başlatın. Bu komut, uygulamayı arkaplanda (`-d` parametresi) çalıştıracak ve `8080` portunu yönlendirecektir.
    ```bash
    docker run -d -p 8080:8080 --name api-gateway-container api-gateway-app
    ```

3.  **Logları Görüntüleme:**
    Uygulamanın loglarını ve olası hataları görmek için aşağıdaki komutu kullanabilirsiniz.
    ```bash
    docker logs api-gateway-container
    ```

## SKY API Dökümantasyonu

### AvailabilityController
Uçuş müsaitliği ve liman bilgileri ile ilgili işlemleri yönetir.

---

#### 1. Havayolu Liman Gruplarını Getir
Hizmet verilen tüm havaalanı gruplarını ve bu gruplara ait limanların listesini döner.

*   **HTTP Metodu:** `GET`
*   **Endpoint:** `/portGroups`
*   **Request Body:** Gerekli değil.
*   **Örnek İstek:**
    ```
    GET /portGroups
    ```
*   **Örnek Yanıt:**
    ```json
    {
        "Greece": [
            {
                "city": "Corfu",
                "code": "CFU",
                "country": "EL",
                "cityName": "Corfu",
                "countryName": "Greece",
                "portName": "Corfu",
                "timeZone": null
            }
        ],
        "Danmark": [
            {
                "city": "Billund",
                "code": "BLL",
                "country": "DK",
                "cityName": "Billund",
                "countryName": "Danmark",
                "portName": "Billund",
                "timeZone": null
            }
        ]
    }
    ```

---

#### 2. Seçilen Porta Göre Destinasyonları Getir
`/portGroups`'dan dönen portlar içerisinden seçilen bir kalkış (`From`) noktasına göre hangi havalimanlarına (`To`) gidilebildiğinin listesini döner.

*   **HTTP Metodu:** `GET`
*   **Endpoint:** `/portsByCountry/{portCode}`
*   **Path Variable:**
    *   `portCode` (String): Hava limanı kodu (Örn: "NAS").
*   **Request Body:** Gerekli değil.
*   **Örnek İstek:**
    ```
    GET /portsByCountry/NAS
    ```
*   **Örnek Yanıt:**
    ```json
    {
        "Haiti": [
            {
                "city": "CAP",
                "code": "CAP",
                "country": "HT",
                "cityName": "Cap Haitien",
                "countryName": "Haiti",
                "portName": "Cap Haitien",
                "timeZone": null
            }
        ],
        "USA": [
            {
                "city": "FLL",
                "code": "FLL",
                "country": "US",
                "cityName": "Fort Lauderdale",
                "countryName": "USA",
                "portName": "Fort Lauderdale/Hollywood Intl",
                "timeZone": null
            }
        ]
    }
    ```

---

#### 3. Uçuş Müsaitliğini Sorgula
Belirtilen kriterlere göre uçuş müsaitliğini arar ve uygun uçuşların listesini döner.

*   **HTTP Metodu:** `POST`
*   **Endpoint:** `/availability`
*   **Açıklama:**
    *   `depPort` alanına `/portGroups` isteğinin yanıtında bulunan `code` alanı setlenir.
    *   `arrPort` alanına ise seçilen `depPort`'a göre `/portsByCountry/{depPort}` isteği gönderilerek dönen yanıttaki `code` alanı setlenir.
    *   `tripType`, `passengerType`, `cabinClass` ve `lang` değerleri için örnekteki sabit değerler gönderilebilir.
*   **Request Body:** `AvailabilityRequest` nesnesi.

| Alan Adı      | Tipi   | Açıklama                           | Örnek Değer  |
|---------------|--------|------------------------------------|--------------|
| `tripType`    | String | Seyahat tipi.                      | "ONE_WAY"    |
| `depPort`     | String | Kalkış limanı kodu.                | "AOI"        |
| `arrPort`     | String | Varış limanı kodu.                 | "LIN"        |
| `departureDate`| String | Gidiş tarihi (Format: dd.mm.yyyy). | "15.09.2025" |
| `passengerType`| String | Yolcu tipi.                        | "ADULT"      |
| `quantity`    | int    | Yolcu sayısı.                      | 1            |
| `currency`    | String | Para birimi ("EUR", "USD").        | "EUR"        |
| `cabinClass`  | String | Kabin sınıfı.                      | "ECONOMY"    |
| `lang`        | String | Dil tercihi.                       | "EN"         |

*   **Örnek İstek Body'si:**
    ```json
    {
      "tripType": "ONE_WAY",
      "depPort": "AOI",
      "arrPort": "LIN",
      "departureDate": "15.09.2025",
      "passengerType": "ADULT",
      "quantity": 1,
      "currency": "EUR",
      "cabinClass": "ALL",
      "lang": "EN"
    }
    ```

---
---

### UserController
Kullanıcı kaydı, girişi ve kullanıcıya özel işlemlerle ilgili endpoint'leri içerir. Tüm endpoint'ler `/user` path'i altındadır.

---

#### 1. Kullanıcı Kaydı
Yeni bir kullanıcı hesabı oluşturur.

*   **HTTP Metodu:** `POST`
*   **Endpoint:** `/user/register`
*   **Örnek İstek Body'si:**
    ```json
    {
        "name": "Test User",
        "email": "test@example.com",
        "password": "1234"
    }
    ```
*   **Örnek Yanıt (`201 Created`):**
    ```json
    {
        "id": 1,
        "name": "Test User",
        "email": "test@example.com",
        "password": null
    }
    ```

---

#### 2. Kullanıcı Girişi
Kullanıcının sisteme giriş yapmasını sağlar ve başarılı girişte kullanıcı bilgilerini döner.

*   **HTTP Metodu:** `POST`
*   **Endpoint:** `/user/login`
*   **Request Body:**
    *   `email` (String): Kullanıcının e-posta adresi.
    *   `password` (String): Kullanıcının şifresi.
*   **Örnek İstek Body'si:**
    ```json
    {
        "email":"test@example.com",
        "password":"1234"
    }
    ```
*   **Örnek Yanıtlar:**
    *   **Başarılı (`200 OK`):**
        ```json
        {
            "id": 1,
            "name": "Test User",
            "email": "test@example.com"
        }
        ```
    *   **Hatalı (`401 Unauthorized`):**
        ```
        "Email or password incorrect."
        ```

---

#### 3. Fiyat Alarmı Oluştur
Belirli bir uçuş sorgusu için fiyat alarmı kurar.

*   **HTTP Metodu:** `POST`
*   **Endpoint:** `/user/alarms`
*   **Açıklama:** Bu endpoint, bir `/availability` isteği atılıp sonuçlar listelendikten sonra kullanıcının isteği üzerine tetiklenir.
*   **Örnek İstek Body'si:**
    ```json
    {
       "userId": 1,
       "expectedPrice": 50,
       "availabilityRequest": {
          "tripType": "ONE_WAY",
          "depPort": "AOI",
          "arrPort": "LIN",
          "departureDate": "15.09.2025",
          "passengerType": "ADULT",
          "quantity": 1,
          "currency": "EUR",
          "cabinClass": "ALL",
          "lang": "EN"
        }
    }
    ```
*   **Örnek Yanıt (`201 Created`):**
    ```json
    {
        "id": 1,
        "userId": 1,
        "availabilityRequest": {
            "tripType": "ONE_WAY",
            "depPort": "AOI",
            "arrPort": "LIN",
            "departureDate": "15.09.2025",
            "passengerType": "ADULT",
            "quantity": 1,
            "currency": "EUR",
            "cabinClass": "ALL",
            "lang": "EN"
        },
        "expectedPrice": 50.0,
        "lastPrice": 78.0
    }
    ```

---

#### 4. Kullanıcının Fiyat Alarmlarını Getir
Belirtilen kullanıcı ID'sine ait tüm fiyat alarmlarını listeler.

*   **HTTP Metodu:** `GET`
*   **Endpoint:** `/user/alarms/{userId}`
*   **Path Variable:**
    *   `userId` (int): Fiyat alarmları getirilecek kullanıcının ID'si.
*   **Örnek İstek:**
    ```
    GET /user/alarms/1
    ```
*   **Örnek Yanıt (`200 OK`):**
    ```json
    [
        {
            "id": 1,
            "userId": 1,
            "availabilityRequest": {
                "tripType": "ONE_WAY",
                "depPort": "AOI",
                "arrPort": "LIN",
                "departureDate": "15.09.2025",
                "passengerType": "ADULT",
                "quantity": 1,
                "currency": "EUR",
                "cabinClass": "ALL",
                "lang": "EN"
            },
            "expectedPrice": 50.0,
            "lastPrice": 78.0
        }
    ]
    ```
---

#### 5. Fiyat Alarmını Sil
Belirtilen ID'ye sahip fiyat alarmını siler.

*   **HTTP Metodu:** `DELETE`
*   **Endpoint:** `/user/alarms/{id}`
*   **Path Variable:**
    *   `id` (Long): Silinecek alarmın ID'si.
*   **Örnek İstek:**
    ```
    DELETE /user/alarms/1
    ```
*   **Yanıt:** `200 OK` (Body içermez)