package com.sky.apigateway.service;

import com.sky.apigateway.model.AvailabilityRequest;
import com.sky.apigateway.model.Flight;
import com.sky.apigateway.model.PriceAlarm;
import com.sky.apigateway.repository.PriceAlarmRepository;
import com.sky.apigateway.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PriceAlarmScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PriceAlarmScheduler.class);

    private final PriceAlarmRepository priceAlarmRepository;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;

    @Autowired
    public PriceAlarmScheduler(PriceAlarmRepository priceAlarmRepository, UserRepository userRepository, AvailabilityService availabilityService) {
        this.priceAlarmRepository = priceAlarmRepository;
        this.userRepository = userRepository;
        this.availabilityService = availabilityService;
    }

    /**
     * Bu metot, her saat başında fiyat alarmlarını kontrol etmek için çalışır.
     * Cron ifadesi "0 0 * * * ?" her saatin başında anlamına gelir.
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkPriceAlarms() {
        logger.info("Zamanlanmış fiyat alarmı kontrolü başlatılıyor...");
        List<PriceAlarm> alarms = priceAlarmRepository.findAll();

        for (PriceAlarm alarm : alarms) {
            AvailabilityRequest request = alarm.getAvailabilityRequest();

            List<Flight> flights = availabilityService.getAvailability(request);

            // Dönen uçuşlar içindeki en düşük fiyatı bul
            Optional<Double> minPriceOptional = flights.stream()
                    .map(Flight::getMinFarePrice)
                    .min(Double::compare);

            if (minPriceOptional.isPresent()) {
                double minPrice = minPriceOptional.get();
                alarm.setLastPrice(minPrice);
                priceAlarmRepository.save(alarm);

                if (minPrice < alarm.getExpectedPrice()) {
                    logger.info("Fiyat alarmı tetiklendi! Kullanıcı ID: {}, En Düşük Fiyat: {}, Alarm Fiyatı: {}", alarm.getUserId(), minPrice, alarm.getExpectedPrice());
                    sendEmailNotification(alarm);
                }
            } else {
                logger.warn("Alarm ID {} için yapılan kontrolde uçuş bulunamadı.", alarm.getId());
            }
        }
        logger.info("Zamanlanmış fiyat alarmı kontrolü tamamlandı.");
    }

    private void sendEmailNotification(PriceAlarm alarm) {
        userRepository.findById(alarm.getUserId()).ifPresent(user -> {
            String email = user.getEmail();
            String subject = "Fiyat Alarmı! Bilet Fiyatı Düştü!";
            String message = String.format(
                    "Merhaba %s,\n\nTalep ettiğiniz uçuş için bir fiyat düşüşü tespit ettik." +
                            "\nAlarm Fiyatınız: %.2f\nMevcut Fiyat: %.2f" +
                            "\n\nUçuş Detayları:\nKalkış: %s\nVarış: %s\nTarih: %s",
                    user.getName(),
                    alarm.getExpectedPrice(),
                    alarm.getLastPrice(),
                    alarm.getAvailabilityRequest().getDepPort(),
                    alarm.getAvailabilityRequest().getArrPort(),
                    alarm.getAvailabilityRequest().getDepartureDate()
            );

            // Gerçek bir uygulamada burada bir e-posta servisi kullanılır.
            logger.info("{} adresine e-posta gönderimi simüle ediliyor:", email);
            logger.info("Konu: {}", subject);
            logger.info("İçerik: {}", message);
        });
    }
}
