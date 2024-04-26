package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/* Метод createIntroduceGoodsDocument отправляет POST запрос на создание документа о введении товаров по переданным документам и подписи,
для ограничения лимита запросов используется очередь requestTimes, счетчик запросов requestCount и установка времени ожидания перед повторным запросом если превышен лимит. */

public class CrptApi {
    private final TimeUnit timeUnit; // Времени для ограничения запросов
    private final int requestLimit; // Лимит запросов
    private final BlockingQueue<Long> requestTimes; // Очередь для хранения времени запросов
    private final AtomicInteger requestCount; // Счетчик запросов
    private final Logger logger = LoggerFactory.getLogger(CrptApi.class); // Это логгер

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requestTimes = new LinkedBlockingQueue<>(requestLimit);
        this.requestCount = new AtomicInteger(0);
    }

    public void createIntroduceGoodsDocument(IntroduceGoodsDocument document, String signature) {
        try {
            long currentTime = System.currentTimeMillis();
            requestTimes.add(currentTime); // Добавляем время текущего запроса в очередь

            if (requestCount.incrementAndGet() > requestLimit) { // Проверяем превысил ли счетчик лимит запросов
                long oldestRequestTime = requestTimes.poll(); // Достаем из очереди метку самого старого запроса
                long timeDifference = currentTime - oldestRequestTime; // Считаем разницу между текущим временем и самым старым запросом
                long timeToWait = timeUnit.toMillis(1) - timeDifference; // Считаем время которое нужно подождать до конца промежутка времени
                if (timeToWait > 0) {
                    TimeUnit.MILLISECONDS.sleep(timeToWait); // Если есть время ожидания то поток засыпает на это время чтобы не превысить лимит запросов
                }
                requestCount.set(requestLimit); // Установливаем счетчик на максимальное значение после подождавшего интервала
            }

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonRequest = objectMapper.writeValueAsString(document); // Преобразуем объек в JSON

            HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create"); // Задаем адрес запроса
            httpPost.setEntity(new StringEntity(jsonRequest)); // Передаем JSON в запрос

            HttpResponse response = HttpClients.createDefault().execute(httpPost); // Выполняем запрос


        } catch (InterruptedException | IOException e) {
            logger.error("ERROR ", e);
        }
    }

    private static class IntroduceGoodsDocument {
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private Product[] products;
        private String regDate;
        private String regNumber;
        // Геттеры и сеттеры
    }

    private static class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
        // Геттеры и сеттеры
    }
}