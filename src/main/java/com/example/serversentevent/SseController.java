package com.example.serversentevent;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping(value = "de", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Log4j2
public class SseController {

    @Autowired
    SsePushNotificationService service;

    private ExecutorService executor = Executors.newCachedThreadPool();
    private static final Logger LOGGER = LoggerFactory.getLogger(SseController.class);


    @GetMapping(Constants.API_SSE)
    public SseEmitter handleSse() {
        SseEmitter emitter = new SseEmitter();

        executor.execute(() -> {
            try {
                emitter.send(Constants.API_SSE_MSG + " @ " + new Date());
                emitter.complete();
            } catch (Exception ex) {
                System.out.println(Constants.GENERIC_EXCEPTION);
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }


    @GetMapping("/stream-sse-mvc")
    public SseEmitter streamSseMvc() {
        SseEmitter emitter = new SseEmitter();

        executor.execute(() -> {
            try {
                for (int i = 0; true; i++) {
                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                            .data("SSE MVC - " + LocalTime.now()
                                    .toString())
                            .id(String.valueOf(i))
                            .name("sse event - mvc");
                    emitter.send(event);
                    Thread.sleep(1000);
                }
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    @Autowired
    FileStorageServiceImpl fileStorageService;

    private Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    @GetMapping("/progress")
    public SseEmitter eventEmitter() throws IOException {
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        UUID guid = UUID.randomUUID();
        sseEmitters.put(guid.toString(), sseEmitter);
        sseEmitter.send(SseEmitter.event().name("GUI_ID").data(guid));
        sseEmitter.onCompletion(() -> sseEmitters.remove(guid.toString()));
        sseEmitter.onTimeout(() -> sseEmitters.remove(guid.toString()));
        return sseEmitter;
    }

    @PostMapping("/upload/local")
    public ResponseEntity<String> uploadFileLocal(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("guid") String guid) throws IOException {
        String message = "";
        try {
            fileStorageService.save(file, sseEmitters.get(guid), guid);
            sseEmitters.remove(guid);
            message = "Uploaded the file successfull:" + file.getOriginalFilename() + "!";
            return ResponseEntity.status(HttpStatus.OK).body(message);
        } catch (Exception e) {
            message = "Could not upload the file:" + file.getOriginalFilename() + "!";
            sseEmitters.remove(guid);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(message);
        }

    }



//    @PostConstruct
//    public void init() {
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            executor.shutdown();
//            try {
//                executor.awaitTermination(1, TimeUnit.SECONDS);
//            } catch (InterruptedException e) {
//                LOGGER.error(e.toString());
//            }
//        }));
//    }
//
//    @GetMapping("/notification")
////    @CrossOrigin
//    public SseEmitter streamDateTime() {
//
//        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
//
//        sseEmitter.onCompletion(() -> LOGGER.info("SseEmitter is completed"));
//
//        sseEmitter.onTimeout(() -> LOGGER.info("SseEmitter is timed out"));
//
//        sseEmitter.onError((ex) -> LOGGER.info("SseEmitter got error:", ex));
//
//        executor.execute(() -> {
//            for (int i = 0; i < 15; i++) {
//                try {
//                    sseEmitter.send(UUID.randomUUID());
//                    sleep(1, sseEmitter);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    sseEmitter.completeWithError(e);
//                }
//            }
//            sseEmitter.complete();
//        });
//
//        LOGGER.info("Controller exits");
//        return sseEmitter;
//    }
//
//    private void sleep(int seconds, SseEmitter sseEmitter) {
//        try {
//            Thread.sleep(seconds * 1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            sseEmitter.completeWithError(e);
//        }
//    }
}