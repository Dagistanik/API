package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CrptApi {
    private final Semaphore semaphore;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Logger logger = Logger.getLogger(CrptApi.class.getName());


    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        semaphore = new Semaphore(requestLimit);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            int permitsToRelease = requestLimit - semaphore.availablePermits();
            if (permitsToRelease > 0) {
                semaphore.release(permitsToRelease);
            }
        }, 0, timeUnit.toSeconds(1), TimeUnit.SECONDS);
    }

    public void createDocument(Document document, String signature) throws Exception {
        semaphore.acquire();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(document.toJson()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.log(Level.SEVERE, "Failed to create document: " + response.body());
                throw new Exception("Failed to create document: " + response.body());
            }
        } finally {
            semaphore.release();
        }
    }

    public static class Document {
        private static final ObjectMapper objectMapper = new ObjectMapper();

        @JsonProperty("description")
        private Description description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        @JsonProperty("importRequest")
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private String productionDate;
        @JsonProperty("production_type")
        private String productionType;
        @JsonProperty("products")
        private List<Product> products;
        @JsonProperty("reg_date")
        private String regDate;
        @JsonProperty("reg_number")
        private String regNumber;


        public String toJson() throws Exception {
            return objectMapper.writeValueAsString(this);
        }

        public static class Description {
            @JsonProperty("participantInn")
            private String participantInn;

        }

        public static class Product {
            @JsonProperty("certificate_document")
            private String certificateDocument;
            @JsonProperty("certificate_document_date")
            private String certificateDocumentDate;
            @JsonProperty("certificate_document_number")
            private String certificateDocumentNumber;
            @JsonProperty("owner_inn")
            private String ownerInn;
            @JsonProperty("producer_inn")
            private String producerInn;
            @JsonProperty("production_date")
            private String productionDate;
            @JsonProperty("tnved_code")
            private String tnvedCode;
            @JsonProperty("uit_code")
            private String uitCode;
            @JsonProperty("uitu_code")
            private String uituCode;

        }
    }
}