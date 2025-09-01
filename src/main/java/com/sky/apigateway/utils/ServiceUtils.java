package com.sky.apigateway.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ServiceUtils {


    public static String getUrlDateFormat(String date) {
        if (date == null || date.isEmpty())
            return "";

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd+MMM+yyyy", Locale.ENGLISH);
        return LocalDate.parse(date, inputFormatter).format(outputFormatter);
    }

    public static double parsePrice(String priceText) {
        String cleanedPrice = priceText.replaceAll("[^\\d,.]", "");
        if (cleanedPrice.lastIndexOf(',') > cleanedPrice.lastIndexOf('.')) {
            // Avrupa formatı (örn: 1.234,56)
            cleanedPrice = cleanedPrice.replace(".", "").replace(',', '.');
        } else {
            // ABD/İngiltere formatı (örn: 1,234.56)
            cleanedPrice = cleanedPrice.replace(",", "");
        }

        double price = cleanedPrice.isEmpty() ? 0.0 : Double.parseDouble(cleanedPrice);
        return price;
    }
}
